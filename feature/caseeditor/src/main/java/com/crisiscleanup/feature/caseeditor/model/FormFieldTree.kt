package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.model.data.IncidentFormField

data class FormFieldNode(
    val formField: IncidentFormField,
    val children: List<FormFieldNode>,
    val options: Map<String, String>,
    val fieldKey: String = formField.fieldKey,
    val parentKey: String = formField.parentKey,
    val isRootNode: Boolean = fieldKey.isEmpty(),
) {
    companion object {
        fun buildTree(
            formFields: Collection<IncidentFormField>,
            keyTranslator: KeyTranslator,
        ): List<FormFieldNode> {
            val inputFormFields = formFields.filter { !it.isHidden }

            val lookup = mutableMapOf(Pair("", "") to EmptyIncidentFormField)
            inputFormFields.forEach { lookup[Pair(it.parentKey, it.fieldKey)] = it }

            val groupedByParent = mutableMapOf<String, MutableList<IncidentFormField>>()
            inputFormFields.forEach {
                val parentKey = it.parentKey
                if (!groupedByParent.containsKey(parentKey)) {
                    groupedByParent[parentKey] = mutableListOf()
                }
                groupedByParent[parentKey]!!.add(it)
            }
            groupedByParent.values.forEach {
                it.sortWith { a, b ->
                    val aListOrder = a.listOrder
                    val bListOrder = b.listOrder
                    if (aListOrder == bListOrder) {
                        return@sortWith if (a.labelOrder < b.labelOrder) -1 else 1
                    }
                    if (aListOrder < bListOrder) -1 else 1
                }
            }

            fun buildNode(parentKey: String, fieldKey: String): FormFieldNode {
                val children =
                    groupedByParent[fieldKey]?.map { buildNode(it.parentKey, it.fieldKey) }
                        ?: emptyList()
                val formField = lookup[Pair(parentKey, fieldKey)]!!
                val options = formField.values.ifEmpty {
                    formField.valuesDefault?.entries?.associate { it.key to (it.value ?: "") }
                        ?: emptyMap()
                }
                val translatedOptions = options.entries.associate {
                    val phraseKey = it.value
                    val value = keyTranslator.translate(phraseKey) ?: phraseKey
                    it.key to value
                }
                return FormFieldNode(formField, children, translatedOptions)
            }

            return buildNode("", "").children
        }
    }
}

fun FormFieldNode.flatten(): FormFieldNode {
    // TODO Flatten more than 1 level deep
    val flatChildren = children.fold(mutableListOf<FormFieldNode>()) { acc, node ->
        acc.add(node)
        acc.addAll(node.children)
        acc
    }
    return copy(children = flatChildren)
}

internal val EmptyIncidentFormField = IncidentFormField(
    "",
    "",
    "",
    "",
    "",
    "",
    null,
    emptyMap(),
    false,
    "",
    isRequired = false,
    isReadOnly = false,
    isReadOnlyBreakGlass = false,
    labelOrder = 0,
    listOrder = 0,
    isInvalidated = false,
    fieldKey = "",
    parentKey = "",
    selectToggleWorkType = "",
)

internal val EmptyFormFieldNode = FormFieldNode(
    EmptyIncidentFormField,
    emptyList(),
    emptyMap(),
)
