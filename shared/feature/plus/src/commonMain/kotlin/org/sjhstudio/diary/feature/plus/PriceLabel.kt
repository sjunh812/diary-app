package org.sjhstudio.diary.feature.plus

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.price_krw

/** 원화는 "14,900원" 꼴(한국어 화면), 그 밖의 통화는 스토어가 준 글자 그대로 */
@Composable
internal fun Price.label(): String =
    if (currencyCode == KRW) stringResource(Res.string.price_krw, groupThousands(amountMicros / MICROS)) else formatted

/** 원화는 소수점이 없어 세 자리마다 쉼표만 찍음 */
internal fun groupThousands(value: Long): String = value.toString().reversed().chunked(DIGITS).joinToString(",").reversed()

private const val KRW = "KRW"
private const val MICROS = 1_000_000L
private const val DIGITS = 3
