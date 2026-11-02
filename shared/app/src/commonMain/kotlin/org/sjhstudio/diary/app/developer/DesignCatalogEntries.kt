package org.sjhstudio.diary.app.developer

import org.sjhstudio.diary.app.navigation.DesignCatalogKey
import org.sjhstudio.diary.core.designsystem.catalog.DesignCatalogScreen

/**
 * 디자인 시스템 견본 화면(FR-029). 개발 빌드의 등록 코드만 부른다.
 * 이 파일에 다른 함수를 두지 않는다. 출시 빌드에서 이 함수와 견본 화면이 같이 빠지게 하려고다.
 */
fun designCatalogEntries(): DeveloperEntries = DeveloperEntries { pop -> entry<DesignCatalogKey> { DesignCatalogScreen(onBack = pop) } }
