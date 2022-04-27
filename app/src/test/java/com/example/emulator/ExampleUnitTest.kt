package com.example.emulator

import org.junit.Test

class GameboyBinaryCalculationTest() {
    @Test
    fun gameboy_test_convertToBits() {
        val gb = GameBoy()
        val result1 : IntArray = intArrayOf(0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,0)
        val result2 : IntArray = intArrayOf(1,1,0,0,0,0,1,0,1,1,0,1,0,1,1,1)
        val result3 : IntArray = intArrayOf(1,1,1,1,1,1,1,1,0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0,1,1)
        assert(gb.convertToBits(0x01, 0x02).contentEquals(result1))
        assert(gb.convertToBits(0xc2, 0xd7).contentEquals(result2))
        assert(gb.convertToBits(0x1fe3, 0x1fe3).contentEquals(result3))
    }
    @Test
    fun gameboy_test_performCalculation() {
        val gb = GameBoy()
        //Addition
        val result1add : IntArray = intArrayOf(0,0,0,0,0,0,1,1)
        val result2add : IntArray = intArrayOf(1,1,1,0,0,0,0,1)
        val result3add : IntArray = intArrayOf(1,1,1,1,1,1,1,0)
        val result4add : IntArray = intArrayOf(0,0,0,0,0,0,0,1)

        assert(gb.performCalculation(0x01, 0x02, "add").contentEquals(result1add))
        assert(gb.performCalculation(0x1a, 0xc7, "add").contentEquals(result2add))
        assert(gb.performCalculation(0xff, 0xff, "add").contentEquals(result3add))
//        assert(gb.performCalculation(0xfa, 0x06, "add").contentEquals(result4add))

        // Addition with carry
        val result1adc : IntArray = intArrayOf(1,1,1,0,0,0,1,0)
        val result2adc : IntArray = intArrayOf(1,1,1,1,0,0,0,0)

        gb.setFlag('C', 1)
        assert(gb.performCalculation(0x1a, 0xc7, "adc").contentEquals(result1adc))
        gb.setFlag('C', 1)
        assert(gb.performCalculation(0xf0, 0xff, "adc").contentEquals(result2adc))

        // Subtraction
        val result1sub : IntArray = intArrayOf(0,0,0,0,0,0,1,1)
        val result2sub : IntArray = intArrayOf(0,1,0,1,0,0,1,1)
        val result3sub : IntArray = intArrayOf(0,0,0,0,0,0,0,0)
        val result4sub = 250

//        assert(gb.performCalculation(0x04, 0x01, "sub").contentEquals(result1sub))
//        assert(gb.performCalculation(0x1a, 0xc7, "sub").contentEquals(result2sub))
//        assert(gb.performCalculation(0xff, 0xff, "sub").contentEquals(result3sub))
        val test = gb.intToBinarySubtractionHex(0x01, 0x11)
        assert(gb.intToBinarySubtractionHex(0x01, 0x11).equals(result4sub))

    }
    @Test
    fun gameboy_test_binaryToInteger() {
        val gb = GameBoy()
        val result1 = 488
        val result2 = 6543
        assert(gb.binaryToInteger(intArrayOf(1,1,1,1,0,1,0,0,0)).equals(result1))
        assert(gb.binaryToInteger(intArrayOf(1,1,0,0,1,1,0,0,0,1,1,1,1)).equals(result2))
    }
}