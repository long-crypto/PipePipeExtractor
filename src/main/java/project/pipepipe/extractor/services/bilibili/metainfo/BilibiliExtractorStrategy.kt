package project.pipepipe.extractor.services.bilibili.metainfo

import com.fasterxml.jackson.databind.JsonNode
import project.pipepipe.shared.infoitem.StreamInfo


interface BilibiliExtractorStrategy {
    val streamMetaInfo: StreamInfo
    
    suspend fun fetchData()
    
    suspend fun setMetadata()
    
    suspend fun buildStreams()
    
    suspend fun processSubtitles()
    
    fun getRelatedItemsInfo()
}