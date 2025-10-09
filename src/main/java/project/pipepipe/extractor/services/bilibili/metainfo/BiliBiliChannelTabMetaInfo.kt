//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.extractor.metainfo.ChannelTabMetaInfo
//import project.pipepipe.extractor.metainfo.ChannelTabs
//import project.pipepipe.shared.infoitem.PlaylistInfo
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.services.bilibili.Utils
//import project.pipepipe.extractor.services.bilibili.dataparser.BiliBiliPlaylistInfoDataParser
//import project.pipepipe.extractor.utils.json.bodyAsJson
//import project.pipepipe.extractor.utils.json.requireArray
//import project.pipepipe.extractor.utils.json.requireLong
//import project.pipepipe.extractor.utils.json.requireObject
//import project.pipepipe.shared.infoitem.Info
//
//class BiliBiliChannelTabMetaInfo(
//    url: String,
//    private val tabName: String
//) : ChannelTabMetaInfo(url, tabName) {
//
//    enum class BiliBiliPlaylistType {
//        SEASON,
//        SERIES
//    }
//
//    override suspend fun init() {
//        url!!
//        when (tabName) {
//            ChannelTabs.VIDEOS -> {
//                // For videos tab, delegate to BiliBiliChannelMetaInfo
//                val channelMetaInfo = BiliBiliChannelExtractor(url)
//                channelMetaInfo.init()
//
//                // Copy the video items
//                channelMetaInfo.itemList.forEach { item ->
//                    itemList.add(item as Info)
//                }
//                hasNextPage = channelMetaInfo.hasNextPage
//            }
//            ChannelTabs.PLAYLISTS -> {
//                processPlaylistsTab()
//            }
//        }
//    }
//
//    override suspend fun fetchNextPage() {
//        when (tabName) {
//            ChannelTabs.VIDEOS -> {
//                // For videos tab, this shouldn't be called as we delegate to ChannelMetaInfo
//                // But if it is called, we can create a new instance and fetch next page
//                val channelMetaInfo = BiliBiliChannelExtractor(url!!)
//                channelMetaInfo.init()
//                channelMetaInfo.fetchNextPage()
//
//                channelMetaInfo.itemList.forEach { item ->
//                    itemList.add(item as Info)
//                }
//
//                hasNextPage = channelMetaInfo.hasNextPage
//            }
//
//            ChannelTabs.PLAYLISTS -> {
//                fetchNextPlaylistsPage()
//            }
//        }
//    }
//
//    private suspend fun processPlaylistsTab() {
//        val data = ExtractorContext.downloader.get(url!!,
//            BilibiliService.Companion.getHeaders(url)
//        ).bodyAsJson().requireObject("data")
//
//        val itemsLists = data.requireObject("items_lists")
//        val seasonsList = itemsLists.requireArray("seasons_list")
//        val seriesList = itemsLists.requireArray("series_list")
//
//        if (seasonsList.size() + seriesList.size() == 0) {
//            hasNextPage = false
//            return
//        }
//
//        seasonsList.forEach { season ->
//            val streamCount = season.requireLong("media_count")
//            if (streamCount > 0) {
//                commit<PlaylistInfo>(
//                    BiliBiliPlaylistInfoDataParser.parseFromPlaylistInfoJson(
//                        season,
//                        BiliBiliPlaylistType.SEASON
//                    )
//                )
//            }
//        }
//        seriesList.forEach { series ->
//            val streamCount = series.requireLong("media_count")
//            if (streamCount > 0) {
//                commit<PlaylistInfo>(BiliBiliPlaylistInfoDataParser.parseFromPlaylistInfoJson(series, BiliBiliPlaylistType.SERIES))
//            }
//        }
//
//        hasNextPage = true
//    }
//
//    private suspend fun fetchNextPlaylistsPage() {
//        val nextPageUrl = Utils.getNextPageFromCurrentUrl(url!!, "page_num", 1)
//        val data = ExtractorContext.downloader.get(nextPageUrl, BilibiliService.Companion.getHeaders(nextPageUrl)).bodyAsJson().requireObject("data")
//
//        val itemsLists = data.requireObject("items_lists")
//        val seasonsList = itemsLists.requireArray("seasons_list")
//        val seriesList = itemsLists.requireArray("series_list")
//
//        if (seasonsList.size() + seriesList.size() == 0) {
//            hasNextPage = false
//            return
//        }
//
//        seasonsList.forEach { season ->
//            val streamCount = season.requireLong("media_count")
//            if (streamCount > 0) {
//                commit<PlaylistInfo>(
//                    BiliBiliPlaylistInfoDataParser.parseFromPlaylistInfoJson(
//                        season,
//                        BiliBiliPlaylistType.SEASON
//                    )
//                )
//            }
//        }
//        seriesList.forEach { series ->
//            val streamCount = series.requireLong("media_count")
//            if (streamCount > 0) {
//                commit<PlaylistInfo>(
//                    BiliBiliPlaylistInfoDataParser.parseFromPlaylistInfoJson(
//                        series,
//                        BiliBiliPlaylistType.SERIES
//                    )
//                )
//            }
//        }
//
//        hasNextPage = true
//    }
//}