package project.pipepipe.extractor.services.bilibili.metainfo

import org.jsoup.Jsoup
import project.pipepipe.extractor.Extractor
import project.pipepipe.shared.infoitem.DanmakuInfo
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
import project.pipepipe.extractor.services.bilibili.BilibiliService
import project.pipepipe.extractor.services.bilibili.Utils
import project.pipepipe.extractor.services.bilibili.dataparser.BiliBIliDanmakuInfoDataParser
import project.pipepipe.shared.getQueryValue
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
import kotlin.io.encoding.Base64

class BilibiliDanmakuExtractor(
    url: String,
)  : Extractor<Nothing, DanmakuInfo>(url){
    override suspend fun fetchFirstPage(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult {
        if (currentState == null) {
            return JobStepResult.ContinueWith(listOf(
                ClientTask(
                    payload = Payload(
                        RequestMethod.GET,
                        "${BiliBiliLinks.QUERY_VIDEO_BULLET_COMMENTS_URL}${getQueryValue(url, "cid")}",
                        BilibiliService.getUserAgentHeaders(BiliBiliLinks.WWW_REFERER),
                        shouldReturnBase64Bytes = true
                    )
                )
            ), state = PlainState(0))
        } else {
            val data = Jsoup.parse(
                String(
                    Utils.decompress(
                        Base64.decode(clientResults!!.first { it.taskId.isDefaultTask() }.result!!)
                    )!!
                ))
            if (data.select("state").text() == "1") {
                return JobStepResult.CompleteWith(ExtractResult())
            }
            data.select("d").forEach{ element ->
                val attr = element.attr("p").split(",")
                if (attr.size > 5 && attr[5].toInt() == 3) { // voting
                    return@forEach
                }
                commit { BiliBIliDanmakuInfoDataParser.parseFromDanmakuElement(element) }
            }
            return JobStepResult.CompleteWith(ExtractResult(errors = errors, pagedData = PagedData(itemList, null)))
        }
    }

//
//    override suspend fun getLiveMessages(): List<DanmakuInfo> {
//        if (!isLiveStream || webSocketClient == null) {
//            return emptyList()
//        }
//
//        val messages = webSocketClient?.messages ?: return emptyList()
//        val danmakuItems = mutableListOf<DanmakuInfo>()
//
//        for (message in messages) {
//            val cmd = message.requireString("cmd")
//            try {
//                when {
//                    cmd == "DANMU_MSG" -> {
//                        danmakuItems.add(BiliBIliDanmakuInfoDataParser.parseFromLiveDanmakuJson(message, startTime))
//                    }
//                    cmd.contains("SUPER_CHAT_MESSAGE") -> {
//                        danmakuItems.add(BiliBIliDanmakuInfoDataParser.parseFromLiveDanmakuJson(message, startTime))
//                    }
//                }
//            } catch (e: Exception) {
//                // Skip malformed messages
//            }
//        }
//
//        return danmakuItems
//    }
//
//    override val isLive: Boolean
//        get() = isLiveStream
//
//    override suspend fun disconnect() {
//        webSocketClient?.disconnect()
//    }
//
//    override suspend fun reconnect() {
//        if (webSocketClient?.webSocketClient?.isClosed == true) {
//            try {
//                webSocketClient?.wrappedReconnect()
//            } catch (e: URISyntaxException) {
//                throw RuntimeException(e)
//            } catch (e: InterruptedException) {
//                throw RuntimeException(e)
//            }
//        }
//    }

    private fun getId(): String {
        return url!!.split("/").last().split("?")[0]
    }

//    private suspend fun initLiveStream() {
//        try {
//            roomId = BilibiliService.Companion.watchDataCache.getRoomId()
//            startTime = BilibiliService.Companion.watchDataCache.getStartTime()
//            val response = ExtractorContext.downloader.get("${BiliBiliLinks.QUERY_DANMU_INFO_URL}$roomId").bodyAsJson()
//            val token = response.requireObject("data").requireString("token")
//            webSocketClient = BilibiliWebSocketClient(roomId, token)
//            webSocketClient?.webSocketClient?.connectBlocking()
//            isLiveStream = true
//        } catch (e: URISyntaxException) {
//            throw RuntimeException(e)
//        } catch (e: InterruptedException) {
//            throw RuntimeException(e)
//        } catch (e: Exception) {
//            throw ParsingException("Failed to connect to live chat", e)
//        }
//    }
}