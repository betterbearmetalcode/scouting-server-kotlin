package SDScoring

import kotlin.math.absoluteValue

fun normalizeWeights(weights: Map<String, Double>): Map<String, Double> {
    val normalizedWeights = HashMap<String, Double>()
    var sum = 0.0
    weights.values.forEach { weightValue ->
        sum += weightValue.absoluteValue
    }
    for ((key, value) in weights) {
        normalizedWeights[key] = value / sum
    }
    return normalizedWeights
}