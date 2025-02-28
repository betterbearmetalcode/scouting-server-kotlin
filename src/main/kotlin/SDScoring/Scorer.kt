package SDScoring

import java.util.*

fun scoring(normalizedWeights: DoubleArray, sdScores: Array<DoubleArray>) {
    for (r in normalizedWeights.indices) {
        val max = Arrays.stream(sdScores[r]).max().asDouble
        if (max != 0.0) {
            for (c in sdScores[r].indices) {
                sdScores[r][c] = normalizedWeights[r] * sdScores[r][c] / max
                //normalizedWeights[r] might need to be changed based on how it's stored.
            }
        }
    }
}