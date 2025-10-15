package project.pipepipe.extractor.services.niconico.extractor

import org.jsoup.Jsoup
import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.ExtractorContext
import project.pipepipe.extractor.ExtractorContext.objectMapper
import project.pipepipe.extractor.services.niconico.NicoNicoLinks.CHANNEL_URL
import project.pipepipe.extractor.services.niconico.NicoNicoLinks.WATCH_URL
import project.pipepipe.extractor.services.niconico.NicoNicoLinks.getAccessUrl
import project.pipepipe.extractor.services.niconico.NicoNicoService.Companion.GOOGLE_HEADER
import project.pipepipe.extractor.services.niconico.NicoNicoService.Companion.isChannel
import project.pipepipe.extractor.services.niconico.NicoNicoUrlParser.parseStreamId
import project.pipepipe.shared.state.State
import project.pipepipe.shared.state.StreamExtractState
import project.pipepipe.extractor.ExtractorContext.asJson
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireBoolean
import project.pipepipe.shared.utils.json.requireLong
import project.pipepipe.shared.utils.json.requireString
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.infoitem.StreamType
import project.pipepipe.shared.infoitem.helper.stream.Description
import project.pipepipe.shared.job.*
import project.pipepipe.shared.state.PlainState
import java.time.ZonedDateTime


class NicoNicoStreamExtractor(
    url: String,
) : Extractor<StreamInfo, Nothing>(url)  {
    override suspend fun fetchInfo(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        val id = parseStreamId(url)
        if (currentState == null) {
            return JobStepResult.ContinueWith(listOf(
                ClientTask(payload = Payload(RequestMethod.GET, WATCH_URL + id, GOOGLE_HEADER)),
            ), state = PlainState(0))
        } else if (currentState.step == 0) {
            val response = clientResults!!.first { it.taskId.isDefaultTask()}.result!!
            val page = Jsoup.parse(response)
            val watchData = objectMapper.readTree(page.select("meta[name=\"server-response\"]").first()!!.attr("content"))
            val streamInfo = StreamInfo(
                url = WATCH_URL + id,
                serviceId = "NICONICO",
                id = null,
                name = watchData.requireString("/data/response/video/title"),
                streamType = StreamType.VIDEO_STREAM,
                uploaderName =
                    if (isChannel(watchData)) runCatching {
                        watchData.requireString("/data/response/channel/name")
                    }.getOrDefault("Removed")
                    else watchData.requireString(
                        "/data/response/owner/nickname"
                    ),
                uploadDate = safeGet { ZonedDateTime.parse(watchData.requireString("/data/response/video/registeredAt")).toInstant().toEpochMilli() },
                duration = watchData.requireLong("/data/response/video/duration"),
                uploaderUrl = safeGet {
                    if (isChannel(watchData)) CHANNEL_URL + watchData.requireString("/data/response/channel/id")
                    else watchData.requireString(
                        "/data/response/owner/id"
                    )
                },
                uploaderAvatarUrl = safeGet {
                    if (isChannel(watchData)) watchData.requireString("/data/response/channel/thumbnail/url")
                    else watchData.requireString(
                        "/data/response/owner/iconUrl"
                    )
                },
                viewCount = safeGet { watchData.requireLong("/data/response/video/count/view") },
                likeCount = safeGet { watchData.requireLong("/data/response/video/count/like") },
                thumbnailUrl = safeGet { watchData.requireString("/data/response/video/thumbnail/ogp") },
                description = safeGet { Description(watchData.requireString("/data/response/video/description"),
                    Description.HTML) },
                tags = safeGet { watchData.requireArray("/data/response/tag/items").map { it.requireString("name") } },
                commentInfo = null,
                relatedItemInfo = null
            )
            val audioId = watchData.requireArray("/data/response/media/domand/audios")
                .first { it.requireBoolean("isAvailable") }.requireString("id")
            val resolutionArray = watchData.requireArray("/data/response/media/domand/videos")
                .filter { it.requireBoolean("isAvailable") }
                .map { listOf(it.requireString("id"), audioId) }

            val body = objectMapper.writeValueAsString(mapOf(
                "outputs" to resolutionArray
            ))
            return JobStepResult.ContinueWith(listOf(
                ClientTask(payload = Payload(
                    RequestMethod.POST,
                    getAccessUrl(id!!, watchData.requireString("/data/response/client/watchTrackId")),
                    getAccessHeaders(watchData.requireString("/data/response/media/domand/accessRightKey")),
                    body
                ))), state = StreamExtractState(1, streamInfo))
        } else {
            val streamInfo = (currentState as StreamExtractState).streamInfo
            val response = clientResults!!.first { it.taskId.isDefaultTask()}
            streamInfo.hlsUrl = response.result!!.asJson().requireString("/data/contentUrl")
            streamInfo.headers = hashMapOf("Cookie" to response.responseHeader!!["set-cookie"]!![0].split(";").first { it.trim().startsWith("domand_bid=") })
            return JobStepResult.CompleteWith(ExtractResult(streamInfo))
        }
    }
    fun getAccessHeaders(key: String) = mapOf(
        "Referer" to "https://www.nicovideo.jp/",
        "X-Frontend-Id" to "6",
        "X-Frontend-Version" to "0",
        "X-Request-With" to "nicovideo",
        "X-Access-Right-Key" to key,
    )
}