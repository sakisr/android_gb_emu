package com.example.emulator

import org.junit.Test

import org.junit.Assert.*
import java.util.*

class GameboyBinaryCalculationTest() {
    @Test
    fun gameboy_test_convertToBits() {
        val gb = GameBoy()
        val result1 : IntArray = intArrayOf(0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,0)
        val result2 : IntArray = intArrayOf(1,1,0,0,0,0,1,0,1,1,0,1,0,1,1,1)
        assert(gb.convertToBits(0x01, 0x02).contentEquals(result1))
        assert(gb.convertToBits(0xc2, 0xd7).contentEquals(result2))
    }
    @Test
    fun gameboy_test_performCalculation() {
        val gb = GameBoy()
        val result1 : IntArray = intArrayOf(0,0,0,0,0,0,1,1)
        val result2 : IntArray = intArrayOf(1,1,1,0,0,0,0,1)
        assert(gb.performCalculation(0x01, 0x02, "add").contentEquals(result1))
        assert(gb.performCalculation(0x1a, 0xc7, "add").contentEquals(result2))
    }
}