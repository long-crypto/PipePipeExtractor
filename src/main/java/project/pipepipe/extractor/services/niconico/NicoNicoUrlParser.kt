package project.pipepipe.extractor.services.niconico

object NicoNicoUrlParser {
    fun parseStreamId(url: String): String? {
        return when {
            url.contains("nicovideo.jp/watch/") ->
                url.substringAfter("nicovideo.jp/watch/").substringBefore("?")
            url.contains("nico.ms/") ->
                url.substringAfter("nico.ms/").substringBefore("?")
            else -> null
        }
    }
}