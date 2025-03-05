data class Order(
    val rankOne: Int, val rankTwo: Int, val rankThree: Int
) {

    fun finder(team: Int): IntArray? {
        return when (team) {
            rankOne -> intArrayOf(rankTwo, rankThree)
            rankTwo -> intArrayOf(rankThree, rankOne)
            rankThree -> intArrayOf(rankThree, rankOne)
            else -> null
        }
    }
}