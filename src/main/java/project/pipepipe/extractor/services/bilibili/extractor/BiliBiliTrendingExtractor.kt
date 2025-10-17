package project.pipepipe.extractor.services.bilibili.extractor

import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.ExtractorContext.asJson
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.FETCH_RECOMMENDED_LIVES_URL
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks.FETCH_TOP_100_URL
import project.pipepipe.extractor.services.bilibili.BilibiliService
import project.pipepipe.extractor.services.bilibili.dataparser.BiliBiliStreamInfoDataParser
import project.pipepipe.shared.infoitem.RecommendationInfo
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.job.*
import project.pipepipe.shared.state.PlainState
import project.pipepipe.shared.state.State
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireObject

class BiliBiliTrendingExtractor(url: String) : Extractor<RecommendationInfo, StreamInfo>(url) {
    override suspend fun fetchInfo(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        if (currentState == null) {
            return JobStepResult.ContinueWith(
                listOf(
                    ClientTask(
                        payload = Payload(
                            RequestMethod.GET,
                            url,
                            headers = BilibiliService.getHeadersWithCookie(url, cookie!!)
                        )
                    ),
                ), state = PlainState(1)
            )
        } else {
            val data = clientResults!!.first { it.taskId.isDefaultTask() }.result!!.asJson()
            when (url) {
                FETCH_RECOMMENDED_LIVES_URL -> {
                    val results = data.requireObject("data").requireArray("list")
                    results.forEach { item ->
                        commit { BiliBiliStreamInfoDataParser.parseFromRecommendLiveInfoJson(item) }
                    }
                }

                FETCH_TOP_100_URL -> {
                    val results = data.requireObject("data").requireArray("list")
                    results.forEach { item ->
                        commit { BiliBiliStreamInfoDataParser.parseFromTrendingInfoJson(item) }
                    }
                }
            }
            return JobStepResult.CompleteWith(
                result = ExtractResult(
                    info = RecommendationInfo(url, when (url) {
                        FETCH_TOP_100_URL -> "Top 100"
                        FETCH_RECOMMENDED_LIVES_URL -> "Recommended Lives"
                        else -> error("")
                    }),
                    errors = errors,
                    pagedData = PagedData(
                        itemList, null
                    )
                )
            )
        }
    }
}