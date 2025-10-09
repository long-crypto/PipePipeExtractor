package project.pipepipe.extractor.services.bilibili

import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import project.pipepipe.extractor.ExtractorContext

object BiliBiliUrlParser {
    // General form of stream link url: https://m.bilibili.com/video/<ID> (mobile) and https://www.bilibili.com/video/<ID> (PC)
    // but there are also things like www.bilibili.com/bangumi
    fun parseStreamId(url: String): String?{
        if ("bilibili.com" !in url && "b23.tv" !in url) {
            return null
        }
        var processedUrl = url

        if (url.contains("b23.tv")) {
            processedUrl = runBlocking {
                ExtractorContext.downloader.get("https://b23.wtf/api?full=${url.split("://")[1]}&status=200")
                    .bodyAsText().trim()
            }
        }

        var p = "1"
        var t = "0"

        if (processedUrl.contains("p=")) {
            p = processedUrl.split("p=")[1].split("&")[0]
        }
        if (processedUrl.contains("t=")) {
            t = processedUrl.split("t=")[1].split("&")[0]
        }
        if (processedUrl.endsWith("/")) {
            processedUrl = processedUrl.substring(0, processedUrl.length - 1)
        }
        if (processedUrl.contains("/?")) {
            processedUrl = processedUrl.replace("/?", "?")
        }

        val urlParts = processedUrl.split("/")
        val lastPart = urlParts[urlParts.size - 1]

        var result =  when {
            lastPart.startsWith("BV") -> {
                val parseResult = processedUrl.split("/BV")[1].split("\\?".toRegex())[0].split("/")[0]
                "BV$parseResult?p=$p"
            }
            processedUrl.contains("bvid=") -> {
                val parseResult = processedUrl.split("bvid=")[1].split("&")[0]
                "$parseResult?p=$p"
            }
            lastPart.startsWith("av") -> {
                val parseResult = processedUrl.split("av")[1].split("\\?".toRegex())[0]
                "${Utils.av2bv(parseResult.toLong())}?p=$p"
            }
            processedUrl.contains("aid=") -> {
                val parseResult = processedUrl.split("aid=")[1].split("&")[0]
                "${Utils.av2bv(parseResult.toLong())}?p=$p"
            }
            processedUrl.contains(BiliBiliLinks.LIVE_BASE_URL) || processedUrl.contains("bangumi/play/") -> {
                urlParts[urlParts.size - 1].split("\\?".toRegex())[0]
            }
            else -> null
        }

        if (t != "0") {
            result += "#timestamp=$t"
        }

        return result
    }

    fun urlFromStreamID(id: String): String {
        return when {
            id.startsWith("BV") -> "https://www.bilibili.com/video/$id"
            id.startsWith("ss") || id.startsWith("ep") -> "https://www.bilibili.com/bangumi/play/$id"
            else -> "https://live.bilibili.com/$id"
        }
    }

    fun parseCommentsId(url: String): String? {
        if ("bilibili.com" !in url && "api.bilibili.com" !in url) {
            return null
        }

        return when {
            url.contains("live.bilibili.com") -> "LIVE"

            url.contains("bangumi/play/") -> {
//                val episodeId = url.split("bangumi/play/")[1].split("\\?".toRegex())[0]
//                BilibiliService.Companion.watchDataCache.getBvid(episodeId)
                null
            }

            url.contains("api.bilibili.com/x/v2/reply") && url.contains("oid=") -> {
                if (url.contains("api.bilibili.com/x/v2/reply/reply")) {
                    url.split("oid=")[1]
                } else {
                    url.split("oid=")[1].split("&")[0]
                }
            }

            else -> {
                // Try to parse as stream URL
                parseStreamId(url)?.let { streamId ->
                    Utils.getPureBV(streamId)
                }
            }
        }
    }

    fun urlFromCommentsId(id: String, cookie: String): String {
        val processedId = if (id.startsWith("BV")) {
            Utils.bv2av(id).toString()
        } else {
            id
        }

        return if (processedId.contains("&root")) {
            // Reply to specific comment - pn must be placed at the end
            "https://api.bilibili.com/x/v2/reply/reply?type=1&ps=20&oid=$processedId&pn=1"
        } else {
            // Main comments
            val params = linkedMapOf(
                "oid" to processedId,
                "type" to "1",
                "mode" to "3",
                "pagination_str" to "{\"offset\":\"\"}",
                "plat" to "1",
                "web_location" to "1315875"
            )
            Utils.getWbiResult(BiliBiliLinks.FETCH_COMMENTS_URL, params, cookie)
        }
    }

    fun parseChannelId(url: String): String? {
        if ("bilibili.com" !in url) {
            return null
        }

        if (url.contains("mid=")) {
            return url.split("mid=")[1].split("&")[0]
        }
        
        var processedUrl = url.split("?")[0]
        if (processedUrl.endsWith("/")) {
            processedUrl = processedUrl.substring(0, processedUrl.length - 1)
        }
        
        val baseUrl = "https://space.bilibili.com/"
        
        return when {
            processedUrl.contains(baseUrl) || processedUrl.contains("/space/") -> {
                val urlParts = processedUrl.split("/")
                urlParts[urlParts.size - 1]
            }
            else -> null
        }
    }

    fun urlFromChannelId(id: String): String {
        return "https://space.bilibili.com/$id"
    }
}