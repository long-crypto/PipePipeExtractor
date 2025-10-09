package project.pipepipe.extractor.services.bilibili.dataparser

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringEscapeUtils
import project.pipepipe.extractor.services.bilibili.BiliBiliLinks
import project.pipepipe.extractor.services.bilibili.BiliBiliUrlParser
import project.pipepipe.extractor.services.bilibili.Utils
import project.pipepipe.extractor.utils.getDurationFromString
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.infoitem.StreamType
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireInt
import project.pipepipe.shared.utils.json.requireLong
import project.pipepipe.shared.utils.json.requireObject
import project.pipepipe.shared.utils.json.requireString

object BiliBiliStreamInfoDataParser {
    fun parseFromStreamInfoJson(item: JsonNode): () -> StreamInfo = {
        StreamInfo(
            url = BiliBiliUrlParser.urlFromStreamID(BiliBiliUrlParser.parseStreamId(item.requireString("arcurl"))!!),
            id = BiliBiliUrlParser.parseStreamId(item.requireString("arcurl"))!!,
            name = StringEscapeUtils.unescapeHtml4(
                item.requireString("title")
                    .replace("<em class=\"keyword\">", "")
                    .replace("</em>", "")
            ),
            serviceId = "BILIBILI",
            thumbnailUrl = "https:" + item.requireString("pic"),
            streamType = StreamType.VIDEO_STREAM,
            duration = getDurationFromString(item.requireString("duration")),
            viewCount = item.requireLong("play"),
            uploaderName = item.requireString("author"),
            uploaderAvatarUrl = item.requireString("upic").replace("http:", "https:"),
            uploadDate = item.requireLong("pubdate") * 1000,
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromTrendingInfoJson(item: JsonNode): () -> StreamInfo = {
        StreamInfo(
            url = BiliBiliLinks.VIDEO_BASE_URL + item.requireString("bvid") + "?p=1",
            id = item.requireString("bvid"),
            name = item.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = item.requireString("pic").replace("http:", "https:"),
            streamType = StreamType.VIDEO_STREAM,
            duration = item.requireLong("duration"),
            viewCount = item.requireObject("stat").requireLong("view"),
            uploaderName = item.requireObject("owner").requireString("name"),
            uploaderAvatarUrl = item.requireObject("owner").requireString("face"),
            uploadDate = item.requireLong("pubdate"),
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    // 处理相关视频信息
    fun parseFromRelatedInfoJson(item: JsonNode): () -> StreamInfo = {
        val actualId = try {
            item.requireString("bvid")
        } catch (e: Exception) {
            Utils.av2bv(item.requireLong("aid"))
        }

        StreamInfo(
            url = "${BiliBiliLinks.VIDEO_BASE_URL}$actualId",
            id = actualId,
            name = item.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = item.requireString("pic").replace("http", "https"),
            streamType = StreamType.VIDEO_STREAM,
            duration = item.requireLong("duration"),
            viewCount = item.requireObject("stat").requireLong("view"),
            uploaderName = item.requireObject("owner").requireString("name"),
            uploaderAvatarUrl = item.requireObject("owner").requireString("face").replace("http", "https"),
            uploadDate = item.requireLong("pubdate") * 1000,
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    // 处理分P视频信息
    fun parseFromPartitionInfoJson(
        item: JsonNode,
        id: String,
        p: Int = 1,
    ): () -> StreamInfo = {
        StreamInfo(
            url = "${BiliBiliLinks.VIDEO_BASE_URL}$id?p=$p",
            id = id,
            name = item.requireString("part"),
            serviceId = "BILIBILI",
            streamType = StreamType.VIDEO_STREAM,
            duration = item.requireLong("duration"),
            viewCount = -1,
            uploadDate = item.requireLong("ctime") * 1000,
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }


    fun parseFromRecommendLiveInfoJson(data: JsonNode): () -> StreamInfo = {
        val thumbnailUrl = try {
            data.requireString("user_cover")
        } catch (e: Exception) {
            data.requireString("system_cover")
        }
        StreamInfo(
            url = "https://${BiliBiliLinks.LIVE_BASE_URL}/${data.requireLong("roomid")}",
            id = data.requireLong("roomid").toString(),
            name = data.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = thumbnailUrl.replace("http:", "https:"),
            streamType = StreamType.LIVE_STREAM,
            viewCount = data.requireObject("watched_show").requireLong("num"),
            uploaderName = data.requireString("uname"),
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromRecommendedVideoJson(item: JsonNode): () -> StreamInfo = {
        StreamInfo(
            url = item.requireString("uri") + "?p=1",
            id = BiliBiliUrlParser.parseStreamId(item.requireString("uri"))!!,
            name = item.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = item.requireString("pic").replace("http:", "https:"),
            streamType = StreamType.VIDEO_STREAM,
            duration = item.requireLong("duration"),
            viewCount = item.requireObject("stat").requireLong("view"),
            uploaderName = item.requireObject("owner").requireString("name"),
            uploaderAvatarUrl = item.requireObject("owner").requireString("face"),
            uploadDate = item.requireLong("pubdate"),
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromPremiumContentJson(data: JsonNode): () -> StreamInfo = {
        val getPubtime = {
            try {
                data.requireLong("pubtime") / 1000
            } catch (e: Exception) {
                data.requireLong("pub_time")
            }
        }

        val url = try {
            data.requireString("url")
        } catch (e: Exception) {
            data.requireString("share_url")
        }

        StreamInfo(
            url = url,
            id = BiliBiliUrlParser.parseStreamId(url)!!,
            name = try {
                data.requireString("share_copy")
            } catch (e: Exception) {
                data.requireString("title")
                    .replace("<em class=\"keyword\">", "")
                    .replace("</em>", "")
            },
            serviceId = "BILIBILI",
            thumbnailUrl = data.requireString("cover").replace("http:", "https:"),
            streamType = StreamType.VIDEO_STREAM,
            duration = data.requireLong("duration") / 1000,
            uploaderName = data.requireString("org_title")
                .replace("<em class=\"keyword\">", "")
                .replace("</em>", ""),
            uploadDate = getPubtime(),
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromLiveInfoJson(item: JsonNode, type: Int): () -> StreamInfo = {
        val name = if (item.requireInt("live_status") == 2) {
            item.requireString("uname") + "的投稿视频轮播"
        } else {
            item.requireString("title")
                .replace("<em class=\"keyword\">", "")
                .replace("</em>", "")
        }

        val roomIdField = if (type == 0) "roomid" else "room_id"
        val url = "https://live.bilibili.com/" + item.requireLong(roomIdField)

        StreamInfo(
            url = url,
            id = item.requireLong(roomIdField).toString(),
            name = name,
            serviceId = "BILIBILI",
            thumbnailUrl = if (type == 1) {
                item.requireString("cover_from_user")
            } else {
                "https:" + item.requireString("user_cover")
            },
            streamType = StreamType.LIVE_STREAM,
            duration = -1,
            viewCount = item.requireLong("online"),
            uploaderName = item.requireString("uname"),
            uploaderAvatarUrl = if (type == 1) {
                item.requireString("face")
            } else {
                "https:" + item.requireString("uface")
            },
                        uploadDate = null,
            isRoundPlayStream = type == 1,
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromClientChannelInfoResponseJson(item: JsonNode, uploaderName: String, uploaderAvatarUrl: String?): () -> StreamInfo = {
        val bvid = item.requireString("bvid")

        StreamInfo(
            url = "${BiliBiliLinks.VIDEO_BASE_URL}$bvid?p=1",
            id = bvid,
            name = item.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = item.requireString("cover").replace("http:", "https:"),
            streamType = StreamType.VIDEO_STREAM,
            duration = try {
                item.requireLong("duration")
            } catch (e: Exception) {
                getDurationFromString(item.requireString("length"))
            },
            viewCount = try {
                item.requireLong("play")
            } catch (e: Exception) {
                item.requireObject("stat").requireLong("view")
            },
            uploaderName = uploaderName,
            uploaderAvatarUrl = uploaderAvatarUrl,
            uploadDate = item.requireLong("ctime"),
            isPaid = item.requireArray("badges").toString().contains("充电专属"),
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }

    fun parseFromWebChannelInfoResponseJson(item: JsonNode): () -> StreamInfo = {
        StreamInfo(
            url = BiliBiliLinks.VIDEO_BASE_URL + item.requireString("bvid") + "?p=1",
            id = item.requireString("bvid"),
            name = item.requireString("title"),
            serviceId = "BILIBILI",
            thumbnailUrl = item.requireString("pic").replace("http:", "https:"),
            streamType = StreamType.VIDEO_STREAM,
            duration = try {
                item.requireLong("duration")
            } catch (e: Exception) {
                getDurationFromString(item.requireString("length"))
            },
            viewCount = try {
                item.requireLong("play")
            } catch (e: Exception) {
                item.requireObject("stat").requireLong("view")
            },
            uploaderName = item.requireString("author"),
            uploadDate = if (item.requireInt("created") == 0) item.requireLong("pubdate") * 1000 else item.requireLong("created") * 1000,
            isPaid = item.requireInt("elec_arc_type") == 1,
            headers = hashMapOf("Referer" to "https://www.bilibili.com")
        )
    }
}
