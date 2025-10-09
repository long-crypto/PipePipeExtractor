package project.pipepipe.extractor.services.niconico

object NicoNicoLinks {
    const val WATCH_URL = "https://www.nicovideo.jp/watch/"
    const val USER_URL = "https://www.nicovideo.jp/user/"
    const val CHANNEL_URL = "https://ch.nicovideo.jp/"
    const val SUGGESTION_URL = "https://sug.search.nicovideo.jp/suggestion/expand/"
    const val SEARCH_URL = "https://www.nicovideo.jp/search/"
    const val PLAYLIST_SEARCH_API_URL = "https://nvapi.nicovideo.jp/v1/search/list?_frontendId=6&_frontendVersion=0"
    fun getAccessUrl(id: String, trackId: String) = "https://nvapi.nicovideo.jp/v1/watch/${id}/access-rights/hls?actionTrackId=$trackId"
}