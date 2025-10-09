package project.pipepipe.extractor.localization

import java.io.Serializable

data class ContentCountry(val countryCode: String) : Serializable {
    override fun toString(): String = countryCode

    companion object {
        val DEFAULT = ContentCountry(Localization.DEFAULT.countryCode!!)

        fun listOf(vararg countryCodes: String): List<ContentCountry> =
            countryCodes.map { ContentCountry(it) }.toList()
    }
}
