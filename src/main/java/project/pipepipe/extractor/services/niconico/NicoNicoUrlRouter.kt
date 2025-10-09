package project.pipepipe.extractor.services.niconico

import project.pipepipe.extractor.Extractor
import project.pipepipe.extractor.services.niconico.NicoNicoLinks.SEARCH_URL
import project.pipepipe.extractor.services.niconico.NicoNicoLinks.USER_URL
import project.pipepipe.extractor.services.niconico.NicoNicoUrlParser.parseStreamId
import project.pipepipe.extractor.services.niconico.extractor.NicoNicoChannelMainTabExtractor
import project.pipepipe.extractor.services.niconico.extractor.NicoNicoSearchExtractor
import project.pipepipe.extractor.services.niconico.extractor.NicoNicoStreamExtractor

object NicoNicoUrlRouter {
    fun route(url: String): Extractor<*,*>? {
        return when {
            parseStreamId(url) != null -> NicoNicoStreamExtractor(url)
            url.contains(SEARCH_URL) -> NicoNicoSearchExtractor(url)
            url.contains(USER_URL) -> NicoNicoChannelMainTabExtractor(url)
            else -> null
        }
    }
}