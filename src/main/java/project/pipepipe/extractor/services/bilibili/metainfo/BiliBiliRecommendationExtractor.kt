//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import project.pipepipe.shared.job.ExtractResult
//import project.pipepipe.extractor.Extractor
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.shared.job.PagedData
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.services.bilibili.dataparser.BiliBiliStreamInfoDataParser
//import project.pipepipe.extractor.utils.json.bodyAsJson
//import project.pipepipe.extractor.utils.json.requireArray
//import project.pipepipe.extractor.utils.json.requireObject
//import project.pipepipe.shared.infoitem.Info
//import project.pipepipe.shared.infoitem.RecommendationInfo
//import project.pipepipe.shared.infoitem.StreamInfo
//
//class BiliBiliRecommendationExtractor(url: String) : Extractor<RecommendationInfo, Info>(url) {
//    val name = when (url) {
//        "https://www.bilibili.com" -> "Recommended Videos"
//        BiliBiliLinks.FETCH_RECOMMENDED_LIVES_URL -> "Recommended Lives"
//        "https://api.bilibili.com/x/web-interface/ranking/v2" -> "Top 100"
//        else -> null
//    }
//    override suspend fun fetchInfo(): ExtractResult<RecommendationInfo, Info> {
//
//        val data = when (name) {
//            "Recommended Videos" -> {
//                ExtractorContext.downloader.get("https://api.bilibili.com/x/web-interface/index/top/rcmd?fresh_type=3",
//                    BilibiliService.Companion.getHeaders(url)
//                ).bodyAsJson()
//            }
//            "Top 100" -> {
//                ExtractorContext.downloader.get(url, BilibiliService.Companion.getHeaders(url)).bodyAsJson()
//            }
//            "Recommended Lives" -> {
//                ExtractorContext.downloader.get("${url}&page=1", BilibiliService.Companion.getHeaders(url)).bodyAsJson()
//            }
//            else -> return ExtractResult()
//        }
//
//        when (name) {
//            "Recommended Videos" -> {
//                val results = data.requireObject("data").requireArray("item")
//                results.forEach { item ->
//                    commit<StreamInfo>(
//                        BiliBiliStreamInfoDataParser.parseFromRecommendedVideoJson(item)
//                    )
//                }
//            }
//            "Recommended Lives" -> {
//                val results = data.requireObject("data").requireArray("list")
//                results.forEach { item ->
//                    commit<StreamInfo>(
//                        BiliBiliStreamInfoDataParser.parseFromRecommendLiveInfoJson(item)
//                    )
//                }
//            }
//            "Top 100" -> {
//                val results = data.requireObject("data").requireArray("list")
//                results.forEach { item ->
//                    commit<StreamInfo>(
//                        BiliBiliStreamInfoDataParser.parseFromTrendingInfoJson(item)
//                    )
//                }
//            }
//        }
//        return ExtractResult(errors = errors, pagedData = PagedData(
//            itemList, null
//        ))
//    }
//}