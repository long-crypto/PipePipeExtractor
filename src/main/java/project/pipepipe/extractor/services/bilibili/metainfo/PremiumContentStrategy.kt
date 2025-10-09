//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import com.fasterxml.jackson.databind.JsonNode
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.extractor.exceptions.*
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.services.bilibili.Utils
//import project.pipepipe.shared.infoitem.PlaylistInfo
//import project.pipepipe.shared.infoitem.helper.stream.Description
//import project.pipepipe.shared.infoitem.helper.stream.MediaFormat
//import java.time.Instant
//import java.time.OffsetDateTime
//import java.time.ZoneOffset
//
//class PremiumContentStrategy(override val streamMetaInfo: BiliBiliStreamMetaInfo) : BilibiliExtractorStrategy {
//
//    private lateinit var watch: JsonNode
//    private lateinit var premiumData: JsonNode
//    private lateinit var relatedPaidItems: JsonNode
//    private lateinit var bvid: String
//    private var cid: Long = 0
//    private var paymentStatus = PaymentStatus.FREE
//    private lateinit var playData: JsonNode
//    private lateinit var dataObject: JsonNode
//
//    override suspend fun fetchData() {
//        val headers = BilibiliService.Companion.getHeaders(streamMetaInfo.url!!)
//        val id = streamMetaInfo.id
//        val type = if (id.startsWith("ss")) PremiumContentType.SEASON else PremiumContentType.EPISODE
//
//        val apiUrl = "https://api.bilibili.com/pgc/view/web/season?" +
//                if (type == PremiumContentType.SEASON) "season_id=" else "ep_id=" + id.substring(2)
//
//        try {
//            val response = ExtractorContext.downloader.get(apiUrl, headers).bodyAsJson()
//            premiumData = response.requireObject("result")
//            relatedPaidItems = premiumData.requireArray("episodes")
//
//            watch = if (type == PremiumContentType.SEASON) {
//                relatedPaidItems[0]
//            } else {
//                relatedPaidItems.find {
//                    it.requireString("share_url").endsWith(id)
//                } ?: throw ExtractionException("Not found id in series data")
//            }
//
//            bvid = watch.requireString("bvid")
//            cid = watch.requireLong("cid")
//            streamMetaInfo.duration = watch.requireLong("duration") / 1000
//            paymentStatus =
//                if (watch.requireObject("rights").requireInt("pay") == 1) PaymentStatus.PAID else PaymentStatus.FREE
//
//        } catch (e: Exception) {
//            throw ContentNotAvailableException("Unknown reason")
//        }
//
//        BilibiliService.Companion.watchDataCache.setCid(id, cid)
//        BilibiliService.Companion.watchDataCache.setBvid(id, bvid)
//
//        // Fetch stream data
//        val baseUrl = BiliBiliLinks.PAID_VIDEO_API_URL
//        val params = linkedMapOf<String, String>().apply {
//            put("avid", Utils.bv2av(bvid).toString())
//            put("bvid", bvid)
//            put("cid", cid.toString())
//            put("qn", "120")
//            put("fnver", "0")
//            put("fnval", "4048")
//            put("fourk", "1")
//        }
//
//        val requestHeaders = headers.toMutableMap()
//        if (ExtractorContext.ServiceList.BiliBili.hasTokens && ExtractorContext.ServiceList.BiliBili.cookieFunctions?.contains("high_res") == true) {
//            requestHeaders["Cookie"] = ExtractorContext.ServiceList.BiliBili.tokens!!
//        }
//
//        val finalUrl = "$baseUrl?${Utils.createQueryString(params)}"
//        playData = ExtractorContext.downloader.get(finalUrl, requestHeaders).bodyAsJson()
//
//        when (playData.requireInt("code")) {
//            0 -> {} // Success
//            -10403 -> {
//                val message = playData.requireString("message")
//                if (message.contains("地区")) {
//                    throw GeographicRestrictionException(message)
//                }
//                throw ContentNotAvailableException(message)
//            }
//            else -> {
//                val message = playData.requireString("message")
//                if (message.contains("地区")) {
//                    throw GeographicRestrictionException(message)
//                }
//                throw ContentNotAvailableException(message)
//            }
//        }
//
//        val dataParentObject = playData.requireObject("result").requireObject("video_info")
//        dataObject = dataParentObject.requireObject("dash")
//
//        if (dataObject.size() == 0) {
//            throw PaidContentException("Paid content")
//        }
//
//        if (paymentStatus == PaymentStatus.PAID && dataObject.requireArray("video").size() + dataObject.requireArray("audio").size() == 0) {
//            throw PaidContentException("Paid content")
//        }
//    }
//
//    override suspend fun setMetadata() {
//        streamMetaInfo.name = watch.requireString("share_copy")
//        streamMetaInfo.thumbnailUrl = watch.requireString("cover").replace("http:", "https:")
//
//        val upInfo = premiumData.get("up_info")
//        streamMetaInfo.uploaderName = upInfo?.get("uname")?.asText() ?: "BiliBili"
//        streamMetaInfo.uploaderUrl = upInfo?.let {
//            "https://space.bilibili.com/${it.requireLong("mid")}"
//        }
//        streamMetaInfo.uploaderAvatarUrl = try {
//            upInfo?.requireString("avatar")?.replace("http:", "https:")
//        } catch (e: Exception) {
//            "https://i2.hdslb.com/bfs/face/0c84b9f4ad546d3f20324809d45fc439a2a8ddab.jpg@240w_240h_1c_1s.webp"
//        }
//
//        val stat = premiumData.requireObject("stat")
//        streamMetaInfo.viewCount = stat.requireLong("views")
//        streamMetaInfo.likeCount = stat.requireLong("likes")
//        streamMetaInfo.description = Description(
//            premiumData.requireString("evaluate"),
//            Description.Companion.PLAIN_TEXT
//        )
//
//        val timestamp = watch.requireLong("pub_time")
//        streamMetaInfo.uploadDate = OffsetDateTime.ofInstant(
//            Instant.ofEpochSecond(timestamp),
//            ZoneOffset.ofHours(8)
//        )
//        streamMetaInfo.startPosition = streamMetaInfo.duration!!
//    }
//
//    override suspend fun buildStreams() {
//        if (dataObject.requireArray("audio").size() == 0) {
//            val dataArray = dataObject.requireArray("video")
//            streamMetaInfo.videoStreams.addAll(dataArray.map { videoNode ->
//                val resolution = BilibiliService.Companion.getResolution(videoNode.requireInt("id"))
//                project.pipepipe.shared.infoitem.helper.stream.VideoStream(
//                    content = videoNode.requireString("base_url"),
//                    id = "bilibili-${watch.requireLong("cid")}",
//                    isUrl = true,
//                    videoOnly = false,
//                    videoResolution = resolution,
//                    codec = "Unknown"
//                )
//            })
//        } else {
//            buildVideoOnlyStreamsArray()
//            buildAudioStreamsArray()
//        }
//    }
//
//    private fun buildVideoOnlyStreamsArray() {
//        val videoArray = dataObject.requireArray("video")
//        videoArray.forEach { videoNode ->
//            val code = videoNode.requireInt("id")
//            val resolution = BilibiliService.Companion.getResolution(code)
//            streamMetaInfo.videoOnlyStreams.add(
//                project.pipepipe.shared.infoitem.helper.stream.VideoStream(
//                    content = videoNode.requireString("baseUrl"),
//                    id = "bilibili-$bvid-video",
//                    isUrl = true,
//                    mediaFormat = MediaFormat.MPEG_4,
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
//                format = MediaFormat.M4A,
//                averageBitrate = 192000,
//            )
//        )
//    }
//
//    override suspend fun processSubtitles() {
//        // Premium content subtitles would be handled here if needed
//    }
//
//    override fun getRelatedItemsInfo(): RelatedItemsInfo {
//        val partitionPlaylistInfo = PlaylistInfo(
//            BiliBiliLinks.GET_PARTITION_URL +
//                    bvid +
//                    "&name=" +
//                    watch.requireString("share_copy") +
//                    "&thumbnail=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.thumbnailUrl) +
//                    "&uploaderName=" +
//                    streamMetaInfo.uploaderName +
//                    "&uploaderAvatar=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderAvatarUrl) +
//                    "&uploaderUrl=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderUrl),
//            watch.requireString("share_copy"),
//            thumbnailUrl = streamMetaInfo.thumbnailUrl,
//            uploaderName = streamMetaInfo.uploaderName
//        )
//
//        return RelatedItemsInfo(
//            bvid = bvid,
//            relatedPaidItems = relatedPaidItems,
//            liveStatus = null,
//            nextRoundPlayStreamInfo = null,
//            partitionPlaylistInfo = partitionPlaylistInfo,
//            pubDate = watch.requireLong("pub_time")
//        )
//    }
//}