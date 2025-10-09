//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import com.fasterxml.jackson.databind.JsonNode
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.shared.downloader.Downloader
//import project.pipepipe.extractor.exceptions.*
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.services.bilibili.Utils
//import project.pipepipe.extractor.utils.UtilsOld
//import project.pipepipe.shared.infoitem.PlaylistInfo
//import project.pipepipe.shared.infoitem.StaffInfo
//import project.pipepipe.shared.infoitem.StreamType
//import project.pipepipe.shared.infoitem.helper.stream.Description
//import project.pipepipe.shared.infoitem.helper.stream.MediaFormat
//import project.pipepipe.shared.infoitem.helper.stream.SubtitlesStream
//import java.time.Instant
//import java.time.OffsetDateTime
//import java.time.ZoneOffset
//
//class RegularVideoStrategy(val url: String, streamType: StreamType) : BilibiliExtractorStrategy {
//
//    private lateinit var watch: JsonNode
//    private lateinit var bvid: String
//    private var cid: Long = 0
//    private lateinit var page: JsonNode
//    private var paymentStatus = PaymentStatus.FREE
//    private lateinit var playData: JsonNode
//    private lateinit var dataObject: JsonNode
//    private lateinit var tagData: JsonNode
//    private lateinit var subtitleData: JsonNode
//
//    override suspend fun fetchData() {
//        val headers = BilibiliService.getHeaders(streamMetaInfo.url)
//        bvid = Utils.getPureBV(streamMetaInfo.id)
//        val apiUrl = Utils.getUrl(streamMetaInfo.url, bvid)
//
//        val loggedHeaders = BilibiliService.getLoggedHeadersOrNull(streamMetaInfo.url, "ai_subtitle")
//        val requestHeaders = loggedHeaders ?: headers
//
//        val requests = mutableListOf<Downloader.HttpRequest>()
//
//        var watchResult: JsonNode? = null
//        requests.add(
//            Downloader.HttpRequest(
//                { ExtractorContext.downloader.get(apiUrl, requestHeaders) },
//                { response -> watchResult = response.bodyAsJson().requireObject("data") }
//            ))
//
//        requests.add(
//            Downloader.HttpRequest(
//                { ExtractorContext.downloader.get(BiliBiliLinks.FETCH_TAGS_URL + Utils.getPureBV(streamMetaInfo.id), headers) },
//                { response -> tagData = response.bodyAsJson().requireArray("data") }
//            ))
//
//        ExtractorContext.downloader.executeAll(requests)
//        watch = watchResult!!
//
//        val pageNumString = UtilsOld.getQueryValue(
//            UtilsOld.stringToURL(streamMetaInfo.url),
//            "p"
//        )
//        val pageNum = pageNumString?.toIntOrNull() ?: 1
//
//        page = watch.requireArray("pages")[pageNum - 1]
//        cid = page.requireLong("cid")
//        streamMetaInfo.duration = page.requireLong("duration")
//        paymentStatus =
//            if (watch.requireObject("rights").requireInt("pay") == 1) PaymentStatus.PAID else PaymentStatus.FREE
//
//        BilibiliService.Companion.watchDataCache.setCid(streamMetaInfo.id, cid)
//        BilibiliService.Companion.watchDataCache.setBvid(streamMetaInfo.id, bvid)
//
//        if (loggedHeaders != null) {
//            val params = linkedMapOf<String, String>().apply {
//                put("aid", Utils.bv2av(bvid).toString())
//                put("cid", cid.toString())
//                put("isGaiaAvoided", "false")
//                put("web_location", "1315873")
//                putAll(Utils.getDmImgParams())
//            }
//
//            val subtitleUrl = Utils.getWbiResult(BiliBiliLinks.GET_SUBTITLE_META_URL, params)
//            val subtitleRequests = listOf(
//                Downloader.HttpRequest(
//                    { ExtractorContext.downloader.get(subtitleUrl, loggedHeaders) },
//                    { response ->
//                        subtitleData = response.bodyAsJson()
//                            .requireObject("data")
//                            .requireObject("subtitle")
//                            .requireArray("subtitles")
//                    }
//                ))
//            ExtractorContext.downloader.executeAll(subtitleRequests)
//        }
//
//        // Fetch stream data
//        val baseUrl = BiliBiliLinks.FREE_VIDEO_API_URL
//        val streamParams = linkedMapOf<String, String>().apply {
//            put("avid", Utils.bv2av(bvid).toString())
//            put("bvid", bvid)
//            put("cid", cid.toString())
//            put("qn", "120")
//            put("fnver", "0")
//            put("fnval", "4048")
//            put("fourk", "1")
//            put("web_location", "1315873")
//            put("try_look", "1")
//            putAll(Utils.getDmImgParams())
//        }
//
//        val streamRequestHeaders = headers.toMutableMap()
//        if (ExtractorContext.ServiceList.BiliBili.hasTokens && ExtractorContext.ServiceList.BiliBili.cookieFunctions?.contains("high_res") == true) {
//            streamRequestHeaders["Cookie"] = ExtractorContext.ServiceList.BiliBili.tokens!!
//            streamParams.remove("try_look")
//        }
//
//        val finalUrl = Utils.getWbiResult(baseUrl, streamParams)
//        playData = ExtractorContext.downloader.get(finalUrl, streamRequestHeaders).bodyAsJson()
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
//        dataObject = playData.requireObject("data").requireObject("dash")
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
//        val title = watch.requireString("title")
//        streamMetaInfo.name = if (watch.requireArray("pages").size() > 1) {
//            "P${page.requireInt("page")} ${page.requireString("part")} | $title"
//        } else {
//            title
//        }
//
//        streamMetaInfo.thumbnailUrl = watch.requireString("pic").replace("http:", "https:")
//
//        val owner = watch.requireObject("owner")
//        streamMetaInfo.uploaderName = owner.requireString("name")
//        streamMetaInfo.uploaderUrl = "https://space.bilibili.com/${owner.requireLong("mid")}"
//        streamMetaInfo.uploaderAvatarUrl = owner.requireString("face").replace("http:", "https:")
//
//        val stat = watch.requireObject("stat")
//        streamMetaInfo.viewCount = stat.requireLong("view")
//        streamMetaInfo.likeCount = stat.requireLong("like")
//        streamMetaInfo.description = Description(watch.requireString("desc"), Description.Companion.PLAIN_TEXT)
//
//        val timestamp = watch.requireLong("pubdate")
//        streamMetaInfo.uploadDate = OffsetDateTime.ofInstant(
//            Instant.ofEpochSecond(timestamp),
//            ZoneOffset.ofHours(8)
//        )
//
//        val staffArray = watch.get("staff")
//        streamMetaInfo.staffs = if (staffArray != null && staffArray.isArray && staffArray.size() > 0) {
//            staffArray.map { staff ->
//                StaffInfo(
//                    "https://space.bilibili.com/${staff.requireLong("mid")}",
//                    staff.requireString("name"),
//                    staff.requireString("face"),
//                    staff.requireString("title")
//                )
//            }
//        } else {
//            emptyList()
//        }
//
//        streamMetaInfo.stats = stat.fieldNames().asSequence().associateWith {
//            stat.get(it).asText()
//        }
//
//        if (::tagData.isInitialized) {
//            streamMetaInfo.tags = tagData.map { it.requireString("tag_name") }
//        } else {
//            streamMetaInfo.tags = emptyList()
//        }
//
//        streamMetaInfo.startPosition = try {
//            streamMetaInfo.url!!.split("#timestamp=")[1].toLong()
//        } catch (e: Exception) {
//            0
//        }
//    }
//
//
//
//    override suspend fun processSubtitles() {
//        if (::subtitleData.isInitialized) {
//            streamMetaInfo.subtitles = ArrayList()
//            val headers = BilibiliService.Companion.getHeaders(streamMetaInfo.url!!)
//
//            val subtitleRequests = mutableListOf<Downloader.HttpRequest>()
//
//            subtitleData.forEachIndexed { index, subtitleItem ->
//                val subtitleUrl = "https:" + subtitleItem.requireString("subtitle_url")
//                    .replace("http:", "https:")
//                val languageCode = subtitleItem.requireString("lan").replace("ai-", "")
//                val isAutoGenerated = subtitleItem.requireInt("ai_status") != 0
//
//                subtitleRequests.add(
//                    Downloader.HttpRequest(
//                        { ExtractorContext.downloader.get(subtitleUrl, headers) },
//                        { response ->
//                            try {
//                                val bccResult = response.bodyAsJson()
//                                val subtitleContent = Utils.bcc2srt(bccResult)
//
//                                synchronized(streamMetaInfo.subtitles!!) {
//                                    streamMetaInfo.subtitles!!.add(
//                                        SubtitlesStream.Companion.create(
//                                            content = subtitleContent,
//                                            isUrl = false,
//                                            languageCode = languageCode,
//                                            autoGenerated = isAutoGenerated,
//                                            mediaFormat = MediaFormat.SRT
//                                        )
//                                    )
//                                }
//                            } catch (e: Exception) {
//                                // Skip this subtitle if there's an error processing it
//                            }
//                        }
//                    ))
//            }
//
//            if (subtitleRequests.isNotEmpty()) {
//                ExtractorContext.downloader.executeAll(subtitleRequests)
//            }
//        }
//    }
//
//    override fun getRelatedItemsInfo(): RelatedItemsInfo {
//        val partitionPlaylistInfo = PlaylistInfo(
//            BiliBiliLinks.GET_PARTITION_URL +
//                    bvid +
//                    "&name=" +
//                    watch.requireString("title") +
//                    "&thumbnail=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.thumbnailUrl) +
//                    "&uploaderName=" +
//                    streamMetaInfo.uploaderName +
//                    "&uploaderAvatar=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderAvatarUrl) +
//                    "&uploaderUrl=" +
//                    Utils.formatParamWithPercentSpace(streamMetaInfo.uploaderUrl),
//            watch.requireString("title"),
//            thumbnailUrl = streamMetaInfo.thumbnailUrl,
//            uploaderName = streamMetaInfo.uploaderName
//        )
//
//        return RelatedItemsInfo(
//            bvid = bvid,
//            relatedPaidItems = null,
//            liveStatus = null,
//            nextRoundPlayStreamInfo = null,
//            partitionPlaylistInfo = partitionPlaylistInfo,
//            pubDate = watch.requireLong("pubdate")
//        )
//    }
//}