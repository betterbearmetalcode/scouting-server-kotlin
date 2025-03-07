package SDScoring

import java.util.*

fun scoring(normalizedWeights: Map<String, Double>, sdScores: Map<String, HashMap<Int, Double>>) {
    for ((key, value) in normalizedWeights) {
        val max = sdScores[key]!!.values.max()
        if (max != 0.0) {
            val scores = sdScores[key]!!
            for ((teamKey, zScore) in scores) {
                scores[teamKey] = value * zScore / max
            }
        }
    }
}