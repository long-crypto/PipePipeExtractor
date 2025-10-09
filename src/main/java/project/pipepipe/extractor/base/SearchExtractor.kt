package project.pipepipe.extractor.base

import project.pipepipe.extractor.Extractor
import project.pipepipe.shared.infoitem.Info

open class SearchExtractor(url: String): Extractor<Info, Info>(url)