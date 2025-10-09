//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import com.fasterxml.jackson.databind.JsonNode
//import project.pipepipe.extractor.Extractor
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.services.bilibili.Utils
//import project.pipepipe.extractor.services.bilibili.dataparser.BiliBiliStreamInfoDataParser
//import project.pipepipe.extractor.state.State
//import project.pipepipe.extractor.utils.json.asJson
//import project.pipepipe.extractor.utils.json.requireArray
//import project.pipepipe.extractor.utils.json.requireObject
//import project.pipepipe.extractor.utils.json.requireString
//import project.pipepipe.shared.infoitem.PlaylistInfo
//import project.pipepipe.shared.infoitem.StreamInfo
//import project.pipepipe.shared.job.*
//import java.net.URLDecoder
//
//class BiliBiliPlaylistExtractor(url: String) : Extractor<PlaylistInfo, StreamInfo>(url) {
//
//    override suspend fun fetchInfo(currentState: State?, clientResults: List<TaskResult>?): JobStepResult {
//        if (clientResults == null) {
//            return JobStepResult.ContinueWith(listOf(
//                ClientTask(taskId = "info", payload = Payload(RequestMethod.GET, url, headers = BilibiliService.getHeaders(url))),
//                ClientTask(taskId = "user_data", payload = Payload(RequestMethod.GET, BiliBiliLinks.QUERY_USER_INFO_URL + Utils.getMidFromRecordApiUrl(url), headers = BilibiliService.getHeaders(url))),
//            ))
//        }
//        val data = clientResults.first { it.taskId == "info" }.result!!.asJson()
//
//        val type = when {
//            url.contains(BiliBiliLinks.GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL) -> "seasons_archives"
//            url.contains(BiliBiliLinks.GET_SERIES_BASE_URL) -> "series"
//            url.contains(BiliBiliLinks.GET_PARTITION_URL) -> "partition"
//            else -> "archives"
//        }
//
//        if (type == "partition") {
//            return handlePartitionPlaylist(data)
//        } else {
//            return handleRegularPlaylist(data, clientResults.first { it.taskId == "user_data" }.result!!.asJson(), type)
//        }
//    }
//
//    private fun handlePartitionPlaylist(data: JsonNode):JobStepResult {
//        val relatedArray = data.requireObject("data").requireArray("data")
//        val thumbnailUrl = safeGet{URLDecoder.decode(url.split("thumbnail=")[1].split("&")[0], "UTF-8")}
//        val uploaderName = safeGet{ URLDecoder.decode(url.split("uploaderName=")[1].split("&")[0], "UTF-8") }
//
//        relatedArray.forEachIndexed { index, item ->
//            commit<StreamInfo>(
//                BiliBiliStreamInfoDataParser.parseFromRelatedInfoJson(
//                    item, url.split("bvid=")[1].split("&")[0], thumbnailUrl, index + 1,
//                    uploaderName
//                )
//            )
//        }
//
//        return JobStepResult.CompleteWith(ExtractResult(PlaylistInfo(
//            url = url,
//            name = URLDecoder.decode(url.split("name=")[1].split("&")[0], "UTF-8"),
//            thumbnailUrl = thumbnailUrl,
//            uploaderUrl = safeGet{ URLDecoder.decode(url.split("uploaderUrl=")[1].split("&")[0], "UTF-8") },
//            uploaderAvatarUrl = safeGet{URLDecoder.decode(url.split("uploaderAvatar=")[1].split("&")[0], "UTF-8")},
//            uploaderName = uploaderName,
//            streamCount = relatedArray.size().toLong()
//        ), errors, PagedData(itemList, null)))
//    }
//
//    private fun handleRegularPlaylist(data: JsonNode, userData: JsonNode, type: String): JobStepResult {
//        val playlistData = data.requireObject("data")
//        val userCard = userData.requireObject("data").requireObject("card")
//        val uploaderName = safeGet{ userCard.requireString("name") }
//        val uploaderAvatarUrl = safeGet{ userCard.requireString("face").replace("http:", "https:") }
//
////        if (type == "seasons_archives" || type == "series") {
////            val metaObject = playlistData.requireObject("meta")
////            safeSet { thumbnailUrl = metaObject.requireString("cover") }
////            safeSet { streamCount = playlistData.requireObject("page").requireLong("total") }
////        }
//
//        val archives = playlistData.requireArray("archives")
//        archives.forEach { archive ->
//            commit<StreamInfo>(
//                BiliBiliStreamInfoDataParser.parseFromWebChannelInfoResponseJson(
//                    archive, uploaderName, uploaderAvatarUrl
//                )
//            )
//        }
//        return JobStepResult.CompleteWith(ExtractResult(PlaylistInfo(
//            url = url,
//            name = URLDecoder.decode(url.split("name=")[1].split("&")[0], "UTF-8"),
//            uploaderUrl = BiliBiliLinks.CHANNEL_BASE_URL + Utils.getMidFromRecordApiUrl(url),
//            uploaderName = uploaderName,
//            uploaderAvatarUrl = uploaderAvatarUrl
//        ), errors, PagedData(itemList, null)))
//    }
//}