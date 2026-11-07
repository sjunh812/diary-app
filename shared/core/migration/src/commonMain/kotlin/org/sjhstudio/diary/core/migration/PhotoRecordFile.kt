package org.sjhstudio.diary.core.migration

import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem

/** 옮긴 사진 목록(`photos.json`). 다시 시작할 때 이미 옮긴 사진을 건너뛴다. */
internal class PhotoRecordFile(private val fileSystem: FileSystem, private val paths: MigrationPaths) {
    fun read(): List<PhotoRecord> = if (fileSystem.exists(paths.photosJson)) {
        LegacySnapshotter.JSON.decodeFromString(
            ListSerializer(PhotoRecord.serializer()),
            fileSystem.read(paths.photosJson) { readUtf8() }
        )
    } else {
        emptyList()
    }

    fun write(records: List<PhotoRecord>) {
        fileSystem.createDirectories(paths.snapshotDir)
        val temp = paths.snapshotDir / "photos.json.tmp"
        fileSystem.write(temp) { writeUtf8(LegacySnapshotter.JSON.encodeToString(ListSerializer(PhotoRecord.serializer()), records)) }
        fileSystem.atomicMove(temp, paths.photosJson)
    }
}
