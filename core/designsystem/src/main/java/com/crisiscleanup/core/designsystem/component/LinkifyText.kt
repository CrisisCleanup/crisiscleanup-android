package com.crisiscleanup.core.designsystem.component

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import com.crisiscleanup.core.designsystem.R
import java.util.regex.Pattern

@Composable
fun LinkifyText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
    linkify: (TextView) -> Unit = {},
) {
    val context = LocalContext.current
    val linkifyTextView = remember {
        val typeface = ResourcesCompat.getFont(context, R.font.nunito_sans_family)
        TextView(context, null, 0, textStyleRes)
            .apply { setTypeface(typeface, Typeface.NORMAL) }
    }
    AndroidView(
        factory = { linkifyTextView },
        modifier = modifier,
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
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
) {
    val htmlText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
    LinkifyText(htmlText, modifier, textStyleRes)
}

@Composable
fun LinkifyPhoneText(
    text: String,
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
) = LinkifyText(text, modifier, textStyleRes) { textView ->
    Linkify.addLinks(
        textView,
        Linkify.PHONE_NUMBERS,
    )
}

@Composable
fun LinkifyEmailText(
    text: String,
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
) = LinkifyText(text, modifier, textStyleRes) { textView ->
    Linkify.addLinks(
        textView,
        Linkify.EMAIL_ADDRESSES,
    )
}

private val allPattern = Pattern.compile(".*", Pattern.DOTALL)

@Composable
fun LinkifyLocationText(
    text: String,
    locationQuery: String,
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
) = LinkifyText(text, modifier, textStyleRes) { textView ->
    Linkify.addLinks(
        textView,
        allPattern,
        locationQuery,
    )
}

@Composable
fun LinkifyPhoneEmailText(
    text: String,
    modifier: Modifier = Modifier,
    @StyleRes textStyleRes: Int = R.style.link_text_style,
) = LinkifyText(text, modifier, textStyleRes) { textView ->
    Linkify.addLinks(
        textView,
        Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS,
    )
}