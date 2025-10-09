//package project.pipepipe.extractor.services.bilibili.metainfo
//
//import project.pipepipe.extractor.ExtractorContext
//import project.pipepipe.extractor.SuggestionExtractor
//import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
//import project.pipepipe.extractor.services.bilibili.BilibiliService
//import project.pipepipe.extractor.utils.json.bodyAsJson
//import project.pipepipe.extractor.utils.json.requireArray
//import project.pipepipe.extractor.utils.json.requireObject
//import project.pipepipe.extractor.utils.json.requireString
//
//class BiliBiliSuggestionMetaInfo : SuggestionExtractor() {
//
//    override suspend fun getSuggestionList(query: String): List<String> {
//        val data = ExtractorContext.downloader.get(
//            BiliBiliLinks.GET_SUGGESTION_URL + query,
//            BilibiliService.Companion.getHeaders(BiliBiliLinks.WWW_REFERER)
//        ).bodyAsJson()
//
//        val resultList = mutableListOf<String>()
//        val respObject = data.requireObject("result").requireArray("tag")
//        respObject.forEach { item ->
//            resultList.add(item.requireString("value"))
//        }
//
//        return resultList
//    }
//}