package com.crisiscleanup.feature.team.model

enum class TeamEditorStep(val literal: String, val translateKey: String) {
    Name("name", "~~Team Name"),
    Members("members", "~~Members"),
    Cases("cases", "~~Cases"),
    Equipment("equipment", "~~Assets"),
    Review("review", "~~Review"),
}

private val literalStepLookup = TeamEditorStep.entries.associateBy(TeamEditorStep::literal)
fun stepFromLiteral(literal: String) = literalStepLookup[literal] ?: TeamEditorStep.Name
