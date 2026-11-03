package org.sjhstudio.diary.core.platform

/**
 * 일기 DB, 사진, 설정이 놓이는 자리(data-model.md "파일 자리").
 * 모두 앱 전용 저장소이고 캐시 폴더에는 아무것도 두지 않는다(헌법 I).
 * 경로는 절대경로이고 DB에는 담지 않는다.
 */
expect class AppFiles {
    /** 일기 DB 파일. */
    val databasePath: String

    /** 사진 원본 `<sha256>.<ext>`. 백업에 올린다. */
    val mediaDirectory: String

    /** 썸네일. 다시 만들 수 있어 백업에서 뺀다. */
    val thumbnailDirectory: String

    /** 고를 때 받은 일기 글꼴 `<id>.ttf`. 다시 받을 수 있어 백업에서 뺀다. */
    val fontDirectory: String

    /** 설정과 잠금 DataStore 파일. */
    val dataStoreDirectory: String

    /** 이전 상태·결과·작업 DB. 옮길 레거시가 없는 iOS는 null. */
    val migrationDirectory: String?

    /** 레거시 원본 사본. iOS는 null. */
    val legacySnapshotDirectory: String?

    /** 폴더가 없으면 만든다. 앱 시작 때 한 번 부른다. */
    fun ensureDirectories()
}
