package org.sjhstudio.diary.core.ui

/** 세 자리마다 쉼표(1,284). */
fun Int.grouped(): String = toLong().grouped()

fun Long.grouped(): String {
    val digits = kotlin.math.abs(this).toString()
    val out = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) out.append(',')
        out.append(c)
    }
    return if (this < 0) "-$out" else out.toString()
}

/** 사람이 읽는 크기(850MB, 1.2GB). 1GB부터 소수 한 자리, 아래는 MB 정수(올림). */
fun formatBytes(bytes: Long): String {
    val mb = 1024.0 * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> {
            val tenths = kotlin.math.ceil(bytes / gb * 10).toLong()
            "${tenths / 10}.${tenths % 10}GB"
        }

        else -> "${kotlin.math.ceil(bytes / mb).toLong().coerceAtLeast(1)}MB"
    }
}
