package project.pipepipe.extractor.services.bilibili

import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.Router.getType
import project.pipepipe.extractor.Router.resetType
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.COMMENT_REPLIES_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.DANMAKU_RAW_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.FETCH_COMMENTS_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.SEARCH_BASE_URL
import project.pipepipe.extractor.services.bilibili.metainfo.*

object BiliBiliUrlRouter {

    fun route(url: String): Extractor<*,*>? {
        return when {
            url.contains(DANMAKU_RAW_URL) -> BilibiliDanmakuExtractor(url)
            url.getType() == "related" && acceptsStreamUrl(url) -> BiliBiliRelatedItemsExtractor(url.resetType())
            url.contains(FETCH_COMMENTS_URL) || url.contains(COMMENT_REPLIES_URL) -> BiliBiliCommentExtractor(url)
            acceptsStreamUrl(url) -> BiliBiliStreamExtractor(url)
            acceptsChannelUrl(url) -> BiliBiliChannelMainTabExtractor(url)
            url.contains(SEARCH_BASE_URL) -> BiliBiliSearchExtractor(url)
//            acceptsChannelUrl(url) -> BiliBiliChannelExtractor(
//                BiliBiliUrlParser.urlFromChannelId(
//                    BiliBiliUrlParser.parseChannelId(
//                        url
//                    )!!
//                )
//            )

//            acceptsFeedUrl(url) -> BiliBiliRecommendationExtractor(url)
//            acceptsPlaylistUrl(url) -> BiliBiliPlaylistExtractor(url)
            else -> null
        }
    }

    private fun acceptsStreamUrl(url: String): Boolean {
        return try {
            BiliBiliUrlParser.parseStreamId(url) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun acceptsChannelUrl(url: String): Boolean {
        return try {
            BiliBiliUrlParser.parseChannelId(url) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun acceptsFeedUrl(url: String): Boolean {
        return url == BiliBiliLinks.FETCH_RECOMMENDED_VIDEOS_URL ||
                url.contains(BiliBiliLinks.FETCH_RECOMMENDED_LIVES_URL) ||
                url == BiliBiliLinks.FETCH_TOP_100_URL
    }

    private fun acceptsPlaylistUrl(url: String): Boolean {
        return url.contains(BiliBiliLinks.GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL) ||
                url.contains(BiliBiliLinks.GET_SERIES_BASE_URL) ||
                url.contains(BiliBiliLinks.GET_PARTITION_URL)
    }
}