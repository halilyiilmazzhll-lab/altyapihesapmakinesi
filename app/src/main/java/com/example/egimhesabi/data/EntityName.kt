package com.example.egimhesabi.data

import java.text.Normalizer
import java.util.Locale

private val repeatedWhitespace = Regex("\\s+")
private val turkishLocale = Locale.forLanguageTag("tr-TR")

fun normalizeEntityName(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .trim()
    .replace(repeatedWhitespace, " ")
    .lowercase(turkishLocale)
