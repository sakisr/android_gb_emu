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
    fun Gameboy_CheckCarry() {
        val gb = GameBoy()
        assertEquals(gb.checkCarry(15, 15, "add"), true)
    }
}