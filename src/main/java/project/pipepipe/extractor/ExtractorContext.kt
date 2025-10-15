package project.pipepipe.extractor

//import project.pipepipe.extractor.services.bandcamp.BandcampService
//import project.pipepipe.extractor.services.media_ccc.MediaCCCService
//import project.pipepipe.extractor.services.niconico.NiconicoService
//import project.pipepipe.extractor.services.peertube.PeertubeService
//import project.pipepipe.extractor.services.soundcloud.SoundcloudService
//import project.pipepipe.extractor.services.youtube.YoutubeService

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.lettuce.core.RedisClient
import project.pipepipe.shared.downloader.Downloader
import project.pipepipe.extractor.localization.ContentCountry
import project.pipepipe.extractor.localization.Localization
import project.pipepipe.extractor.services.bilibili.BilibiliService
import project.pipepipe.extractor.services.niconico.NicoNicoService
import project.pipepipe.extractor.services.youtube.YouTubeService

object ExtractorContext {
    val downloader: Downloader = Downloader(HttpClient(CIO) {
        expectSuccess = false
        followRedirects = true
    })
    val objectMapper = ObjectMapper()
    fun String.asJson(): JsonNode = objectMapper.readTree(this)

    var preferredLocalization: Localization = Localization.Companion.DEFAULT
    var preferredContentCountry: ContentCountry = ContentCountry.Companion.DEFAULT
    var audioLanguage: String = "original"

    var showAutoTranslatedSubtitles: Boolean = false
//
//    var filterTypes: MutableSet<String> = HashSet()
//    var filterConfig: MetaInfoWithPagedData.FilterConfig? = null


    var proxyToken: String? = null
    var proxyEnabled: Boolean = false

    var loadingTimeout: Int = 5
    var fetchFullPlaylist: Boolean = false

    object ServiceList {
        val YouTube = YouTubeService("YOUTUBE")
//        val SoundCloud = SoundcloudService(1)
//        val MediaCCC = MediaCCCService(2)
//        val PeerTube = PeertubeService(3)
//        val Bandcamp = BandcampService(4)
        val BiliBili = BilibiliService("BILIBILI")
        val NicoNico = NicoNicoService("NICONICO")
//        val NicoNico = (6)

        val all = listOf(BiliBili, YouTube, NicoNico)
    }
}
