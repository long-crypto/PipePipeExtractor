package project.pipepipe.extractor.services.youtube.extractor

import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.ExtractorContext
import project.pipepipe.extractor.services.youtube.YouTubeLinks.CHANNEL_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.COMMENT_RAW_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.NEXT_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.STREAM_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.VIDEO_INFO_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.getAndroidFetchStreamUrl
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.ANDROID_HEADER
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.WEB_HEADER
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.getAndroidFetchStreamBody
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.getVideoInfoBody
import project.pipepipe.extractor.services.youtube.YouTubeUrlParser
import project.pipepipe.extractor.services.youtube.dataparser.YouTubeStreamInfoDataParser.parseFromLockupViewModel
import project.pipepipe.shared.state.State
import project.pipepipe.extractor.utils.UtilsOld.mixedNumberWordToLong
import project.pipepipe.extractor.utils.createMultiStreamDashManifest
import project.pipepipe.extractor.utils.parseMediaType
import project.pipepipe.shared.infoitem.CommentInfo
import project.pipepipe.shared.infoitem.RelatedItemInfo
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.infoitem.StreamType
import project.pipepipe.shared.infoitem.helper.stream.AudioStream
import project.pipepipe.shared.infoitem.helper.stream.Description
import project.pipepipe.shared.infoitem.helper.stream.Description.Companion.PLAIN_TEXT
import project.pipepipe.shared.infoitem.helper.stream.SubtitleStream
import project.pipepipe.shared.infoitem.helper.stream.VideoStream
import project.pipepipe.shared.job.*
import project.pipepipe.shared.state.CachedExtractState
import project.pipepipe.shared.state.PlainState
import project.pipepipe.shared.utils.json.asJson
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireInt
import project.pipepipe.shared.utils.json.requireLong
import project.pipepipe.shared.utils.json.requireObject
import project.pipepipe.shared.utils.json.requireString
import java.time.OffsetDateTime

