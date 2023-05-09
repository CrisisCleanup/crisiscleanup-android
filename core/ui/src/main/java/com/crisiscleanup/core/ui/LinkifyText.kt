package com.crisiscleanup.core.ui

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import java.util.regex.Pattern

@Composable
fun LinkifyText(
    text: CharSequence,
    // TODO Apply style to legacy TextView
    style: TextStyle = LocalTextStyle.current,
    linkify: (TextView) -> Unit = {},
) {
    val context = LocalContext.current
    val linkifyTextView = remember { TextView(context) }
    AndroidView(
        factory = { linkifyTextView },
        update = { textView ->
            textView.text = text
            linkify(textView)
            textView.movementMethod = LinkMovementMethod.getInstance()
        },
    )
}

@Composable
fun LinkifyHtmlText(
    text: String,
) {
    val htmlText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
    LinkifyText(htmlText)
}

@Composable
fun LinkifyPhoneText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
) = LinkifyText(text, style) { textView -> Linkify.addLinks(textView, Linkify.PHONE_NUMBERS) }

@Composable
fun LinkifyEmailText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
) = LinkifyText(text, style) { textView -> Linkify.addLinks(textView, Linkify.EMAIL_ADDRESSES) }

private val allPattern = Pattern.compile(".*", Pattern.DOTALL)

@Composable
fun LinkifyLocationText(
    text: String,
    locationQuery: String,
    style: TextStyle = LocalTextStyle.current,
) = LinkifyText(text, style) { textView -> Linkify.addLinks(textView, allPattern, locationQuery) }
