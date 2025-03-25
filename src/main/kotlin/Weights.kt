import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Math.pow
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

fun mutate(weights: MutableMap<String, Double>, chance: Double = 0.1, strength: Double = 0.1) {
    weights.replaceAll { key: String, value: Double ->
        if (Math.random() < chance)
            value + (Random.nextDouble(-strength, strength))
        else
            value
    }
}

fun train(
    weights: Map<String, Double>,
    ideal: List<Int>,
    finalVals: Map<String, HashMap<Int, Double>>,
    numConcurrent: Int,
    generations: Int
) = runBlocking<Map<String, Double>> {
    var bestFitness = fitness(genList(weights, finalVals), ideal)

    var bestWeights = weights
    repeat(generations) { generation ->
        println("Starting generation #$generation")
        val weightsThisGen = ArrayList<Map<String, Double>>()

        repeat(numConcurrent) {
            val weight = bestWeights.toMutableMap()
            mutate(weight, 1.0, 1-bestFitness)
            weightsThisGen.add(weight)
        }
        val fitnesses = ArrayList<Pair<Double, Map<String, Double>>>()
        val jobs = ArrayList<Job>()
        weightsThisGen.forEach {
            jobs.add(
                launch {
                    fitnesses.add(Pair(fitness(genList(it, finalVals), ideal), it))
                }
            )
        }

        jobs.forEach { it.join() }

        fitnesses.forEach {
            if (it.first > bestFitness) {
                bestFitness = it.first
                bestWeights = it.second
            }
        }

        println("Best Fitness this generation - $bestFitness")
        println("------------------------")
    }

    println(averageDistance(genList(bestWeights, finalVals), ideal))

    bestWeights
}

fun genList(weights: Map<String, Double>, finalMap: Map<String, HashMap<Int, Double>>): List<Int> {
    val weightedMap = HashMap<String, HashMap<Int, Double>>()
    for (entry in finalMap) {
        weightedMap[entry.key] = entry.value.clone() as java.util.HashMap<Int, Double>
    }
    val weightsAsDouble = HashMap<String, Double>()
    weights.forEach { (key, value) ->
        weightsAsDouble[key] = value
    }
    val teams = calculateList(weightsAsDouble, weightedMap) as java.util.HashMap<Int, Double>
    val sortedMap = ArrayList<Pair<Int, Double>>()

    teams.forEach {
        sortedMap.add(it.toPair())
    }

    sortedMap.sortByDescending { it.second }

    val fin = ArrayList<Int>()
    sortedMap.forEach { fin.add(it.first) }

    return fin
}

fun fitness(exampleOutput: List<Int>, ideal: List<Int>) : Double {
    var fit = 0.0

    fun formula(input: Double) = Math.E.pow(-input)

    repeat(exampleOutput.size) {
        val targetTeam = exampleOutput[it]
        var dist = it - ideal.indexOf(targetTeam)
        dist = dist.absoluteValue + 1

        val sqrtDist = sqrt(dist.toDouble())
        val formIn = ((it + 1) * sqrtDist) - 1

        val formVal = formula(formIn)


        fit += formVal
    }

    return fit / 1.58197670687 // Magic number go brrrrrrrrrrrr (integral of the formula from 0 to infinity)
}

fun averageDistance(exampleOutput: List<Int>, ideal: List<Int>) : Double {
    var total = 0.0
    repeat(exampleOutput.size) {
        val team = exampleOutput[it]

        total += (it - ideal.indexOf(team)).absoluteValue
    }

    return total / exampleOutput.size
}