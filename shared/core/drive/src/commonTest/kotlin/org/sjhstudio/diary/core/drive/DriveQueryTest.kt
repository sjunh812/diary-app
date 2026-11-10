package org.sjhstudio.diary.core.drive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 찾기 조건은 드라이브 질의문. 기기 이름이 파일 이름에 들어가 따옴표가 그대로 오면 질의가 깨짐
class DriveQueryTest {
    @Test
    fun trashed_files_are_always_left_out() {
        assertEquals("trashed = false", DriveQuery().toQ())
    }

    @Test
    fun an_apostrophe_in_a_device_name_stays_inside_the_quotes() {
        val query = DriveQuery(name = "2026-11-07_민수's-폰_ab12.json").toQ()

        assertEquals("trashed = false and name = '2026-11-07_민수\\'s-폰_ab12.json'", query)
    }

    @Test
    fun a_backslash_cannot_undo_the_closing_quote() {
        assertTrue("name = 'a\\\\'" in DriveQuery(name = "a\\").toQ())
    }

    @Test
    fun parent_and_app_properties_are_quoted_too() {
        val query = DriveQuery(parentId = "folder'1", props = mapOf("diary.kind" to "photo'")).toQ()

        assertTrue("'folder\\'1' in parents" in query, query)
        assertTrue("appProperties has { key='diary.kind' and value='photo\\'' }" in query, query)
    }
}
