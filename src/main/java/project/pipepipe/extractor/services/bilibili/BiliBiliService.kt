package project.pipepipe.extractor.services.bilibili
import project.pipepipe.extractor.StreamingService
import project.pipepipe.extractor.base.CookieExtractor
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.GET_SUGGESTION_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.SEARCH_BASE_URL
import project.pipepipe.extractor.services.bilibili.extractor.BiliBiliCookieExtractor
import project.pipepipe.extractor.utils.UtilsOld
import project.pipepipe.shared.infoitem.TrendingInfo
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
        fun getHeadersWithCookie(originalUrl: String, cookie: String): LinkedHashMap<String, String> {
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
                TrendingInfo(BiliBiliLinks.FETCH_TOP_100_URL, "BILIBILI", "trending"),
                TrendingInfo(BiliBiliLinks.FETCH_RECOMMENDED_LIVES_URL, "BILIBILI", "trending_live"),
            ),
            feedFetchInterval = 3000,
            themeColor = "#FB7299"
        )
}
