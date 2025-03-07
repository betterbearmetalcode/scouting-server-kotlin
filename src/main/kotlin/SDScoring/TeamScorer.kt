package SDScoring

fun teamScorer(weightedScoringMatrix: Map<String, HashMap<Int, Double>>): Map<Int, Double> {
    val teamScores = HashMap<Int, Double>()
    val listOfTeams = weightedScoringMatrix[weightedScoringMatrix.keys.first()]!!.keys
    for (team in listOfTeams) {
        for ((key, value) in weightedScoringMatrix) {
            if (teamScores.putIfAbsent(team, value[team]!!) != null) {
                teamScores[team] = teamScores[team]!! + value[team]!!
            }
        }
    }
    return teamScores
}