class YouTubeStreamExtractor(
    url: String,
) : Extractor<StreamInfo, Nothing>(url) {
    override suspend fun fetchInfo(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        val id = YouTubeUrlParser.parseStreamId(url)!!
        if (currentState == null) {
            return JobStepResult.ContinueWith(listOf(
                ClientTask(taskId = "info", payload = Payload(
                    RequestMethod.POST,
                    VIDEO_INFO_URL,
                    WEB_HEADER,
                    getVideoInfoBody(id)
                )),
                ClientTask(taskId = "play_data", payload = Payload(
                    RequestMethod.POST,
                    getAndroidFetchStreamUrl(id),
                    ANDROID_HEADER,
                    getAndroidFetchStreamBody(id)
                )),
                ClientTask(taskId = "next_data", payload = Payload(
                    RequestMethod.POST,
                    NEXT_URL,
                    WEB_HEADER,
                    getVideoInfoBody(id)
                )),
            ), state = PlainState(0))
        } else {
            val info = clientResults!!.first { it.taskId == "info" }.result!!.asJson()
            val nextData = clientResults.first { it.taskId == "next_data" }.result!!.asJson()  // don't use this if possible as it's not stable
            val playData = clientResults.first { it.taskId == "play_data" }.result!!.asJson()
            val savedRelatedData = nextData.requireArray("/contents/twoColumnWatchNextResults/secondaryResults/secondaryResults/results").mapNotNull {
                runCatching { parseFromLockupViewModel(it) }.getOrNull()
            }
            val isLive = playData.requireObject("/playerResponse/playabilityStatus").has("liveStreamability")
            val streamInfo = StreamInfo(
                url = STREAM_URL + id,
                serviceId = "YOUTUBE",
                name = playData.requireString("/playerResponse/videoDetails/title"),
                uploaderName = playData.requireString("/playerResponse/videoDetails/author"),
                uploadDate = safeGet { OffsetDateTime.parse(info.requireString("/microformat/playerMicroformatRenderer/uploadDate")).toInstant().toEpochMilli() },
                viewCount = safeGet { info.requireLong("/microformat/playerMicroformatRenderer/viewCount") },
                uploaderUrl = safeGet { CHANNEL_URL + playData.requireString("/playerResponse/videoDetails/channelId") },
                uploaderAvatarUrl = safeGet { nextData.requireArray("/contents/twoColumnWatchNextResults/results/results/contents/1/videoSecondaryInfoRenderer/owner/videoOwnerRenderer/thumbnail/thumbnails").last().requireString("url") },
                likeCount = safeGet { info.requireLong("/microformat/playerMicroformatRenderer/likeCount") },
                uploaderSubscriberCount = safeGet { mixedNumberWordToLong(nextData.requireString("/contents/twoColumnWatchNextResults/results/results/contents/1/videoSecondaryInfoRenderer/owner/videoOwnerRenderer/subscriberCountText/simpleText")) },
                streamSegments = null,
                previewFrames = null,
                thumbnailUrl = safeGet { info.requireArray("/videoDetails/thumbnail/thumbnails").last().requireString("url") },
                description = safeGet { Description(info.requireString("/microformat/playerMicroformatRenderer/description/simpleText"), PLAIN_TEXT) },
                tags = safeGet{ info.requireArray("/videoDetails/keywords").map { it.asText() } },
                commentInfo = safeGet { CommentInfo.builder().url("$COMMENT_RAW_URL?continuation=${nextData.requireArray("/contents/twoColumnWatchNextResults/results/results/contents").firstNotNullOfOrNull {
                   runCatching { it.requireString("/itemSectionRenderer/contents/0/continuationItemRenderer/continuationEndpoint/continuationCommand/token") }.getOrNull()
                }}").serviceId("YOUTUBE").build() },
                relatedItemInfo = RelatedItemInfo("cache://${sessionId}"),
                headers = hashMapOf("User-Agent" to "com.google.android.youtube/19.28.35 (Linux; U; Android 15; GB) gzip")
            ).apply {
                when (isLive) {
                    false -> {
                        streamType = StreamType.VIDEO_STREAM
                        duration = playData.requireLong("/playerResponse/videoDetails/lengthSeconds")
                        sponsorblockUrl = "sponsorblock://youtube.raw?id=$id"
                        dashManifest = createMultiStreamDashManifest(
                            playData.requireLong("/playerResponse/videoDetails/lengthSeconds") * 1.0,
                            playData.requireArray("/playerResponse/streamingData/adaptiveFormats")
                                .filter { it.requireString("mimeType").startsWith("video") }
                                .map {
                                    VideoStream(
                                        it.requireInt("itag").toString(),
                                        it.requireString("url"),
                                        parseMediaType(it.requireString("mimeType")).first,
                                        parseMediaType(it.requireString("mimeType")).second!!,
                                        it.requireLong("bitrate"),
                                        it.requireInt("width"),
                                        it.requireInt("height"),
                                        it.requireInt("fps").toString(),
                                        "${it.requireInt("/indexRange/start")}-${it.requireInt("/indexRange/end")}",
                                        "${it.requireInt("/initRange/start")}-${it.requireInt("/initRange/end")}"
                                    )
                                }, playData.requireArray("/playerResponse/streamingData/adaptiveFormats")
                                .filter { it.requireString("mimeType").startsWith("audio") }
                                .map {
                                    val hasMultiTracks = it.has("audioTrack")
                                    AudioStream(
                                        if (hasMultiTracks) it.requireString("/audioTrack/displayName") else it.requireInt("itag").toString(),
                                        it.requireString("url"),
                                        parseMediaType(it.requireString("mimeType")).first,
                                        parseMediaType(it.requireString("mimeType")).second!!,
                                        it.requireLong("bitrate"),
                                        "${it.requireInt("/indexRange/start")}-${it.requireInt("/indexRange/end")}",
                                        "${it.requireInt("/initRange/start")}-${it.requireInt("/initRange/end")}",
                                        if (hasMultiTracks) it.requireString("audioSampleRate") else null,
                                        if (hasMultiTracks) it.requireString("/audioTrack/id").substringBefore("-") else null,
                                    )
                                }, playData.at("/playerResponse/captions/playerCaptionsTracklistRenderer/captionTracks")
                                .map {
                                    SubtitleStream(
                                        it.requireString("vssId"),
                                        "application/ttml+xml",
                                        it.requireString("baseUrl").replace("&fmt=[^&]*".toRegex(), "")
                                            .replace("&tlang=[^&]*".toRegex(), "") + "&fmt=ttml",
                                        it.requireString("languageCode")
                                    )
                                }
                        )
                        isPortrait = playData.requireArray("/playerResponse/streamingData/adaptiveFormats")
                            .filter { it.requireString("mimeType").startsWith("video") }[0].let {
                                it.requireInt("width") < it.requireInt("height")
                            }
                    }
                    true -> {
                        streamType = StreamType.LIVE_STREAM
                        hlsUrl = playData.requireString("/playerResponse/streamingData/hlsManifestUrl")
                    }
                }
            }
            return JobStepResult.CompleteWith(ExtractResult(streamInfo, errors), state = CachedExtractState(
                step = 0,
                ExtractResult(info = RelatedItemInfo("cache://${sessionId}"), pagedData = PagedData(savedRelatedData, null))
            ))
        }
    }

}