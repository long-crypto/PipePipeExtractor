//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import com.fasterxml.jackson.databind.JsonNode
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.extractor.exceptions.*
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.stream.*
//import project.pipepipe.shared.infoitem.PlaylistInfo
//import project.pipepipe.shared.infoitem.StreamInfo
//import project.pipepipe.shared.infoitem.helper.ServiceId
//import project.pipepipe.shared.infoitem.helper.stream.VideoStream
//import java.util.*
//
//class LiveStreamStrategy(override val streamMetaInfo: BiliBiliStreamMetaInfo) : BilibiliExtractorStrategy {
//
//    private lateinit var watch: JsonNode
//    private var liveUrl: String? = null
//    private var isRoundPlay = false
//    private var playTime: Long = 0
//    private lateinit var currentRoundTitle: String
//    private var nextTimestamp: Long = 0
//    private lateinit var bvid: String
//    private var cid: Long = 0
//    private lateinit var dataObject: JsonNode
//    private var liveStatus: LiveStatus? = null
//
//    override suspend fun fetchData() {
//        val roomId = streamMetaInfo.id
//        val headers = BilibiliService.Companion.getHeaders(streamMetaInfo.url!!)
//
//        val roomInfoUrl = "https://api.live.bilibili.com/room/v1/Room/room_init?id=$roomId"
//        val roomInfoResponseData = ExtractorContext.downloader.get(roomInfoUrl).bodyAsJson()
//        val roomInfoData = roomInfoResponseData.requireObject("data")
//
//        if (roomInfoData.size() == 0) {
//            val message = roomInfoResponseData.requireString("msg")
//            throw ExtractionException("Can not get live room info. Error message: $message")
//        }
//
//        val uid = roomInfoData.requireLong("uid").toString()
//        BilibiliService.Companion.watchDataCache.setRoomId(roomInfoData.requireLong("room_id"))
//        BilibiliService.Companion.watchDataCache.setStartTime(roomInfoData.requireLong("live_time"))
//
//        val statusUrl = "https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids?uids[]=$uid"
//        val statusData = ExtractorContext.downloader.get(statusUrl).bodyAsJson()
//        watch = statusData.requireObject("data").requireObject(uid)
//
//        when (roomInfoData.requireInt("live_status")) {
//            LiveStatus.NOT_STARTED.code -> throw LiveNotStartException("Live is not started.")
//            LiveStatus.ROUND_PLAY.code -> initRoundPlay(roomInfoData, headers)
//            LiveStatus.LIVE.code -> initLivePlayback(roomId, headers)
//        }
//        liveStatus = enumValues<LiveStatus>().first { it.code == roomInfoData.requireInt("live_status") }
//    }
//
//    private suspend fun initRoundPlay(roomInfoData: JsonNode, headers: Map<String, String>) {
//        val timestamp = if (streamMetaInfo.url!!.contains("timestamp=")) {
//            streamMetaInfo.url.split("timestamp=")[1].split("&")[0].toLong()
//        } else {
//            Date().time
//        }
//
//        isRoundPlay = true
//        val roundPlayUrl = String.format(
//            "https://api.live.bilibili.com/live/getRoundPlayVideo?room_id=%s&a=%s&type=flv",
//            roomInfoData.requireLong("room_id"), timestamp
//        )
//
//        val roundPlayData = ExtractorContext.downloader.get(roundPlayUrl).bodyAsJson().requireObject("data")
//
//        if (roundPlayData.requireLong("cid") < 0) {
//            throw ContentNotAvailableException("Round playing is not available at this moment.")
//        }
//
//        playTime = roundPlayData.requireLong("play_time")
//        currentRoundTitle = roundPlayData.requireString("title")
//        currentRoundTitle = currentRoundTitle.split("-")[1] + currentRoundTitle.split("-")[2]
//        bvid = roundPlayData.requireString("bvid")
//        cid = roundPlayData.requireLong("cid")
//
//        val playUrl =
//            "https://api.bilibili.com/x/player/playurl?cid=$cid&bvid=$bvid&fnval=4048&qn=120&fourk=1&try_look=1"
//        val playData = ExtractorContext.downloader.get(playUrl, headers).bodyAsJson()
//        dataObject = playData.requireObject("data").requireObject("dash")
//
//        nextTimestamp = timestamp + dataObject.requireLong("duration") * 1000
//    }
//
//    private suspend fun initLivePlayback(roomId: String, headers: Map<String, String>) {
//        val playUrl = "https://api.live.bilibili.com/room/v1/Room/playUrl?qn=10000&platform=web&cid=$roomId"
//        val playUrlData = ExtractorContext.downloader.get(playUrl, headers).bodyAsJson()
//        liveUrl = playUrlData.requireObject("data").requireArray("durl")[0].requireString("url")
//    }
//
//    override suspend fun setMetadata() {
//        streamMetaInfo.name = if (isRoundPlay) {
//            "${streamMetaInfo.uploaderName}的投稿视频轮播"
//        } else {
//            watch.requireString("title")
//        }
//
//        streamMetaInfo.thumbnailUrl = watch.requireString("cover_from_user").replace("http:", "https:")
//        streamMetaInfo.uploaderName = watch.requireString("uname")
//        streamMetaInfo.uploaderUrl = "https://space.bilibili.com/${watch.requireLong("uid")}"
//        streamMetaInfo.uploaderAvatarUrl = watch.requireString("face").replace("http:", "https:")
//        streamMetaInfo.viewCount = watch.requireLong("online")
//        streamMetaInfo.likeCount = -1
//        streamMetaInfo.description = null
//        streamMetaInfo.startPosition = -1
//        streamMetaInfo.startAt = if (!isRoundPlay) {
//            watch.requireLong("live_time") * 1000
//        } else {
//            -1
//        }
//
//        val tagName = watch.get("tag_name")?.asText() ?: ""
//        val tags = watch.get("tags")?.asText() ?: ""
//        streamMetaInfo.tags = "$tagName,$tags".split(",").filter { it.isNotEmpty() }
//
//        streamMetaInfo.startPosition = try {
//            streamMetaInfo.url!!.split("#timestamp=")[1].toLong()
//        } catch (e: Exception) {
//            if (isRoundPlay) playTime else 0
//        }
//    }
//
//    override suspend fun buildStreams() {
//        if (isRoundPlay) {
//            buildRoundPlayStreams()
//        } else {
//            buildLiveStreams()
//        }
//    }
//
//    private fun buildRoundPlayStreams() {
//        if (::dataObject.isInitialized) {
//            if (dataObject.requireArray("audio").size() == 0) {
//                val dataArray = dataObject.requireArray("video")
//                streamMetaInfo.videoStreams.addAll(dataArray.map { videoNode ->
//                    val resolution = BilibiliService.Companion.getResolution(videoNode.requireInt("id"))
//                    VideoStream(
//                        content = videoNode.requireString("base_url"),
//                        id = "bilibili-${watch.requireLong("cid")}",
//                        isUrl = true,
//                        videoOnly = false,
//                        videoResolution = resolution,
//                        codec = "Unknown"
//                    )
//                })
//            } else {
//                buildVideoOnlyStreamsArray()
//                buildAudioStreamsArray()
//            }
//        }
//    }
//
//    private fun buildLiveStreams() {
//        if (liveUrl != null) {
//            streamMetaInfo.videoStreams.add(
//                VideoStream(
//                    content = liveUrl!!,
//                    isUrl = true,
//                    id = "bilibili-${watch.requireLong("uid")}-live",
//                    videoOnly = false,
//                    videoResolution = "720p"
//                )
//            )
//        }
//    }
//
//    private fun buildVideoOnlyStreamsArray() {
//        val videoArray = dataObject.requireArray("video")
//        videoArray.forEach { videoNode ->
//            val code = videoNode.requireInt("id")
//            val resolution = BilibiliService.Companion.getResolution(code)
//            streamMetaInfo.videoOnlyStreams.add(
//                VideoStream(
//                    content = videoNode.requireString("baseUrl"),
//                    id = "bilibili-$bvid-video",
//                    isUrl = true,
//                    mediaFormat = project.pipepipe.shared.infoitem.helper.stream.MediaFormat.MPEG_4,
//                    videoResolution = resolution,
//                    codec = videoNode.requireString("codecs"),
//                    videoOnly = true
//                )
//            )
//        }
//    }
//
//    private fun buildAudioStreamsArray() {
//        val audioObject = dataObject.requireArray("audio")[0]
//        streamMetaInfo.audioStreams.add(
//            project.pipepipe.shared.infoitem.helper.stream.AudioStream(
//                id = "bilibili-$bvid-audio",
//                content = audioObject.requireString("baseUrl"),
//                isUrl = true,
//                format = project.pipepipe.shared.infoitem.helper.stream.MediaFormat.M4A,
//                averageBitrate = 192000,
//            )
//        )
//    }
//
//    override suspend fun processSubtitles() {
//        // Live streams don't have subtitles
//    }
//
//    override fun getRelatedItemsInfo(): RelatedItemsInfo {
//        val nextRoundPlayStreamInfo = if (isRoundPlay) {
//            StreamInfo(
//                streamMetaInfo.url + "?timestamp=" + nextTimestamp,
//                streamMetaInfo.id,
//                streamMetaInfo.uploaderName + "的投稿视频轮播",
//                ServiceId.BILIBILI,
//                thumbnailUrl = streamMetaInfo.thumbnailUrl,
//                viewCount = streamMetaInfo.viewCount
//            )
//        } else null
//
//        val partitionPlaylistInfo = PlaylistInfo(
//            project.pipepipe.extractor.services.bilibili.BiliBiliLinks.GET_PARTITION_URL +
//                    bvid +
//                    "&name=" +
//                    watch.requireString("title") +
//                    "&thumbnail=" +
//                    project.pipepipe.extractor.services.bilibili.Utils.formatParamWithPercentSpace(streamMetaInfo.thumbnailUrl) +
//                    "&uploaderName=" +
//                    streamMetaInfo.uploaderName +
//                    "&uploaderAvatar=" +
//                    project.pipepipe.extractor.services.bilibili.Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderAvatarUrl) +
//                    "&uploaderUrl=" +
//                    project.pipepipe.extractor.services.bilibili.Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderUrl),
//            watch.requireString("title"),
//            thumbnailUrl = streamMetaInfo.thumbnailUrl,
//            uploaderName = streamMetaInfo.uploaderName
//        )
//
//        return RelatedItemsInfo(
//            bvid = if (::bvid.isInitialized) bvid else "",
//            relatedPaidItems = null,
//            liveStatus = liveStatus,
//            nextRoundPlayStreamInfo = nextRoundPlayStreamInfo,
//            partitionPlaylistInfo = partitionPlaylistInfo,
//            pubDate = watch.get("live_time")?.asLong() ?: 0
//        )
//    }
//}