package project.pipepipe.extractor.base

import project.pipepipe.shared.state.State
import project.pipepipe.shared.infoitem.CookieInfo
import project.pipepipe.shared.job.ExtractResult
import project.pipepipe.shared.job.JobStepResult
import project.pipepipe.shared.job.TaskResult

open class CookieExtractor {
    open suspend fun refreshCookie(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?
    ): JobStepResult = JobStepResult.CompleteWith(ExtractResult(CookieInfo(null, 0)))
}