package project.pipepipe.extractor.services.youtube.dataparser

import com.fasterxml.jackson.databind.JsonNode
import project.pipepipe.extractor.services.youtube.YouTubeLinks.CHANNEL_URL
import project.pipepipe.extractor.services.youtube.YouTubeLinks.STREAM_URL
import project.pipepipe.extractor.utils.TimeAgoParser
import project.pipepipe.extractor.utils.extractDigitsAsLong
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireString
import project.pipepipe.shared.infoitem.StreamInfo
import project.pipepipe.shared.infoitem.StreamType

object YouTubeStreamInfoDataParser {
    fun parseFromVideoRenderer(data: JsonNode, overrideChannelName: String? = null, overrideChannelId: String? = null): StreamInfo {
        if (!data.has("videoRenderer")) {
            return parseFromLockupViewModel(data, overrideChannelName,  overrideChannelName)
        }
        val isLive = when {
            runCatching { data.requireString("/videoRenderer/badges/0/metadataBadgeRenderer/style") }.getOrNull() == "BADGE_STYLE_TYPE_LIVE_NOW" -> true
            runCatching{ data.requireString("/videoRenderer/badges/0/metadataBadgeRenderer/label") }.getOrNull()?.startsWith("LIVE") == true -> true
            else -> false
        }

        return StreamInfo(
            url = STREAM_URL + data.requireString("/videoRenderer/videoId"),
            serviceId = "YOUTUBE",
            name = data.requireString("/videoRenderer/title/runs/0/text"),
            uploaderName = runCatching{ data.requireString("/videoRenderer/longBylineText/runs/0/text") }.getOrDefault(overrideChannelName),
            uploaderUrl = runCatching{ CHANNEL_URL + data.requireString("/videoRenderer/longBylineText/runs/0/navigationEndpoint/browseEndpoint/browseId") }.getOrDefault(CHANNEL_URL + overrideChannelId),
            uploaderAvatarUrl = runCatching{
                data.requireArray("/videoRenderer/channelThumbnailSupportedRenderers/channelThumbnailWithLinkRenderer/thumbnail/thumbnails")
                    .first().requireString("url")
            }.getOrNull(),
            thumbnailUrl = data.requireArray("/videoRenderer/thumbnail/thumbnails").last().requireString("url"),
            isPaid = runCatching{ data.requireString("/videoRenderer/badges/0/metadataBadgeRenderer/style") == "BADGE_STYLE_TYPE_MEMBERS_ONLY" }.getOrDefault(false),
        ).apply {
            when (isLive) {
                false -> {
                    streamType = StreamType.VIDEO_STREAM
                    uploadDate = TimeAgoParser.parseToTimestamp(data.requireString("/videoRenderer/publishedTimeText/simpleText"))
                    duration = parseDurationString(data.requireString("/videoRenderer/lengthText/simpleText"))
                    viewCount = runCatching { data.requireString("/videoRenderer/viewCountText/simpleText").extractDigitsAsLong() }.getOrNull()
                }
                true -> {
                    streamType = StreamType.LIVE_STREAM
                    viewCount = data.requireString("/videoRenderer/viewCountText/runs/0/text").extractDigitsAsLong()
                    isShort = false //todo
                }
            }
        }
    }

//    fun parseShortsRenderer(data: JsonNode): StreamInfo {
//        return StreamInfo(
//            url = BASE_URL + data.requireString("/videoRenderer/videoId"),
//            serviceId = "YOUTUBE",
//            name = data.requireString("/videoRenderer/title/runs/0/text"),
//            streamType = StreamType.VIDEO_STREAM,
//            uploaderName = data.requireString("/videoRenderer/longBylineText/runs/0/text"),
//            uploaderUrl = data.requireString("/videoRenderer/longBylineText/runs/0/navigationEndpoint/browseEndpoint/browseId"),
//            uploaderAvatarUrl = data.requireArray("/videoRenderer/channelThumbnailSupportedRenderers/channelThumbnailWithLinkRenderer/thumbnail/thumbnails").first().requireString("url"),
//            uploadDate = TimeAgoParser.parseToTimestamp(data.requireString("/videoRenderer/publishedTimeText")),
//            duration = parseDurationString(data.requireString("/videoRenderer/lengthText/simpleText")),
//            viewCount = data.requireString("/videoRenderer/viewCountText/simpleText").extractDigitsAsLong(),
//            shortFormContent = false, //todo
//            thumbnailUrl = data.requireArray("/videoRenderer/thumbnail/thumbnails").last().requireString("url"),
//            isPaid = data.requireString("/videoRenderer/badges/0/metadataBadgeRenderer/style") == "BADGE_STYLE_TYPE_MEMBERS_ONLY",
//        )
//    }

