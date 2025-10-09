package project.pipepipe.extractor.services.youtube.dataparser

import com.fasterxml.jackson.databind.JsonNode
import project.pipepipe.extractor.services.youtube.YouTubeLinks.BASE_URL
import project.pipepipe.extractor.utils.extractDigitsAsLong
import project.pipepipe.shared.infoitem.PlaylistInfo
import project.pipepipe.shared.utils.json.requireArray
import project.pipepipe.shared.utils.json.requireString

object YouTubePlaylistInfoDataParser {
    fun parseFromLockupMetadataViewModel(data: JsonNode, overrideName: String? = null): PlaylistInfo {
        val url = BASE_URL + data.requireArray("/lockupViewModel/metadata/lockupMetadataViewModel/metadata/contentMetadataViewModel/metadataRows")
            .firstNotNullOf {
                runCatching {
                    it.requireString("/metadataParts/0/text/commandRuns/0/onTap/innertubeCommand/commandMetadata/webCommandMetadata/url")
                }.getOrNull()
            }
        return PlaylistInfo(
            thumbnailUrl = data.requireArray("/lockupViewModel/contentImage/collectionThumbnailViewModel/primaryThumbnail/thumbnailViewModel/image/sources")
                .last().requireString("url"),
            streamCount = data.requireString("/lockupViewModel/contentImage/collectionThumbnailViewModel/primaryThumbnail/thumbnailViewModel/overlays/0/thumbnailOverlayBadgeViewModel/thumbnailBadges/0/thumbnailBadgeViewModel/text")
                .extractDigitsAsLong(),
            url = url,
            serviceId = "YOUTUBE",
            name = data.requireString("/lockupViewModel/metadata/lockupMetadataViewModel/title/content"),
            uploaderName = overrideName,
        )
    }
}