package org.sjhstudio.diary.core.model

/**
 * 옛 앱이 로그인한 구글 계정과 드라이브에 올린 `note.db` 파일 ID.
 * 옛 `pref.xml`의 이메일 모양 키와 `"<계정>'s id"` 값에서 읽는다(data-model.md LegacyBackupLink).
 */
data class LegacyBackupLink(val account: String, val fileId: String?)
