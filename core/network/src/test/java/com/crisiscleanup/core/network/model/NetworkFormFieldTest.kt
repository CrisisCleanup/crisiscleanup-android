package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test
import kotlin.test.assertEquals

class NetworkFormFieldTest {
    @Test
    fun deserializeFormFields() {
        val testCases = listOf(
            Pair("/incidentFormFields1.json", 61),
            Pair("/incidentFormFields2.json", 89),
            Pair("/incidentFormFields3.json", 70),
        )
        testCases.onEach {
            val result = TestUtil.decodeResource<IncidentFormField>(it.first)
            assertEquals(it.second, result.fields.size)

            // Inspect fields with inconsistent values
            /*
            result.fields
                .filter { fields -> fields.values?.isNotEmpty() == true && !fields.isExpectedValueDefault }
                .mapIndexed { index, fields ->
                    println("Irregular value defaults $index\n  ${fields.valuesDefault}\n  ${fields.values}")
                }
             */
        }
    }
}

@Serializable
private data class IncidentFormField(
    val id: Long,
    val name: String,
    @SerialName("form_fields")
    val fields: List<NetworkIncidentFormField>,
)