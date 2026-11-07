package org.sjhstudio.diary.core.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

internal actual fun fontFamilyFromFile(path: String): FontFamily = FontFamily(Font(File(path)))
