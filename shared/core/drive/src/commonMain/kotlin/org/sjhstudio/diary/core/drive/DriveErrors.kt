package org.sjhstudio.diary.core.drive

import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 드라이브 호출 실패. 메시지에 드라이브 응답 본문, 파일 이름, 경로를 넣지 않는다(FR-032).
 * [retryable]이면 잠시 뒤 다시 해 볼 만하다.
 */
open class DriveException(val kind: BackupFailure, val retryable: Boolean = false, val status: Int = 0) :
    Exception("drive failure $kind $status")

/** 파일이나 폴더가 없어졌다. */
class DriveNotFound : DriveException(BackupFailure.DRIVE_ERROR, status = 404)

/** 토큰이 거절됐다. 새 토큰으로 한 번 다시 한다. */
class DriveUnauthorized : DriveException(BackupFailure.AUTH, status = 401)

/** 응답 코드와 드라이브 오류 까닭을 실패 종류로 나눈다(contracts/drive-gateway.md "오류 나누기"). */
object DriveErrorMapper {
    fun map(status: Int, reason: String?): DriveException = when {
        status == 401 -> DriveUnauthorized()
        status == 404 -> DriveNotFound()
        status == 403 && reason == "storageQuotaExceeded" -> DriveException(BackupFailure.NO_SPACE, status = status)
        status == 403 && reason in RATE_LIMITS -> DriveException(BackupFailure.DRIVE_ERROR, retryable = true, status = status)
        status == 403 -> DriveException(BackupFailure.AUTH, status = status)
        status == 429 || status >= 500 -> DriveException(BackupFailure.DRIVE_ERROR, retryable = true, status = status)
        else -> DriveException(BackupFailure.DRIVE_ERROR, status = status)
    }

    private val RATE_LIMITS = setOf("userRateLimitExceeded", "rateLimitExceeded")
}
