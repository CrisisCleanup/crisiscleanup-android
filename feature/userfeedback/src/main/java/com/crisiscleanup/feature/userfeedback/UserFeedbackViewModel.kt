package com.crisiscleanup.feature.userfeedback

import android.net.Uri
import android.webkit.ValueCallback
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserFeedbackViewModel @Inject constructor() : ViewModel() {
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    fun onSaveFileCallback(callback: ValueCallback<Array<Uri>>?) {
        fileCallback = callback
    }

    fun onMediaSelected(uri: Uri) {
        fileCallback?.onReceiveValue(arrayOf(uri))
        fileCallback = null
    }
}