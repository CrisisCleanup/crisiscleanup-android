package com.crisiscleanup.feature.team.model

enum class TeamEditorStep(val literal: String, val translateKey: String) {
    Info("info", "~~Team Info"),
    Members("members", "~~Members"),
    Cases("cases", "~~Cases"),
    Equipment("equipment", "~~Equipment"),
    Review("review", "~~Review"),
}

private val literalStepLookup = TeamEditorStep.entries.associateBy(TeamEditorStep::literal)
fun stepFromLiteral(literal: String) = literalStepLookup[literal] ?: TeamEditorStep.Info
