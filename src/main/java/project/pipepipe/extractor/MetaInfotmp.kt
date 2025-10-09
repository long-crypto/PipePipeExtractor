package project.pipepipe.extractor

import project.pipepipe.shared.infoitem.helper.stream.Description
import java.io.Serializable
import java.net.URL
import javax.annotation.Nonnull

data class MetaInfotmp(
    var title: String = "",
    var content: Description = Description.Companion.EMPTY_DESCRIPTION,
    var urls: MutableList<URL> = mutableListOf(),
    var urlTexts: MutableList<String> = mutableListOf()
) : Serializable {

    fun addUrl(@Nonnull url: URL) = urls.add(url)

    fun addUrlText(@Nonnull urlText: String) = urlTexts.add(urlText)
}
