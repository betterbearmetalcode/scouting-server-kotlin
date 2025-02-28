import kotlin.math.pow
import kotlin.math.sqrt

fun sdScorer(list: HashMap<Int, Double>): HashMap<Int, Double> {
    val average = list.values.average()
    var difference = 0.0
    for (item in list.values) {
        val value = item - average
        difference += value.pow(2)
    }
    val sd = sqrt(difference / list.size)
    val sdScored = HashMap<Int, Double>()
    for ((i, item) in list.values.withIndex()) {
        val change = item - average
        var sdScore = change / sd
        if (sdScore.isInfinite() || sdScore.isNaN()) {
            sdScore = 0.0
        }
        sdScored[list.keys.elementAt(i)] = sdScore
    }
    return sdScored
}