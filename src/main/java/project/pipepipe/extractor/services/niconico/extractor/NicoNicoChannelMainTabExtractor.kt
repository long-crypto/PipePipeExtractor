package project.pipepipe.extractor.services.niconico.extractor

import org.jsoup.Jsoup
import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.services.niconico.NicoNicoService.Companion.GOOGLE_HEADER
import project.pipepipe.extractor.services.niconico.dataparser.NicoNicoStreamInfoDataParser.parseFromRSSXml
import project.pipepipe.extractor.utils.incrementUrlParam
import project.pipepipe.shared.SharedContext
import project.pipepipe.shared.getQueryValue
import project.pipepipe.shared.infoitem.ChannelInfo
import project.pipepipe.shared.infoitem.ChannelTabInfo
import project.pipepipe.shared.infoitem.ChannelTabType
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.job.*
import project.pipepipe.shared.state.PlainState
import project.pipepipe.shared.state.State
import project.pipepipe.shared.utils.json.requireLong
import project.pipepipe.shared.utils.json.requireString
import java.net.URLDecoder
import java.net.URLEncoder

class NicoNicoChannelMainTabExtractor(url: String) : Extractor<ChannelInfo, StreamInfo>(url) {
    override suspend fun fetchInfo(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        val safeUrl = url.substringBefore("?")
        if (currentState == null) {
            return JobStepResult.ContinueWith(listOf(
                ClientTask("info", Payload(RequestMethod.GET, safeUrl, GOOGLE_HEADER)),
                ClientTask("videos", Payload(RequestMethod.GET, "$safeUrl/video?rss=2.0&page=1", GOOGLE_HEADER))
            ), PlainState(0))
        } else {
            val infoData = SharedContext.objectMapper.readTree(
                Jsoup.parse(clientResults!!.first { it.taskId == "info" }.result!!)
                .getElementById("js-initial-userpage-data")!!
                .attr("data-initial-data")
            )
            val userVideoData = Jsoup.parse(clientResults.first { it.taskId == "videos" }.result!!)
            val channelName = infoData.requireString("/state/userDetails/userDetails/user/nickname")
            userVideoData.select("item").forEach {
                commit { parseFromRSSXml(it, channelName) }
            }

            return JobStepResult.CompleteWith(ExtractResult(
                info = ChannelInfo(
                    url = safeUrl,
                    name = channelName,
                    serviceId = "NICONICO",
                    thumbnailUrl = infoData.requireString("/state/userDetails/userDetails/user/icons/large"),
                    subscriberCount = infoData.requireLong("/state/userDetails/userDetails/user/followerCount"),
                    description = infoData.requireString("/state/userDetails/userDetails/user/description"),
                    tabs = listOf(
                        ChannelTabInfo(safeUrl, ChannelTabType.VIDEOS),
//                        ChannelTabInfo("${BiliBiliLinks.GET_SEASON_ARCHIVES_LIST_BASE_URL}?mid=$id&page_num=1&page_size=10", ChannelTabType.PLAYLISTS)
                    )
                ),
                errors = errors,
                pagedData = PagedData(itemList, "$safeUrl/video?rss=2.0&page=2&name=${URLEncoder.encode(channelName, "UTF-8")}")
            ))
        }
    }

    override suspend fun fetchGivenPage(
        url: String,
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        if (currentState == null) {
            return JobStepResult.ContinueWith(listOf(
                ClientTask("videos", Payload(RequestMethod.GET, url, GOOGLE_HEADER))
            ), PlainState(0))
        } else {
            val userVideoData = Jsoup.parse(clientResults!!.first { it.taskId == "videos" }.result!!)
            userVideoData.select("item").forEach {
                commit { parseFromRSSXml(it,URLDecoder.decode(getQueryValue(url, "name"), "UTF-8")) }
            }
            val nextPageUrl  = if (!itemList.isEmpty()) url.incrementUrlParam("page") else null
            return JobStepResult.CompleteWith(ExtractResult(
                errors = errors,
                pagedData = PagedData(itemList, nextPageUrl)
            ))
        }
    }
}