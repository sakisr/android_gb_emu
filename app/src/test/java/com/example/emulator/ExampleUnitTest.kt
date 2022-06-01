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

    @Test
    fun gameboy_test_rotateBitsThroughCarry() {
        val gb = GameBoy()

        // Testing register A bit rotation through carry
        val result1A = 0xcb
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('A', "left")
        assert(gb.regAF[0].equals(result1A))

        val result2A = 0xf2
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('A', "right")
        assert(gb.regAF[0].equals(result2A))

        val result3A = 0xca
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('A', "left")
        assert(gb.regAF[0].equals(result3A))

        val result4A = 0x72
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('A', "right")
        assert(gb.regAF[0].equals(result4A))

        // Testing register B bit rotation through carry
        val result1B = 0xcb
        gb.regBC[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('B', "left")
        assert(gb.regBC[0].equals(result1B))

        val result2B = 0xf2
        gb.regBC[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('B', "right")
        assert(gb.regBC[0].equals(result2B))

        val result3B = 0xca
        gb.regBC[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('B', "left")
        assert(gb.regBC[0].equals(result3B))

        val result4B = 0x72
        gb.regBC[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('B', "right")
        assert(gb.regBC[0].equals(result4B))

        // Testing register C bit rotation through carry
        val result1C = 0xcb
        gb.regBC[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('C', "left")
        assert(gb.regBC[1].equals(result1C))

        val result2C = 0xf2
        gb.regBC[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('C', "right")
        assert(gb.regBC[1].equals(result2C))

        val result3C = 0xca
        gb.regBC[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('C', "left")
        assert(gb.regBC[1].equals(result3C))

        val result4C = 0x72
        gb.regBC[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('C', "right")
        assert(gb.regBC[1].equals(result4C))

        // Testing register D bit rotation through carry
        val result1D = 0xcb
        gb.regDE[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('D', "left")
        assert(gb.regDE[0].equals(result1D))

        val result2D = 0xf2
        gb.regDE[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('D', "right")
        assert(gb.regDE[0].equals(result2D))

        val result3D = 0xca
        gb.regDE[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('D', "left")
        assert(gb.regDE[0].equals(result3D))

        val result4D = 0x72
        gb.regDE[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('D', "right")
        assert(gb.regDE[0].equals(result4D))

        // Testing register E bit rotation through carry
        val result1E = 0xcb
        gb.regDE[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('E', "left")
        assert(gb.regDE[1].equals(result1E))

        val result2E = 0xf2
        gb.regDE[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('E', "right")
        assert(gb.regDE[1].equals(result2E))

        val result3E = 0xca
        gb.regDE[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('E', "left")
        assert(gb.regDE[1].equals(result3E))

        val result4E = 0x72
        gb.regDE[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('E', "right")
        assert(gb.regDE[1].equals(result4E))

        // Testing register H bit rotation through carry
        val result1H = 0xcb
        gb.regHL[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('H', "left")
        assert(gb.regHL[0].equals(result1H))

        val result2H = 0xf2
        gb.regHL[0] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('H', "right")
        assert(gb.regHL[0].equals(result2H))

        val result3H = 0xca
        gb.regHL[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('H', "left")
        assert(gb.regHL[0].equals(result3H))

        val result4H = 0x72
        gb.regHL[0] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('H', "right")
        assert(gb.regHL[0].equals(result4H))

        // Testing register L bit rotation through carry
        val result1L = 0xcb
        gb.regHL[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('L', "left")
        assert(gb.regHL[1].equals(result1L))

        val result2L = 0xf2
        gb.regHL[1] = 0xe5
        gb.setFlag('C', 1)
        gb.rotateBitsThroughCarry('L', "right")
        assert(gb.regHL[1].equals(result2L))

        val result3L = 0xca
        gb.regHL[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('L', "left")
        assert(gb.regHL[1].equals(result3L))

        val result4L = 0x72
        gb.regHL[1] = 0xe5
        gb.setFlag('C', 0)
        gb.rotateBitsThroughCarry('L', "right")
        assert(gb.regHL[1].equals(result4L))
    }
    @Test
    fun gameboy_test_shiftBits() {
        val gb = GameBoy()

        val result1A = 0xca
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 1)
        gb.shiftBits('A', "left")
        assert(gb.regAF[0].equals(result1A))

        val result2A = 0xf2
        gb.regAF[0] = 0xe5
        gb.setFlag('C', 1)
        gb.shiftBits('A', "right")
        assert(gb.regAF[0].equals(result2A))

        val result3A = 0x72
        gb.regAF[0] = 0xe5
        gb.shiftBitsRightWithZero('A')
        assert(gb.regAF[0].equals(result3A))

        val result1M = 0xca
        gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])] = 0xe5.toUByte().toByte()
        gb.setFlag('C', 1)
        gb.shiftBits('M', "left")
        assert(gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])].toUByte().toInt().equals(result1M))

        val result2M = 0xf2
        gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])] = 0xe5.toUByte().toByte()
        gb.setFlag('C', 1)
        gb.shiftBits('M', "right")
        assert(gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])].toUByte().toInt().equals(result2M))
    }
    @Test
    fun gameboy_test_swapbits() {
        val gb = GameBoy()

        val resultA = 0xf0
        gb.regAF[0] = 0x0f
        gb.swapBits('A')
        assert(gb.regAF[0].equals(resultA))

        val resultB = 0xf0
        gb.regBC[0] = 0x0f
        gb.swapBits('B')
        assert(gb.regBC[0].equals(resultB))

        val resultC = 0xf0
        gb.regBC[1] = 0x0f
        gb.swapBits('C')
        assert(gb.regBC[1].equals(resultC))

        val resultD = 0xf0
        gb.regDE[0] = 0x0f
        gb.swapBits('D')
        assert(gb.regDE[0].equals(resultD))

        val resultE = 0xf0
        gb.regDE[1] = 0x0f
        gb.swapBits('E')
        assert(gb.regDE[1].equals(resultE))

        val resultH = 0xf0
        gb.regHL[0] = 0x0f
        gb.swapBits('H')
        assert(gb.regHL[0].equals(resultH))

        val resultL = 0x0f
        gb.regHL[1] = 0xf0
        gb.swapBits('L')
        assert(gb.regHL[1].equals(resultL))

        val resultM = 0xf0
        gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])] = 0x0f.toUByte().toByte()
        gb.swapBits('M')
        assert(gb.memory[gb.bytesToWord(gb.regHL[0],gb.regHL[1])].toUByte().toInt().equals(resultM))
    }
}