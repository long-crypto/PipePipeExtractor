package project.pipepipe.extractor.services.youtube.extractor

import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.services.youtube.YouTubeLinks.BROWSE_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.TAB_RAW_URL
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.WEB_HEADER
import project.pipepipe.extractor.services.youtube.YouTubeRequestHelper.getChannelInfoBody
import project.pipepipe.extractor.services.youtube.dataparser.YouTubeStreamInfoDataParser.parseVideoRenderer
import project.pipepipe.shared.getQueryValue
import project.pipepipe.shared.infoitem.ChannelTabType
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.job.ClientTask
import project.pipepipe.shared.job.ExtractResult
import project.pipepipe.shared.job.JobStepResult
import project.pipepipe.shared.job.PagedData
import project.pipepipe.shared.job.Payload
import project.pipepipe.shared.job.RequestMethod
import project.pipepipe.shared.job.TaskResult
import project.pipepipe.shared.job.isDefaultTask
import project.pipepipe.shared.state.PlainState
import project.pipepipe.shared.state.State
import project.pipepipe.shared.utils.json.asJson
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireObject
import project.pipepipe.shared.utils.json.requireString
import java.net.URLEncoder

class YouTubeChannelLiveTabExtractor(
    url: String,
) : Extractor<Nothing, StreamInfo>(url) {
    override suspend fun fetchFirstPage(
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
                            RequestMethod.POST,
                            BROWSE_URL,
                            WEB_HEADER,
                            getChannelInfoBody(getQueryValue(url, "id")!!, ChannelTabType.LIVES)
                        )
                    )
                ), PlainState(0)
            )
        } else {
            val result = clientResults!!.first { it.taskId.isDefaultTask() }.result!!.asJson()
            val name = result.requireString("/metadata/channelMetadataRenderer/title")
            val nameEncoded = URLEncoder.encode(name, "UTF-8")
            var nextPageUrl: String? = null
            result.requireArray("/contents/twoColumnBrowseResultsRenderer/tabs").forEach {
                val id = runCatching { it.requireString("/tabRenderer/endpoint/browseEndpoint/browseId") }.getOrNull()
                if (runCatching{ it.requireString("/tabRenderer/title") }.getOrNull() == "Live") {
                    it.requireArray("/tabRenderer/content/richGridRenderer/contents").mapNotNull {
                        runCatching{ it.requireObject("/richItemRenderer/content") }.getOrNull()?.let {
                            commit { parseVideoRenderer(it, name, id) }
                        }
                    }
                    runCatching{
                        it.requireArray("/tabRenderer/content/richGridRenderer/contents")
                            .last()
                            .requireString("/continuationItemRenderer/continuationEndpoint/continuationCommand/token")
                    }.getOrNull()?.let {
                        nextPageUrl = "$TAB_RAW_URL?id=$id&type=lives&continuation=$it&name=$nameEncoded"
                    }
                }
            }
            return JobStepResult.CompleteWith(
                ExtractResult(errors = errors, pagedData = PagedData(
                    itemList = itemList,
                    nextPageUrl = nextPageUrl
                )))
        }
    }

    override suspend fun fetchGivenPage(
        url: String,
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        return YouTubeChannelMainTabExtractor(url).fetchGivenPage(url, sessionId, currentState, clientResults, cookie)
    }
}