//package project.pipepipe.extractor.services.bilibili.dataparser
//
//import com.fasterxml.jackson.databind.JsonNode
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.metainfo.BiliBiliChannelTabMetaInfo
//import project.pipepipe.extractor.utils.json.requireLong
//import project.pipepipe.extractor.utils.json.requireObject
//import project.pipepipe.extractor.utils.json.requireString
//import project.pipepipe.shared.infoitem.PlaylistInfo
//
//object BiliBiliPlaylistInfoDataParser {
//    fun parseFromPlaylistInfoJson(itemObject: JsonNode, type: BiliBiliChannelTabMetaInfo.BiliBiliPlaylistType): () -> PlaylistInfo = {
//        val metaObject = itemObject.requireObject("meta")
//
//        val url = when (type) {
//            BiliBiliChannelTabMetaInfo.BiliBiliPlaylistType.SEASON -> String.Companion.format(
//                BiliBiliLinks.GET_SEASON_ARCHIVES_LIST_RAW_URL,
//                metaObject.requireLong("mid"),
//                metaObject.requireLong("season_id"),
//                metaObject.requireString("name")
//            )
//            BiliBiliChannelTabMetaInfo.BiliBiliPlaylistType.SERIES -> String.format(
//                BiliBiliLinks.GET_SERIES_RAW_URL,
//                metaObject.requireLong("mid"),
//                metaObject.requireLong("series_id"),
//                metaObject.requireString("name")
//            )
//        }
//
//        PlaylistInfo(
//            url = url,
//            name = metaObject.requireString("name"),
//            thumbnailUrl = metaObject.requireString("cover").replace("http:", "https:"),
//            streamCount = metaObject.requireLong("total")
//        )
//    }
//}
