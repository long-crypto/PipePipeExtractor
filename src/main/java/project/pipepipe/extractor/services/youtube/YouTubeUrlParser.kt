package project.pipepipe.extractor.services.youtube

import project.pipepipe.extractor.services.youtube.dataparser.YouTubeStreamIdParser
import java.net.URI
import java.util.Locale

object YouTubeUrlParser {
    val GOOGLE_URLS = setOf("google.", "m.google.", "www.google.")
    val INVIDIOUS_URLS = setOf(
        "invidio.us", "dev.invidio.us", "www.invidio.us", "redirect.invidious.io",
        "invidious.snopyta.org", "yewtu.be", "tube.connect.cafe", "tubus.eduvid.org",
        "invidious.kavin.rocks", "invidious.site", "invidious-us.kavin.rocks",
        "piped.kavin.rocks", "vid.mint.lgbt", "invidiou.site", "invidious.fdn.fr",
        "invidious.048596.xyz", "invidious.zee.li", "vid.puffyan.us", "ytprivate.com",
        "invidious.namazso.eu", "invidious.silkky.cloud", "ytb.trom.tf", "invidious.exonip.de",
        "inv.riverside.rocks", "invidious.blamefran.net", "y.com.cm", "invidious.moomoo.me",
        "yt.cyberhost.uk"
    )
    val YOUTUBE_URLS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")


    fun isYoutubeURL(url: URI): Boolean {
        return YOUTUBE_URLS.contains(url.host.lowercase(Locale.ROOT))
    }

    fun isYoutubeServiceURL(url: URI): Boolean {
        val host = url.host
        return host.equals("www.youtube-nocookie.com", ignoreCase = true) ||
                host.equals("youtu.be", ignoreCase = true)
    }

    fun isHooktubeURL(url: URI): Boolean {
        return url.host.equals("hooktube.com", ignoreCase = true)
    }

    fun isInvidiousURL(url: URI): Boolean {
        return INVIDIOUS_URLS.contains(url.host.lowercase(Locale.ROOT))
    }

    fun isY2ubeURL(url: URI): Boolean {
        return url.host.equals("y2u.be", ignoreCase = true)
    }

    fun isHTTP(url: URI): Boolean {
        val protocol = url.scheme.lowercase(Locale.ROOT)
        return protocol == "http" || protocol == "https"
    }

    fun parseStreamId(url: String): String? {
        return YouTubeStreamIdParser.getId(url)
    }
}