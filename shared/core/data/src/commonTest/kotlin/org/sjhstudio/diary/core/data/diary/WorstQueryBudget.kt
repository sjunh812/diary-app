package org.sjhstudio.diary.core.data.diary

/**
 * 1만 편이 모두 걸리는 찾기에 주는 시간(밀리초).
 * 플랫폼마다 시험 빌드의 속도가 크게 달라서 따로 둔다.
 */
internal expect val WORST_QUERY_BUDGET_MILLIS: Long
