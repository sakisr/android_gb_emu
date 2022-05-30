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
        //val result4add : IntArray = intArrayOf(0,0,0,0,0,0,0,1)
        val result4add = 0x00
        assert(gb.performCalculation(0x01, 0x02, "add").contentEquals(result1add))
        assert(gb.performCalculation(0x1a, 0xc7, "add").contentEquals(result2add))
        assert(gb.performCalculation(0xff, 0xff, "add").contentEquals(result3add))
        assert(gb.intToBinaryAdditionHex(0xfa, 0x06).equals(result4add))

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
        val result4sub = 240
        val result5sub = 19

//        assert(gb.performCalculation(0x04, 0x01, "sub").contentEquals(result1sub))
//        assert(gb.performCalculation(0x1a, 0xc7, "sub").contentEquals(result2sub))
//        assert(gb.performCalculation(0xff, 0xff, "sub").contentEquals(result3sub))
        assert(gb.intToBinarySubtractionHex(0x01, 0x11).equals(result4sub))
        assert(gb.intToBinarySubtractionHex(0x00, 0xed).equals(result5sub))
    }
    @Test
    fun gameboy_test_binaryToInteger() {
        val gb = GameBoy()
        val result1 = 488
        val result2 = 6543
        assert(gb.binaryToInteger(intArrayOf(1,1,1,1,0,1,0,0,0)).equals(result1))
        assert(gb.binaryToInteger(intArrayOf(1,1,0,0,1,1,0,0,0,1,1,1,1)).equals(result2))
    }
    @Test
    fun gameboy_test_rotateBits() {
        val gb = GameBoy()

        // Testing register A bit rotation
        val result1A = 0xcf
        gb.regAF[0] = 0xe7
        gb.rotateBits('A', "left")
        assert(gb.regAF[0].equals(result1A))

        val result2A = 0x2c
        gb.regAF[0] = 0x58
        gb.rotateBits('A', "right")
        assert(gb.regAF[0].equals(result2A))

        // Testing register B bit rotation
        val result1B = 0xcf
        gb.regBC[0] = 0xe7
        gb.rotateBits('B', "left")
        assert(gb.regBC[0].equals(result1B))

        val result2B = 0x2c
        gb.regBC[0] = 0x58
        gb.rotateBits('B', "right")
        assert(gb.regBC[0].equals(result2B))

        // Testing register C bit rotation
        val result1C = 0xcf
        gb.regBC[1] = 0xe7
        gb.rotateBits('C', "left")
        assert(gb.regBC[1].equals(result1C))

        val result2C = 0x2c
        gb.regBC[1] = 0x58
        gb.rotateBits('C', "right")
        assert(gb.regBC[1].equals(result2C))

        // Testing register D bit rotation
        val result1D = 0xcf
        gb.regDE[0] = 0xe7
        gb.rotateBits('D', "left")
        assert(gb.regDE[0].equals(result1D))

        val result2D = 0x2c
        gb.regDE[0] = 0x58
        gb.rotateBits('D', "right")
        assert(gb.regDE[0].equals(result2D))

        // Testing register E bit rotation
        val result1E = 0xcf
        gb.regDE[1] = 0xe7
        gb.rotateBits('E', "left")
        assert(gb.regDE[1].equals(result1E))

        val result2E = 0x2c
        gb.regDE[1] = 0x58
        gb.rotateBits('E', "right")
        assert(gb.regDE[1].equals(result2E))

        // Testing register H bit rotation
        val result1H = 0xcf
        gb.regHL[0] = 0xe7
        gb.rotateBits('H', "left")
        assert(gb.regHL[0].equals(result1H))

        val result2H = 0x2c
        gb.regHL[0] = 0x58
        gb.rotateBits('H', "right")
        assert(gb.regHL[0].equals(result2H))

        // Testing register L bit rotation
        val result1L = 0xcf
        gb.regHL[1] = 0xe7
        gb.rotateBits('L', "left")
        assert(gb.regHL[1].equals(result1L))

        val result2L = 0x2c
        gb.regHL[1] = 0x58
        gb.rotateBits('L', "right")
        assert(gb.regHL[1].equals(result2L))
    }
}