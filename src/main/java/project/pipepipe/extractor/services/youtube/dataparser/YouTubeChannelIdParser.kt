package project.pipepipe.extractor.services.youtube.dataparser

import project.pipepipe.extractor.services.youtube.YouTubeUrlParser
import project.pipepipe.shared.stringToURI
import java.util.regex.Pattern

object YouTubeChannelIdParser {

    private val EXCLUDED_SEGMENTS: Pattern = Pattern.compile(
        "playlist|watch|attribution_link|watch_popup|embed|feed|select_site|account|reporthistory|redirect"
    )

    fun parseChannelId(url: String): String? {
        val urlObj = stringToURI(url) ?: return null

        if (!YouTubeUrlParser.isHTTP(urlObj) ||
            !(YouTubeUrlParser.isYoutubeURL(urlObj) ||
                    YouTubeUrlParser.isInvidiousURL(urlObj) ||
                    YouTubeUrlParser.isHooktubeURL(urlObj))
        ) {
            return null
        }

        var path = urlObj.path.removePrefix("/")
        var splitPath = if (path.isEmpty()) emptyList() else path.split('/')

        when {
            isHandle(splitPath) -> {
                // 直接返回 handle（包含 @）
                return splitPath.first()
            }
            isCustomShortChannelUrl(splitPath) -> {
                path = "c/$path"
                splitPath = path.split('/')
            }
        }

        if (!(path.startsWith("user/") ||
                    path.startsWith("channel/") ||
                    path.startsWith("c/"))
        ) {
            return null
        }

        if (splitPath.size < 2) {
            return null
        }

        val id = splitPath[1]
        if (id.isBlank()) {
            return null
        }

        return id
    }

    private fun isCustomShortChannelUrl(segments: List<String>): Boolean {
        return segments.size == 1 &&
                segments[0].isNotEmpty() &&
                !EXCLUDED_SEGMENTS.matcher(segments[0]).matches()
    }

    private fun isHandle(segments: List<String>): Boolean {
        return segments.isNotEmpty() && segments[0].startsWith("@")
    }
}