    fun parseFromPlaylistVideoRenderer(data: JsonNode): StreamInfo {
        return StreamInfo(
            url = STREAM_URL + data.requireString("/playlistVideoRenderer/videoId"),
            serviceId = "YOUTUBE",
            streamType = StreamType.VIDEO_STREAM,
            duration = runCatching{ parseDurationString(data.requireString("/playlistVideoRenderer/lengthText/simpleText")) }.getOrNull(),
            name = data.requireString("/playlistVideoRenderer/title/runs/0/text"),
            uploaderName = runCatching { data.requireString("/playlistVideoRenderer/shortBylineText/runs/0/text") }.getOrNull(),
            uploaderUrl = runCatching{ CHANNEL_URL + data.requireString("/playlistVideoRenderer/shortBylineText/runs/0/navigationEndpoint/browseEndpoint/browseId") }.getOrNull(),
            uploaderAvatarUrl = runCatching{
                data.requireArray("/playlistVideoRenderer/thumbnail/thumbnails").last().requireString("url")
            }.getOrNull(),
            thumbnailUrl = data.requireArray("/playlistVideoRenderer/thumbnail/thumbnails").last().requireString("url"),
            isPaid = runCatching{ data.requireString("/playlistVideoRenderer/badges/0/metadataBadgeRenderer/style") == "BADGE_STYLE_TYPE_MEMBERS_ONLY" }.getOrDefault(false),
        )
    }

    fun parseFromLockupViewModel(data: JsonNode, overrideChannelName: String? = null, overrideChannelId: String? = null): StreamInfo {
        val useOverride = overrideChannelName != null && overrideChannelId != null
        val metadataRows = data.requireArray("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows")
        val hasMultipleRows = metadataRows.size() > 1 // member-only?
        val metadataRowIndex = if (useOverride && hasMultipleRows) 1 else if (useOverride) 0 else 1

        val isLive = if (useOverride && !hasMultipleRows) {
            false
        } else {
            runCatching {
                data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows/$metadataRowIndex/metadataParts/0/text/content").contains("watching")
            }.getOrDefault(false)
        }

        val viewCount = if (useOverride && !hasMultipleRows) {
            null
        } else {
            runCatching {
                data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows/$metadataRowIndex/metadataParts/0/text/content").extractDigitsAsLong()
            }.getOrNull()
        }

        return StreamInfo(
            url = STREAM_URL + data.requireString("/lockupViewModel/contentId"),
            serviceId = "YOUTUBE",
            name = data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/title/content"),
            uploaderName = overrideChannelName ?: data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows/0/metadataParts/0/text/content"),
            uploaderUrl = if (useOverride) {
                CHANNEL_URL + overrideChannelId
            } else {
                runCatching { CHANNEL_URL + data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/image/decoratedAvatarViewModel/rendererContext/commandContext/onTap/innertubeCommand/browseEndpoint/browseId") }.getOrNull()
            },
            uploaderAvatarUrl = if (useOverride) {
                null
            } else {
                data.requireArray("/lockupViewModel/metadata/lockupMetadataViewModel/image/decoratedAvatarViewModel/avatar/avatarViewModel/image/sources").last().requireString("url")
            },
            thumbnailUrl = data.requireArray("/lockupViewModel/contentImage/thumbnailViewModel/image/sources").last().requireString("url"),
            viewCount = viewCount
        ).apply {
            if (isLive) {
                streamType = StreamType.LIVE_STREAM
            } else {
                streamType = StreamType.VIDEO_STREAM
                duration = extractDuration(data)

                // 当 override 为 true 且只有一行时，uploadDate 使用 metadataParts/0
                uploadDate = if (useOverride && !hasMultipleRows) {
                    TimeAgoParser.parseToTimestamp(data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows/0/metadataParts/0/text/content"))
                } else {
                    TimeAgoParser.parseToTimestamp(data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows/$metadataRowIndex/metadataParts/1/text/content"))
                }
            }
        }
    }

    private fun extractDuration(data: JsonNode): Long? {
        val paths = listOf(
            "/lockupViewModel/contentImage/thumbnailViewModel/overlays/0/thumbnailOverlayBadgeViewModel/thumbnailBadges/0/thumbnailBadgeViewModel/text",
            "/lockupViewModel/contentImage/thumbnailViewModel/overlays/0/thumbnailBottomOverlayViewModel/badges/0/thumbnailBadgeViewModel/text"
        )

        return paths.firstNotNullOfOrNull { path ->
            runCatching { parseDurationString(data.requireString(path)) }.getOrNull()
        }
    }


    private fun parseDurationString(input: String): Long {
        val parts = input.split(if (":" in input) ":" else ".")
        val units = listOf(24, 60, 60, 1)

        require(parts.size <= units.size) {
            "Error duration string with unknown format: $input"
        }

        val offset = units.size - parts.size

        return parts.foldIndexed(0) { index, duration, part ->
            units[index + offset] * (duration + part.extractDigitsAsLong())
        }
    }
}