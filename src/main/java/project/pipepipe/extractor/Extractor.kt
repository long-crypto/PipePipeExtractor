package project.pipepipe.extractor

import project.pipepipe.shared.infoitem.Info
import project.pipepipe.shared.job.ExtractResult
import project.pipepipe.shared.job.JobStepResult
import project.pipepipe.shared.job.TaskResult
import project.pipepipe.shared.state.State

const val FAILED_COMMIT_THRESHOLD = 5

abstract class Extractor<META: Info, DATA: Info>(
    val url: String
) {
    val itemList: ArrayList<DATA> = arrayListOf()
    val _errors: MutableList<Throwable> = mutableListOf()
    val errors get() = _errors.map { it.stackTraceToString()}
    
    var failedCommitCount = 0
    open suspend fun fetchInfo(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult = JobStepResult.CompleteWith(ExtractResult())
    open suspend fun fetchFirstPage(
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult = JobStepResult.CompleteWith(ExtractResult())
    open suspend fun fetchGivenPage(
        url: String,
        sessionId: String,
        currentState: State?,
        clientResults: List<TaskResult>?,
        cookie: String?
    ): JobStepResult = JobStepResult.CompleteWith(ExtractResult())

    //TODO: getAllPages

    fun addError(throwable: Throwable) {
        _errors.add(throwable)
    }

    fun addAllErrors(throwables: Collection<Throwable>) {
        _errors.addAll(throwables)
    }

    // used to set **single** property of a metaInfo
    fun <T: Any> safeGet(itemProvider: () -> T): T? {
        return try {
            itemProvider()
        } catch (e: Exception) {
            addError(e)
            null
        }
    }

    inline fun <reified T : DATA> commit(itemProvider: () -> T) {
        try {
            val item = itemProvider()
            itemList.add(item)
        } catch (e: Exception) {
            failedCommitCount ++
            _errors.add(e)
            e.printStackTrace()
            println(url)
            if (failedCommitCount > FAILED_COMMIT_THRESHOLD)  {
                val recentErrors = _errors.takeLast(5).joinToString("\n\n") { err ->
                    val trace = err.stackTrace.take(2).joinToString("\n  ") {
                        "at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})"
                    }
                    "${err.message}\n  $trace"
                }
                error("Too many failed commits ($failedCommitCount). Recent errors:\n$recentErrors")
            }
        }
    }
}