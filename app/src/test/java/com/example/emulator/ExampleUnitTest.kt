package com.example.emulator

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
/*class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}*/
class GameboyBinaryCalculationTest() {
    @Test
    fun convertToBits_isCorrect() {
        val gb = GameBoy()
        val result1 : IntArray = intArrayOf(0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,0)
        assertEquals(gb.convertToBits(0x01, 0x02), result1)
    }
    @Test
    fun Gameboy_CheckCarry() {
        val gb = GameBoy()
        assertEquals(gb.performCalculation(15, 15, "add"), true)
    }

    @Test
    fun checkCarry_isCorrect () {
        //assertEquals(checkCarry())
    }
}