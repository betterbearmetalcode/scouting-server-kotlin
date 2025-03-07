package SDScoring

import Order
import RankType
import manager
import org.bson.Document
import org.tahomarobotics.scouting.DatabaseType
import java.util.EnumMap

class Ranker(rankType: RankType, teamNumber: Int, eventKey: String) {
    companion object {
        private var PLAY: EnumMap<RankType, HashMap<Int, HashMap<Int, HashMap<Boolean, Int>>>>? = null


        fun setPlay(play: EnumMap<RankType, HashMap<Int, HashMap<Int, HashMap<Boolean, Int>>>>) {
            PLAY = play
        }


    }
    private var RANK: Double = 0.0

    fun getRank(): Double {
        return RANK
    }

    init {
        val matches = PLAY!![rankType]!![teamNumber]!!
        val maxMatch = matches.size
        var sum = 0.0

        val stratData = manager.getDataFromEvent(DatabaseType.STRATEGY, eventKey)


        matches.forEach { (key, value) ->
            var tempVal : Document? = null
            val redTeam = value[true]
            val blueTeam = value[false]
            stratData.forEach {
                if (
                    it["match"] as Int == key &&
                        ((redTeam != null && it["is_red_alliance"] as Boolean) ||
                        (blueTeam != null && !(it["is_red_alliance"] as Boolean)))
                ) {
                    tempVal = when (rankType) {
                        RankType.STRATEGY -> it["strategy"] as Document
                        RankType.DRIVING_SKILL -> it["driving_skill"] as Document
                        RankType.MECHANICAL_SOUNDNESS -> it["mechanical_soundness"] as Document
                    }
                }
            }
            val teams = tempVal!!

            val order = Order(teams["1"] as Int, teams["2"] as Int, teams["3"] as Int)
            sum += weighter(order, rankType, key, teamNumber)
            tempVal = null
        }
        RANK = sum / maxMatch
    }

    fun averager(rankType: RankType, teamNumber: Int): Double {
        var sum = 0
        val matches = PLAY!![rankType]!![teamNumber]!!
        val numMatches = matches.size
        matches.forEach { (key, value) ->
            val redScore = value[true]
            val blueScore = value[false]
            sum += redScore ?: blueScore ?: 0
        }
        return sum.toDouble() / numMatches
    }

    fun weighter(order: Order, rankType: RankType, matchNumber: Int, teamNumber: Int): Double {
        val teammates = order.finder(teamNumber)
        val teamScore = averager(rankType, teamNumber)
        val teammateOneScore = averager(rankType, teammates!![0])
        val teammateTwoScore = averager(rankType, teammates[1])
        return teamScore * (teammateOneScore + teammateTwoScore)
    }
}