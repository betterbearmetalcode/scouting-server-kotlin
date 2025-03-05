package SDScoring

import java.util.Arrays

fun teamScorer(weightedScoringMatrix: Array<DoubleArray>): DoubleArray {
    val teamScores = DoubleArray(weightedScoringMatrix[0].size)
    for (c in weightedScoringMatrix[0].indices) {
        for (r in weightedScoringMatrix.indices) {
            teamScores[c] += weightedScoringMatrix[r][c]
        }
    }
    return teamScores
}

fun teamsRanked(teamScores: DoubleArray): DoubleArray {
    return Arrays.stream(teamScores).sorted().toArray()
}