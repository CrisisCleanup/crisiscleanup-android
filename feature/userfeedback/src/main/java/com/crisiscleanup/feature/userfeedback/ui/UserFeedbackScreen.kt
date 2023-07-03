package com.crisiscleanup.feature.userfeedback.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.feature.userfeedback.UserFeedbackViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFeedbackRoute(
    onBack: () -> Unit = {},
    viewModel: UserFeedbackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    Column {
        TopAppBarBackAction(
            title = LocalAppTranslator.current.translator("nav.feedback"),
            onAction = onBack,
        )

        val accountId by viewModel.accountId.collectAsStateWithLifecycle(0)
        if (accountId > 0) {
            val selectImageLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                uri?.let {
                    viewModel.onMediaSelected(uri)
                }
            }
            val onChooseFileCallback = remember(viewModel) {
                { callback: ValueCallback<Array<Uri>>? ->
                    viewModel.onSaveFileCallback(callback)
                    selectImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }

            AndroidView(
                modifier = Modifier.weight(1f),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        loadUrl("file:///android_res/raw/user_feedback_form.html?accountCcid=$accountId")
                        webChromeClient = getChromeClient(
                            onChooseFileCallback,
                            true,
                        )
                    }
                })
        } else {
            Box(Modifier.fillMaxSize()) {
                BusyIndicatorFloatingTopCenter(true)
            }
        }
    }
}


private fun getChromeClient(
    onChooseFileCallback: (ValueCallback<Array<Uri>>?) -> Unit = {},
    printLogs: Boolean = false
): WebChromeClient {
    return object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            onChooseFileCallback(filePathCallback)
            return true
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (!printLogs) {
                return false
            }

            consoleMessage?.let { message ->
                val logMessage = with(message) {
                    "${message()}. Line ${lineNumber()} of ${sourceId()}"
                }
                Log.d("web-view", logMessage)
            }
            return true
        }
    }
}