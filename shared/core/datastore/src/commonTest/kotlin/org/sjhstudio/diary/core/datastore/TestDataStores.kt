package org.sjhstudio.diary.core.datastore

import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.FileSystem
import okio.Path

/**
 * 시험마다 새 임시 폴더에 DataStore 파일을 만든다.
 * 한 파일에 DataStore가 둘 생기지 않게 한다.
 */
class TestDataStores : AutoCloseable {
    val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "diary-datastore-${Random.nextLong().toULong()}"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val factory = DataStoreFactory(directory.toString(), scope)

    init {
        FileSystem.SYSTEM.createDirectories(directory)
    }

    override fun close() {
        scope.cancel()
        FileSystem.SYSTEM.deleteRecursively(directory)
    }
}
