package com.crisiscleanup.feature.userfeedback

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.data.repository.AccountDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class UserFeedbackViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
) : ViewModel() {
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    val accountId = accountDataRepository.accountData.map { it.id }

    init {
        WebView.setWebContentsDebuggingEnabled(true);
    }

    fun onSaveFileCallback(callback: ValueCallback<Array<Uri>>?) {
        fileCallback = callback
    }

    fun onMediaSelected(uri: Uri) {
        fileCallback?.onReceiveValue(arrayOf(uri))
        fileCallback = null
    }
}