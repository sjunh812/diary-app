package org.sjhstudio.diary.feature.write

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.platform.FontInstaller

val writeFeatureModule =
    module {
        viewModel { (target: WriteTarget, restoredKey: RestoredDraftKey) ->
            WriteViewModel(
                target,
                get(),
                mediaPath = get<WriteMediaPath>()::resolve,
                settings = get(),
                fonts = get<FontInstaller>(),
                plans = get(),
                places = get(),
                restoredKey = restoredKey.value
            )
        }
    }

/** 화면이 기억해 둔 임시 저장본 키. 비어 있을 수 있어 값 클래스 대신 감싸서 넘김 */
class RestoredDraftKey(val value: String?)

/** 사진 파일 이름을 앱 저장소 경로로. 파일 자리를 아는 앱 틀이 넘김 */
fun interface WriteMediaPath {
    fun resolve(fileName: String): String
}
