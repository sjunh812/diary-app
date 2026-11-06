package org.sjhstudio.diary.feature.write

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val writeFeatureModule =
    module {
        viewModel { (target: WriteTarget) ->
            WriteViewModel(
                target,
                get(),
                mediaPath = get<WriteMediaPath>()::resolve,
                settings = get(),
                fonts = get(),
                plans = get(),
                places = get()
            )
        }
    }

/**
 * 사진 파일 이름을 앱 저장소 경로로 바꾼다.
 * 앱 틀이 파일 자리를 알아 넘긴다.
 */
fun interface WriteMediaPath {
    fun resolve(fileName: String): String
}
