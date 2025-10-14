package project.pipepipe.extractor.services.bilibili

//import project.pipepipe.extractor.services.bilibili.metainfo.BiliBiliSearchExtractor
//import project.pipepipe.extractor.services.bilibili.metainfo.BiliBiliSuggestionMetaInfo
import project.pipepipe.extractor.StreamingService
import project.pipepipe.extractor.base.CookieExtractor
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.GET_SUGGESTION_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.SEARCH_BASE_URL
import project.pipepipe.extractor.services.bilibili.metainfo.BiliBiliCookieExtractor
import project.pipepipe.extractor.utils.UtilsOld
import project.pipepipe.shared.infoitem.SupportedServiceInfo
import project.pipepipe.shared.infoitem.helper.SearchFilterGroup
import project.pipepipe.shared.infoitem.helper.SearchFilterItem
import project.pipepipe.shared.infoitem.helper.SearchType
import project.pipepipe.shared.job.Payload
import project.pipepipe.shared.job.RequestMethod

class BilibiliService(id: String) : StreamingService(id) {

    companion object {
        fun mapToCookieHeader(cookies: Map<String, String>?): String {
            if (cookies.isNullOrEmpty()) {
                return ""
            }
            return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }
        fun getHeaders(originalUrl: String, cookie: String): LinkedHashMap<String, String> {
            val headers = getUserAgentHeaders(originalUrl)
            headers["Cookie"] = cookie
            return headers
        }

        fun getUserAgentHeaders(originalUrl: String?): LinkedHashMap<String, String> {
            val headers = linkedMapOf<String, String>()
            headers["User-Agent"] = DeviceForger.requireRandomDevice().userAgent
            if (originalUrl != null) {
                var referer = "https://${UtilsOld.stringToURL(originalUrl).host}/"
                if (referer !in listOf(
                        BiliBiliLinks.WWW_REFERER,
                        BiliBiliLinks.SPACE_REFERER,
                        BiliBiliLinks.LIVE_REFERER
                    )) {
                    referer = BiliBiliLinks.WWW_REFERER
                }
                headers["Referer"] = referer
            }
            headers["Accept-Language"] = "zh-CN,zh;q=0.9"
            return headers
        }

        fun getWebSocketHeaders(): LinkedHashMap<String, String> {
            val httpHeaders = linkedMapOf<String, String>()
            httpHeaders["User-Agent"] = DeviceForger.requireRandomDevice().userAgent
            httpHeaders["Accept"] = "*/*"
            httpHeaders["Accept-Language"] = "zh-CN,zh;q=0.9"
            httpHeaders["Accept-Encoding"] = "gzip, deflate, br"
            httpHeaders["Sec-WebSocket-Version"] = "13"
            httpHeaders["Origin"] = "https://www.piesocket.com"
            httpHeaders["Sec-WebSocket-Extensions"] = "permessage-deflate"
            httpHeaders["Sec-WebSocket-Key"] = "9cwI/6tCIyNBM4XSsi3jMA=="
            httpHeaders["Connection"] = "keep-alive, Upgrade"
            httpHeaders["Sec-Fetch-Dest"] = "websocket"
            httpHeaders["Sec-Fetch-Mode"] = "websocket"
            httpHeaders["Sec-Fetch-Site"] = "cross-site"
            httpHeaders["Pragma"] = "no-cache"
            httpHeaders["Cache-Control"] = "no-cache"
            httpHeaders["Upgrade"] = "websocket"
            return httpHeaders
        }

        var sponsorBlockApiUrl: String = "https://bsbsb.top/api/"
        fun getSponsorBlockHeaders(): LinkedHashMap<String, String> {
            val headers = linkedMapOf<String, String>()
            headers["Origin"] = "PipePipe"
            return headers
        }

        fun getLoggedHeadersOrNull(originalUrl: String?, condition: String): LinkedHashMap<String, String>? {
            return null
//            return if (ExtractorContext.ServiceList.BiliBili.hasTokens) {
//                val headers = getUserAgentHeaders(originalUrl)
//                headers["Cookie"] = ExtractorContext.ServiceList.BiliBili.tokens!!
//                headers
//            } else null
        }

        fun isBiliBiliDownloadUrl(url: String): Boolean {
            // *.akamaized.net, *.bilivideo.com
            return url.contains("akamaized.net") || url.contains("bilivideo.com")
        }

        fun isBiliBiliUrl(url: String): Boolean {
            return url.contains("bilibili.com")
        }

        fun getResolution(code: Int): String {
            return when (code) {
                127 -> "8K 超高清"
                126 -> "杜比视界"
                125 -> "HDR 真彩色"
                120 -> "4K 超清"
                116 -> "1080P60 高帧率"
                112 -> "1080P+ 高码率"
                80 -> "1080P 高清"
                74 -> "720P60 高帧率"
                64 -> "720P 高清"
                32 -> "480P 清晰"
                16 -> "360P 流畅"
                6 -> "240P 极速"
                else -> "Unknown resolution"
            }
        }

        fun getBitrate(code: Int): String {
            //30216 	64K
            //30232 	132K
            //30280 	192K
            //30250 	杜比全景声
            //30251 	Hi-Res无损
            return when (code) {
                30216 -> "64K"
                30232 -> "132K"
                30280 -> "192K"
                30250, 30255 -> "杜比全景声"
                30251 -> "Hi-Res无损"
                else -> "Unknown bitrate"
            }
        }
    }
    val sortFilters = listOf(
        SearchFilterGroup(
            groupName = "sortby",
            onlyOneCheckable = true,
            availableSearchFilterItems = listOf(
                SearchFilterItem("sort_overall", "order=totalrank"),
                SearchFilterItem("sort_view", "order=click"),
                SearchFilterItem("sort_publish_time", "order=pubdate"),
                SearchFilterItem("sort_bullet_comments", "order=dm"),
                SearchFilterItem("sort_comments", "order=scores"),
                SearchFilterItem("sort_bookmark", "order=stow")
            ),
            defaultFilter = SearchFilterItem("sort_overall", "order=totalrank")
        ),
        SearchFilterGroup(
            groupName = "duration",
            onlyOneCheckable = true,
            availableSearchFilterItems = listOf(
                SearchFilterItem("< 10 min", "duration=1"),
                SearchFilterItem("10-30 min", "duration=2"),
                SearchFilterItem("30-60 min", "duration=3"),
                SearchFilterItem("> 60 min", "duration=4")
            )
        )
    )

