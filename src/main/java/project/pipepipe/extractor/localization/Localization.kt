package project.pipepipe.extractor.localization

import java.io.Serializable
import java.util.*

data class Localization(
    val languageCode: String,
    val countryCode: String? = null
) : Serializable {
    companion object {
        val DEFAULT = Localization("en", "GB")

        fun listFrom(vararg localizationCodeList: String): List<Localization> =
            localizationCodeList.map { fromLocalizationCode(it) }.toList()

        fun fromLocalizationCode(localizationCode: String): Localization {
            val indexSeparator = localizationCode.indexOf("-")
            return if (indexSeparator != -1) {
                Localization(
                    languageCode = localizationCode.substring(0, indexSeparator),
                    countryCode = localizationCode.substring(indexSeparator + 1)
                )
            } else {
                Localization(languageCode = localizationCode, countryCode = null)
            }
        }

        fun fromLocale(locale: Locale): Localization =
            Localization(locale.language, locale.country)

        fun getLocaleFromThreeLetterCode(code: String): Locale {
            val localeMap = Locale.getISOLanguages().associateBy(
                { Locale(it).isO3Language },
                { Locale(it) }
            )
            return localeMap[code] ?: error("Could not get Locale from this three letter language code$code")
        }
    }


    val countryCodeOrEmpty: String get() = countryCode ?: ""
    val localizationCode: String get() = languageCode + (countryCode?.let { "-$it" } ?: "")

    override fun toString(): String = "Localization[$localizationCode]"
}
