import org.bson.json.JsonObject
import org.tahomarobotics.scouting.DatabaseType.MATCH
import kotlin.collections.component1
import kotlin.collections.component2

class RankedScouting {
    val scoutingRankHash = arrayOf(HashMap<String, Int>())
    val scoutingMistakes = arrayOf(HashMap<String, Int>())

    fun compareDatabases(eventKey : String, firstIndex : String, secondIndex : String){
        val matches = manager.get(databaseIndex.value)?.getMatchesFromEvent(eventKey)
            matches?.forEach { it ->
            val match = matches.indexOf(it)
            manager.get(firstIndex)?.getDataFromMatch(MATCH, match ,eventKey)?.forEach { dataPerRobot ->
                var startingLocation = manager.get(firstIndex)?.getDataFromMatch(MATCH, match ,eventKey)?.indexOf(dataPerRobot)
                var mistakes = 0
                var scout = ""
                dataPerRobot.forEach { (key, firstValue) ->
                    var secondValue = startingLocation.ifNullEqual0().let { it1 ->
                        manager.get(secondIndex)?.getDataFromMatch(MATCH, match ,eventKey)?.get(it1)
                    }?.get(key)
                        if (firstValue != secondValue){
                        mistakes++
                        }
                        //TODO add xp to Scouting RankHash & change scout value
                    }
                scoutingMistakes.get(match).set(scout,mistakes)
                }
            }
        }


    fun applyScoutingMistakes(){
        scoutingRankHash.forEach { match ->
            match.forEach {(scout, mistakes) ->
                match.set(scout, match.get(scout).ifNullEqual0().times(((32-mistakes/32).toDouble())).toInt())
            }
        }

    }

    fun matchMultiplier(eventKey : String){
//        1.6 * scoutXp to make allianceMultiplier work together to make total 2x
        val matches = manager.get(databaseIndex.value)?.getMatchesFromEvent(eventKey)
        var totalMistakes = 0
        matches?.forEach { match ->
            val matchNum = matches.indexOf(match)
            var scouts = arrayOf("")
            scoutingMistakes.get(matchNum).forEach { (scout, mistakes) ->
                    scouts.plus(scout)
                    totalMistakes += mistakes
            }
            if(totalMistakes == 0){
                scouts.forEach { scout ->
                    scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.6).toInt()))
                }
            }
            totalMistakes = 0
        }
    }


    fun allianceMultiplier(eventKey : String) {
        val matches = manager.get(databaseIndex.value)?.getMatchesFromEvent(eventKey)
        var i = 0
        var redMistakes = 0
        var blueMistakes = 0
        //Change this to be based off of actual json data
        matches?.forEach { match ->
            val matchNum = matches.indexOf(match)
            var redScouts = arrayOf("")
            var blueScouts = arrayOf("")
            scoutingMistakes.get(matchNum).forEach { (scout, mistakes) ->
                i++
                if(i <= 3){
                    redScouts.set(i,scout)
                    redMistakes += mistakes
                }else{
                    blueScouts.set(i-3,scout)
                    blueMistakes += mistakes
                }
            }
            if(redMistakes == 0){
                redScouts.forEach { scout ->
                    scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.25).toInt()))
                }
            }
            if (blueMistakes == 0){
                blueScouts.forEach { scout ->
                    scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.25).toInt()))
                }
            }
            redMistakes = 0
            blueMistakes = 0
        }
    }

    fun applyStreaks(){
        scoutingMistakes.forEach { match ->
            var matchNum = scoutingMistakes.indexOf(match)
            var streakHash = HashMap<String, Int>()
            match.forEach { (scout, mistakes) ->
                if(mistakes == 0){
                    streakHash.set(scout, streakHash.get(scout).ifNullEqual0().plus(1))
                }else{
                    streakHash.set(scout, 0)
                }
                when{
                    streakHash.get(scout) == 2->{
                        scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.25).toInt()))
                    }
                    streakHash.get(scout) == 3 ->{
                        scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.5).toInt()))
                    }
                    streakHash.get(scout) == 4->{
                        scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(1.75).toInt()))
                    }
                    streakHash.get(scout)!! <= 5 ->{
                        scoutingRankHash.get(matchNum).set(scout, (scoutingRankHash.get(matchNum).get(scout).ifNullEqual0().times(2).toInt()))
                    }
                    else -> {
                        print("No Streak Change")
                    }
                }
            }
        }
    }

    fun getScoutingRanksAsJson(): JsonObject{
        return JsonObject("ranks")
    }



    /**makeSureColtonIsBetterThanNatik
     * pretty much just prank Natik by taking his score and adding a random amount of Xp
     *
     */
    fun mSCIBTN(){

    }

    fun Int?.ifNullEqual0():Int{
        if(this == null) {
            return 0
        }
        return this
    }
}