    override suspend fun getCookieExtractor(): CookieExtractor {
        return BiliBiliCookieExtractor()
    }


    override fun route(url: String) = BiliBiliUrlRouter.route(url)
    override val serviceInfo: SupportedServiceInfo
        get() = SupportedServiceInfo(
            serviceId = "BILIBILI",
            suggestionPayload = Payload(
                RequestMethod.GET,
                GET_SUGGESTION_URL
            ),
            suggestionStringPath = Pair("/result/tag", "/value"),
            suggestionJsonBetween = null,
            availableSearchTypes = listOf(
                SearchType("videos", "$SEARCH_BASE_URL?search_type=video&keyword=", sortFilters),
                SearchType("lives", "$SEARCH_BASE_URL?search_type=live_room&keyword="),
                SearchType("channels", "$SEARCH_BASE_URL?search_type=bili_user&keyword="),
                SearchType("animes", "$SEARCH_BASE_URL?search_type=media_bangumi&keyword="),
                SearchType("movies_and_tv", "$SEARCH_BASE_URL?search_type=media_ft&keyword=")
            ),
            trendingList = listOf(
                BiliBiliLinks.FETCH_RECOMMENDED_VIDEOS_URL,
                BiliBiliLinks.FETCH_RECOMMENDED_LIVES_URL,
                BiliBiliLinks.FETCH_TOP_100_URL
            ),
            feedFetchInterval = 3000,
            themeColor = "#FB7299"
        )
}
