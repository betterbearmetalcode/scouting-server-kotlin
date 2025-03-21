import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.StrictMath.pow
import kotlin.random.Random

fun mutate(weights: MutableMap<String, Double>, chance: Double = 0.1, strength: Double = 0.1) {
    weights.replaceAll { key: String, value: Double ->
        if (Math.random() < chance)
            value + (Random.nextDouble(-strength, strength))
        else
            value
    }
}

fun train(weights: Map<String, Double>, ideal: List<Int>, finalVals: Map<String, HashMap<Int, Double>>, numConcurrent: Int, generations: Int) = runBlocking<Map<String, Double>> {
    var bestFitness = fitness(genList(weights, finalVals), ideal)

    var bestWeights = weights
    repeat(generations) { generation ->
        println("Starting generation #$generation")
        val weightsThisGen = ArrayList<Map<String, Double>>()

        repeat(numConcurrent) {
            val weight = bestWeights.toMutableMap()
            mutate(weight, 1-bestFitness, 1-bestFitness)
            weightsThisGen.add(weight)
        }
        val fitnesses = ArrayList<Pair<Double, Map<String, Double>>>()
        weightsThisGen.forEach {
            launch {
                fitnesses.add(Pair(fitness(genList(it, finalVals), ideal), it))
            }
        }

        fitnesses.forEach {
            if (it.first > bestFitness) {
                bestFitness = it.first
                bestWeights = it.second
            }
        }

        println("Best Fitness this generation - $bestFitness")
        println("------------------------")
    }

    bestWeights
}

fun genList(weights: Map<String, Double>, finalMap: Map<String, HashMap<Int, Double>>) : List<Int> {
    val weightedMap = HashMap<String, HashMap<Int, Double>>()
    for (entry in finalMap) {
        weightedMap[entry.key] = entry.value.clone() as java.util.HashMap<Int, Double>
    }
    val weightsAsDouble = HashMap<String, Double>()
    weights.forEach { (key, value) ->
        weightsAsDouble[key] = value
    }
    val teams = calculateList(weightsAsDouble, weightedMap) as java.util.HashMap<Int, Double>
    val sortedMap = ArrayList<Int>()

    teams.forEach {
        sortedMap.add(it.key)
    }

    sortedMap.sortByDescending { it }

    return sortedMap
}

fun fitness(exampleOutput: List<Int>, ideal: List<Int>) : Double {
    var fit = 0.0

    fun formula(input: Int) = pow(Math.E, pow(0.1 * input, 4.0))

    repeat(exampleOutput.size) {
        if (exampleOutput[it] == ideal[it])
            fit += formula(it)
    }

    return fit / 9.56402477056 // Magic number go brrrrrrrrrrrr (limit of the summation)
}