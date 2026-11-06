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

enum class ByteUnit { KB, MB, GB }

/**
 * 화면에 보일 크기.
 * 단위 글자는 언어마다 띄어쓰기가 달라서 숫자와 나눠 둔다.
 */
data class ByteSize(val value: String, val unit: ByteUnit)

private const val KILO = 1_000.0
private const val MEGA = KILO * KILO
private const val GIGA = MEGA * KILO
private const val ONE_DECIMAL_BELOW = 10.0

/**
 * 사람이 읽는 크기.
 * Android·iOS 파일 앱과 같게 1,000 단위로 재서 파일 앱에 보이는 숫자와 맞춘다.
 * 공간이 부족하다고 알릴 때 적게 말하지 않도록 마지막 자리를 올린다.
 * 637KB, 1.2MB, 850MB, 1.2GB처럼 쓴다.
 */
fun byteSize(bytes: Long): ByteSize {
    val kilo = kotlin.math.ceil(bytes / KILO).toLong()
    if (kilo < KILO) return ByteSize(kilo.toString(), ByteUnit.KB)
    val mega = bytes / MEGA
    if (mega < ONE_DECIMAL_BELOW) return ByteSize(tenths(mega), ByteUnit.MB)
    val wholeMega = kotlin.math.ceil(mega).toLong()
    if (wholeMega < KILO) return ByteSize(wholeMega.toString(), ByteUnit.MB)
    return ByteSize(tenths(bytes / GIGA), ByteUnit.GB)
}

/**
 * 소수 한 자리로 올린다.
 * 1.0은 1로 쓴다.
 */
private fun tenths(value: Double): String {
    val tenths = kotlin.math.ceil(value * 10).toLong()
    return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

/** 단위를 붙인 크기(850MB, 1.2GB). */
fun formatBytes(bytes: Long): String = byteSize(bytes).let { "${it.value}${it.unit}" }
