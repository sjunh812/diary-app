package org.sjhstudio.diary.core.testing

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.sjhstudio.diary.core.datastore.BackupStore

/**
 * [FakeDriveAuthorizer] 기본 계정이 연결된 백업 설정
 * 백업은 연결해 둔 계정의 기록만 적어 연결 없이 돌리면 성공이 남지 않음
 * 키 이름은 `BackupStore` 저장 키와 같음
 */
fun connectedBackupStore(): BackupStore = BackupStore(InMemoryDataStore(preferencesOf(stringPreferencesKey("account_key") to "sub-1")))
