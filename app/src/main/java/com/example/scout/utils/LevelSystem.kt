package com.example.scout.utils

import androidx.annotation.StringRes
import com.example.scout.R

object LevelSystem {

    data class Level(val number: Int, @StringRes val titleRes: Int, val minSightings: Int, val nextAt: Int?)

    val levels = listOf(
        Level(1, R.string.level_1_title, 0,   5),
        Level(2, R.string.level_2_title, 5,   20),
        Level(3, R.string.level_3_title, 20,  50),
        Level(4, R.string.level_4_title, 50,  100),
        Level(5, R.string.level_5_title, 100, null)
    )

    fun forSightings(count: Int): Level =
        levels.lastOrNull { count >= it.minSightings } ?: levels.first()

    fun canCustomiseTitle(count: Int) = count >= 100

    // returns (progress, total) toward the next level, or null if max level.
    fun progressToNext(count: Int): Pair<Int, Int>? {
        val current = forSightings(count)
        val nextAt = current.nextAt ?: return null
        return Pair(count - current.minSightings, nextAt - current.minSightings)
    }
}