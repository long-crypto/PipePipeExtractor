package project.pipepipe.extractor.utils

import java.net.URI

object RequestHelper {
    fun getQueryValue(url: String, key: String): String? {
        return getQueryValue(stringToURI(url)!!, key)
    }

    fun getQueryValue(url: URI, key: String): String? {
        val query = url.query ?: return null
        val pairs = query.split("&")
        for (pair in pairs) {
            val keyValue = pair.split("=", limit = 2)
            if (keyValue.size == 2 && keyValue[0] == key) {
                return keyValue[1]
            }
        }
        return null
    }

    fun replaceQueryValue(url: String, key: String, value: String): String? {
        val uri = stringToURI(url) ?: return null

        val query = uri.query
        val newQuery = if (query.isNullOrEmpty()) {
            "$key=$value"
        } else {
            val pairs = query.split("&").toMutableList()
            var found = false
            for (i in pairs.indices) {
                val keyValue = pairs[i].split("=", limit = 2)
                if (keyValue.isNotEmpty() && keyValue[0] == key) {
                    pairs[i] = "$key=$value"
                    found = true
                    break
                }
            }

            if (!found) {
                pairs.add("$key=$value")
            }

            pairs.joinToString("&")
        }

        return URI(
            uri.scheme,
            uri.userInfo,
            uri.host,
            uri.port,
            uri.path,
            newQuery,
            uri.fragment
        ).toString()
    }


    fun stringToURI(urlString: String): URI? {
        return runCatching { URI(urlString) }.getOrNull()
    }
}


