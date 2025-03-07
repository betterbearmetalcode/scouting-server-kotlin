package SDScoring

import kotlin.math.absoluteValue

fun normalizeWeights(weights: Map<String, Double>): Map<String, Double> {
    val normalizedWeights = HashMap<String, Double>()
    val sum: Double = weights.values.sum()
    for ((key, value) in weights) {
        normalizedWeights[key] = value.absoluteValue / sum
    }
    return normalizedWeights
}