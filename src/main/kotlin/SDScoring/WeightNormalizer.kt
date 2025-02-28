package SDScoring

fun normalizeWeights(weights: DoubleArray): DoubleArray {
    val normalizedWeights = DoubleArray(weights.size)
    val sum: Double = weights.sum()
    for (i in weights.indices) {
        normalizedWeights[i] = weights[i] / sum
    }
    return normalizedWeights
}