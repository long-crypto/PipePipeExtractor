package project.pipepipe.extractor.services.youtube.dataparser

import project.pipepipe.extractor.services.youtube.YouTubeUrlParser
import project.pipepipe.extractor.utils.RequestHelper.getQueryValue
import project.pipepipe.extractor.utils.RequestHelper.stringToURI
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import java.util.regex.Pattern

object YouTubeStreamIdParser {
    val YOUTUBE_VIDEO_ID_REGEX_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]{11})")
    val SUBPATHS = listOf("embed/", "live/", "shorts/", "watch/", "v/", "w/")

    fun extractId(id: String?): String? {
        if (id != null) {
            val matcher = YOUTUBE_VIDEO_ID_REGEX_PATTERN.matcher(id)
            return if (matcher.find()) matcher.group(1) else null
        }
        return null
    }

    fun getId(theUrlString: String): String? {
        var urlString = theUrlString

        try {
            val uri = URI(urlString)
            val scheme = uri.scheme

            if (scheme != null && (scheme == "vnd.youtube" || scheme == "vnd.youtube.launch")) {
                val schemeSpecificPart = uri.schemeSpecificPart
                if (schemeSpecificPart.startsWith("//")) {
                    val extractedId = extractId(schemeSpecificPart.substring(2))
                    if (extractedId != null) {
                        return extractedId
                    }
                    urlString = "https:$schemeSpecificPart"
                } else {
                    return extractId(schemeSpecificPart)
                }
            }
        } catch (ignored: URISyntaxException) {
            // Continue with URL parsing
        }

        val url = stringToURI(urlString) ?: return null

        val host = url.host
        var path = url.path
        // remove leading "/" of URL-path if URL-path is given
        if (path.isNotEmpty()) {
            path = path.substring(1)
        }

        if (!YouTubeUrlParser.isHTTP(url) || !(YouTubeUrlParser.isYoutubeURL(url) || YouTubeUrlParser.isYoutubeServiceURL(
                url
            ) ||
                    YouTubeUrlParser.isHooktubeURL(url) || YouTubeUrlParser.isInvidiousURL(url) || YouTubeUrlParser.isY2ubeURL(
                url
            ))
        ) {
            return null
        }

        // Check if it's a playlist URL (would need YoutubePlaylistLinkHandlerFactory implementation)
        // For now, skip this check

        return when (host.uppercase(Locale.ROOT)) {
            "WWW.YOUTUBE-NOCOOKIE.COM" -> {
                if (path.startsWith("embed/")) {
                    extractId(path.substring(6))
                } else null
            }

            "YOUTUBE.COM", "WWW.YOUTUBE.COM", "M.YOUTUBE.COM", "MUSIC.YOUTUBE.COM" -> {
                when {
                    path == "attribution_link" -> {
                        val uQueryValue = getQueryValue(url, "u")
                        if (uQueryValue != null) {
                            val decodedURL = stringToURI("https://www.youtube.com$uQueryValue")
                            if (decodedURL != null) {
                                val viewQueryValue = getQueryValue(decodedURL, "v")
                                extractId(viewQueryValue)
                            } else null
                        } else null
                    }

                    else -> {
                        getIdFromSubpathsInPath(path) ?: run {
                            val viewQueryValue = getQueryValue(url, "v")
                            extractId(viewQueryValue)
                        }
                    }
                }
            }

            "Y2U.BE", "YOUTU.BE" -> {
                val viewQueryValue = getQueryValue(url, "v")
                if (viewQueryValue != null) {
                    extractId(viewQueryValue)
                } else {
                    extractId(path)
                }
            }

            "HOOKTUBE.COM", "INVIDIO.US", "DEV.INVIDIO.US", "WWW.INVIDIO.US",
            "REDIRECT.INVIDIOUS.IO", "INVIDIOUS.SNOPYTA.ORG", "YEWTU.BE",
            "TUBE.CONNECT.CAFE", "TUBUS.EDUVID.ORG", "INVIDIOUS.KAVIN.ROCKS",
            "INVIDIOUS-US.KAVIN.ROCKS", "PIPED.KAVIN.ROCKS", "PIPED.VIDEO",
            "INVIDIOUS.SITE", "VID.MINT.LGBT", "INVIDIOU.SITE", "INVIDIOUS.FDN.FR",
            "INVIDIOUS.048596.XYZ", "INVIDIOUS.ZEE.LI", "VID.PUFFYAN.US",
            "YTPRIVATE.COM", "INVIDIOUS.NAMAZSO.EU", "INVIDIOUS.SILKKY.CLOUD",
            "INVIDIOUS.EXONIP.DE", "INV.RIVERSIDE.ROCKS", "INVIDIOUS.BLAMEFRAN.NET",
            "INVIDIOUS.MOOMOO.ME", "YTB.TROM.TF", "YT.CYBERHOST.UK", "Y.COM.CM" -> {
                when {
                    path == "watch" -> {
                        val viewQueryValue = getQueryValue(url, "v")
                        if (viewQueryValue != null) {
                            extractId(viewQueryValue)
                        } else null
                    }

                    else -> {
                        getIdFromSubpathsInPath(path) ?: run {
                            val viewQueryValue = getQueryValue(url, "v")
                            if (viewQueryValue != null) {
                                extractId(viewQueryValue)
                            } else {
                                extractId(path)
                            }
                        }
                    }
                }
            }

            else -> null
        }
    }

    private fun getIdFromSubpathsInPath(path: String): String? {
        for (subpath in SUBPATHS) {
            if (path.startsWith(subpath)) {
                val id = path.substring(subpath.length)
                return extractId(id)
            }
        }
        return null
    }
}