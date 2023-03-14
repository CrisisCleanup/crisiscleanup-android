package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface EditableWorksiteProvider {
    val editableWorksite: MutableStateFlow<Worksite>
    var formFields: List<FormFieldNode>
}

@Singleton
class SingleEditableWorksiteProvider @Inject constructor() : EditableWorksiteProvider {
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
    override var formFields: List<FormFieldNode> = emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
interface EditableWorksiteModule {
    @Binds
    @Singleton
    fun bindsEditableWorksiteProvider(
        provider: SingleEditableWorksiteProvider
    ): EditableWorksiteProvider
}