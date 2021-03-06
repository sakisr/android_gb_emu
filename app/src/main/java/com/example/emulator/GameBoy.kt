package com.example.emulator
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

// Gameboy starting address is 0x0100
val START_ADDRESS = 0x0100

class GameBoy {
    // 64 KiB of memory
    var memory = ByteArray(65536) {0x00}

    // 6 Registers - Can be used as two 8 bit registers or 1 16 bit register
    // ----------------------------------------------------------------
    // Accumulator & Flags Register (AF Register)
    // The Flags Register is the second byte of the AF Register
    // | Bit  | Flag
    // | 7    | Z - Zero Flag (when result is zero)
    // | 6    | N - Subtraction Flag (BCD)
    // | 5    | H - Half Carry Flag (BCD)
    // | 4    | C - Carry Flag
    var regAF = intArrayOf(0x01, 0xb0)
    // ----------------------------------------------------------------
    var regBC = intArrayOf(0x00, 0x13)  // BC Register
    var regDE = intArrayOf(0x00, 0xd8)  // DE Register
    var regHL = intArrayOf(0x01, 0x4d)  // HL Register
    var regSP = 0xfffe                  // Stack Pointer
    var regPC = START_ADDRESS           // Program Counter

    var opcode = 0x00
    var imeFlag =  0    // Interrupt Master Enable Flag

    var haltflag = 0    // Flag to halt execution (for debugging)

    // Fetch current instruction (opcode) from memory at the address pointed by the program counter, and increase program counter
    fun fetch() {
        /*
        print("Running opcode: 0x" + Integer.toHexString(memory[regPC].toUByte().toInt()) + " at memory address: 0x" + Integer.toHexString(regPC) +
                "\n\t | regAF: " + Integer.toHexString(regAF[0]).padStart(2, '0') + Integer.toHexString(regAF[1]).padStart(2, '0') +
                "\t|\tZ: " + getFlag('Z') +
                "\n\t | regBC: " + Integer.toHexString(regBC[0]).padStart(2, '0') + Integer.toHexString(regBC[1]).padStart(2, '0') +
                "\t|\tN: " + getFlag('N') +
                "\n\t | regDE: " + Integer.toHexString(regDE[0]).padStart(2, '0') + Integer.toHexString(regDE[1]).padStart(2, '0') +
                "\t|\tH: " + getFlag('H') +
                "\n\t | regHL: " + Integer.toHexString(regHL[0]).padStart(2, '0') + Integer.toHexString(regHL[1]).padStart(2, '0') +
                "\t|\tC: " + getFlag('C') +
                "\n\t | regPC: " + Integer.toHexString(regPC).padStart(4,'0') +
                "\n")

         */
        print("\t | regAF: " + Integer.toHexString(regAF[0]).padStart(2, '0') + Integer.toHexString(regAF[1]).padStart(2, '0') +
                "\t|\tZ: " + getFlag('Z') +
                "\n\t | regBC: " + Integer.toHexString(regBC[0]).padStart(2, '0') + Integer.toHexString(regBC[1]).padStart(2, '0') +
                "\t|\tN: " + getFlag('N') +
                "\n\t | regDE: " + Integer.toHexString(regDE[0]).padStart(2, '0') + Integer.toHexString(regDE[1]).padStart(2, '0') +
                "\t|\tH: " + getFlag('H') +
                "\n\t | regHL: " + Integer.toHexString(regHL[0]).padStart(2, '0') + Integer.toHexString(regHL[1]).padStart(2, '0') +
                "\t|\tC: " + getFlag('C') +
                "\n\t | regPC: " + Integer.toHexString(regPC).padStart(4,'0') +
                "\n" +
                "Running opcode: 0x" + Integer.toHexString(memory[regPC].toUByte().toInt()).padStart(2,'0') + " at memory address: 0x" + Integer.toHexString(regPC) + "\n")
        opcode = (memory[regPC].toInt())
        regPC += 0x01
    }
    // TODO: Implement flags
    // Decode current instruction (opcode)
    fun decode() {
        when(opcode.toUByte().toInt()) {
            // nn -> immediate data values (8-bit/16-bit)
            // (nn) -> immediate address values (8-bit/16-bit)

            // Doesn't execute anything
            0x00 -> return
            // Load next two bytes into registers BC
            0x01 -> {
                regBC[0] = memory[regPC+1].toUByte().toInt()
                regBC[1] = memory[regPC].toUByte().toInt()
                regPC += 0x02
            }
            // Store Accumulator Register contents into memory[regBC]
            0x02 -> {
                memory[bytesToWord(regBC[0],regBC[1])] = regAF[0].toUByte().toByte()
                //print("regB: " + Integer.toHexString(regBC[0]) + " regC: " + Integer.toHexString(regBC[1]) + " regBC: " + Integer.toHexString(bytesToWord(regBC[0],regBC[1])))
                //print("regA: " + regAF[0] + " memory[" + Integer.toHexString(bytesToWord(regBC[0],regBC[1])) + "]: " + memory[bytesToWord(regBC[0],regBC[1])])
            }
            // Increase regBC by 1
            0x03 -> addToRegisters("BC", 1)
            // Increase register B by 1
            0x04 -> regBC[0] = intToBinaryAdditionHex(regBC[0] , 1)
            // TODO: Test
            // Decrease register B by 1
            0x05 -> regBC[0] = intToBinarySubtractionHex(regBC[0], 1)
            // Load next byte to register B
            0x06 -> {
                regBC[0] = memory[regPC].toInt()
                regPC += 0x01
            }
            // Rotate contents of register A to the left
            0x07 -> {
                rotateBits('A', "left")
            }
            // TODO: Test
            // Store first and second bytes of regSP into memory addresses shown by immediate operand
            0x08 -> {
                val address = getNextTwoBytes(regPC)
                memory[address] = splitToBytes(regSP, 2)
                memory[address+1] = splitToBytes(regSP, 1)
                regPC += 0x02
            }
            //0x09 ->
            // Load memory[regBC] into Accumulator register
            0x0a -> {
                regAF[0] = memory[bytesToWord(regBC[0], regBC[1])].toUByte().toInt()
            }
            // Decrease registers BC by 1
            0x0b -> {
                addToRegisters("BC", -1)
            }
            // Increase register C by 1
            0x0c -> regBC[1] = intToBinaryAdditionHex(regBC[1] , 1)
            // TODO: Test
            // Decrease register C by 1
            0x0d -> regBC[1] = intToBinarySubtractionHex(regBC[1], 1)
            // Load immediate operand into register C
            0x0e -> {
                regBC[1] = memory[regPC].toUByte().toInt()
                regPC += 0x01
            }
            // Rotate contents of register A to the right
            0x0f -> {
                rotateBits('A', "right")
            }

            // STOP
            //0x10 ->
            // Load next two bytes into registers DE
            0x11 -> {
                regDE[0] = memory[regPC+1].toUByte().toInt()
                regDE[1] = memory[regPC].toUByte().toInt()
                regPC += 0x02
            }
            // Store Accumulator Register contents into memory[regDE]
            0x12 -> {
                memory[bytesToWord(regDE[0],regDE[1])] = regAF[0].toUByte().toByte()
                //print("regB: " + Integer.toHexString(regBC[0]) + " regC: " + Integer.toHexString(regBC[1]) + " regBC: " + Integer.toHexString(bytesToWord(regBC[0],regBC[1])))
                //print("regA: " + regAF[0] + " memory[" + Integer.toHexString(bytesToWord(regBC[0],regBC[1])) + "]: " + memory[bytesToWord(regBC[0],regBC[1])])
            }
            // Increase regDE by 1
            0x13 -> addToRegisters("DE", 1)
            // Increase register D by 1
            0x14 -> regDE[0] = intToBinaryAdditionHex(regDE[0] , 1)
            // TODO: Test
            // Decrease register D by 1
            0x15 -> regDE[0] = intToBinarySubtractionHex(regDE[0], 1)
            // Load next byte to register D
            0x16 -> {
                regDE[0] = memory[regPC].toInt()
                regPC += 0x01
            }
            // Rotate contents of register A to the left through the Carry flag
            0x17 -> {
                rotateBitsThroughCarry('A', "left")
            }
            // TODO: Test
            // Relative jump in Program Counter specified by next byte
            0x18 -> {
                regPC += memory[regPC].toInt()
            }
            // Add contents of registers DE to registers HL
            //0x19 -> {
            //}
            // Load memory[regDE] into Accumulator register
            0x1a -> {
                regAF[0] = memory[bytesToWord(regDE[0], regDE[1])].toUByte().toInt()
            }
            // Decrease registers DE by 1
            0x1b -> {
                addToRegisters("DE", -1)
            }
            // Increase register E by 1
            0x1c -> regDE[1] = intToBinaryAdditionHex(regDE[1] , 1)
            // TODO: Test
            // Decrease register E by 1
            0x1d -> regDE[0] = intToBinarySubtractionHex(regDE[1], 1)
            // Load immediate operand into register E
            0x1e -> {
                regDE[1] = memory[regPC].toUByte().toInt()
                regPC += 0x01
            }
            // Rotate contents of register A to the right through the Carry flag
            0x1f -> {
                rotateBitsThroughCarry('A', "right")
            }

            // Check flag Z, if it equals 0 then add immediate byte to Program Counter, else do nothing
            0x20 -> {
                if(getFlag('Z') == 0) {
                    regPC += memory[regPC].toInt()
                } else {
                    regPC += 0x01
                }
                /*if(getFlag('Z') == 0) {
                    //regPC += 0x01
                    //println("regpc is: 0x" + Integer.toHexString(regPC) + " next byte: 0x" + Integer.toHexString(memory[regPC-1].toUByte().toInt()))
                    regPC = intToBinaryAdditionHexNoFlag(regPC, memory[regPC-0x01].toUByte().toInt())
                    //regPC += (memory[regPC-0x01].toUByte().toInt())
                    //println("regpc is: 0x" + Integer.toHexString(regPC))
                } else {
                    regPC += 0x01
                }*/
            }
            // Load next two bytes into registers HL
            0x21 -> {
                regDE[0] = memory[regPC+1].toUByte().toInt()
                regDE[1] = memory[regPC].toUByte().toInt()
                regPC += 0x02
            }
            // Load next two bytes into registers HL
            //0x21 -> {
            //    regHL[0] = splitToBytes(getNextTwoBytes(regPC), 1).toUByte().toInt()
            //    regHL[1] = splitToBytes(getNextTwoBytes(regPC), 2).toUByte().toInt()
            //    regPC += 0x02
            //}
            // Store Accumulator Register contents into memory[regHL] and add 1 to registers HL
            0x22 -> {
                memory[bytesToWord(regHL[0],regHL[1])] = regAF[0].toUByte().toByte()
                addToRegisters("HL", 1)
                //print("regB: " + Integer.toHexString(regBC[0]) + " regC: " + Integer.toHexString(regBC[1]) + " regBC: " + Integer.toHexString(bytesToWord(regBC[0],regBC[1])))
                //print("regA: " + regAF[0] + " memory[" + Integer.toHexString(bytesToWord(regBC[0],regBC[1])) + "]: " + memory[bytesToWord(regBC[0],regBC[1])])
            }
            // Increase regHL by 1
            0x23 -> addToRegisters("HL", 1)
            // Increase register H by 1
            0x24 -> regHL[0] = intToBinaryAdditionHex(regHL[0] , 1)
            // TODO: Test
            // Decrease register H by 1
            0x25 -> regHL[0] = intToBinarySubtractionHex(regHL[0], 1)
            // Load next byte to register H
            0x26 -> {
                regHL[0] = memory[regPC].toInt()
                regPC += 0x01
            }
            // DAA
            //0x27 ->
            // TODO: Test
            // If flag Z equals 1, jump steps specified by the next byte, if flag Z equals 0, proceed to next instruction normally
            0x28 -> {
                if(getFlag('Z') == 1) {
                    regPC += memory[regPC].toInt()
                } else {
                    regPC += 0x01
                }
            }
            // Check flag Z, if it equals 1 then add immediate byte to Program Counter, else do nothing
            //0x28 -> {
            //    if(getFlag('Z') == 1) {
            //        regPC += 0x01
            //        regPC += (memory[regPC-0x01].toUByte().toInt())
            //    } else {
            //        regPC += 0x01
            //    }
            //}
            // Add contents of registers HL to registers HL
            //0x29 ->
            // Load memory[regHL] into Accumulator register and add 1 to registers HL
            0x2a -> {
                regAF[0] = memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt()
                addToRegisters("HL", 1)
            }
            // Decrease registers HL by 1
            0x2b -> {
                addToRegisters("HL", -1)
            }
            // Increase register L by 1
            0x2c -> regHL[1] = intToBinaryAdditionHex(regHL[1] , 1)
            // TODO: Test
            // Decrease register L by 1
            0x2d -> regHL[1] = intToBinarySubtractionHex(regHL[1], 1)
            // Load immediate operand into register E
            0x2e -> {
                regHL[1] = memory[regPC].toUByte().toInt()
                regPC += 0x01
            }
            // 1's complement of register A
            //0x2f ->

            // Check flag C, if it equals 0 then add immediate byte to Program Counter, else do nothing
            0x30 -> {
                if(getFlag('C') == 0) {
                    regPC += memory[regPC].toInt()
                } else {
                    regPC += 0x01
                }
            }
            // Load next two bytes into Stack Pointer register
            0x31 -> {
                regSP = bytesToWord(memory[regPC+1].toInt(), memory[regPC].toInt())
                regPC += 0x02
            }
            // Store Accumulator Register contents into memory[regHL] and add -1 to registers HL
            0x32 -> {
                memory[bytesToWord(regHL[0],regHL[1])] = regAF[0].toUByte().toByte()
                addToRegisters("HL", -1)
                //print("regB: " + Integer.toHexString(regBC[0]) + " regC: " + Integer.toHexString(regBC[1]) + " regBC: " + Integer.toHexString(bytesToWord(regBC[0],regBC[1])))
                //print("regA: " + regAF[0] + " memory[" + Integer.toHexString(bytesToWord(regBC[0],regBC[1])) + "]: " + memory[bytesToWord(regBC[0],regBC[1])])
            }
            // Increase Stack Pointer by 1
            0x33 -> addToRegisters("SP", 1)
            // TODO: Test
            // Increase memory[regHL] by 1
            0x34 -> {
                val word = bytesToWord(regHL[0],regHL[1])
                memory[word] = intToBinaryAdditionHex(memory[word].toInt(), 1).toUByte().toByte()
            }
            // TODO: Test
            // Decrease memory[regHL] by 1
            0x35 -> {
                val word = bytesToWord(regHL[0],regHL[1])
                memory[word] = intToBinarySubtractionHex(memory[word].toInt(), 1).toUByte().toByte()
            }
            // Load next byte to memory[regHL]
            0x36 -> {
                memory[bytesToWord(regHL[0], regHL[1])] = memory[regPC]
                regPC += 0x01
            }
            // Set flag C
            0x37 -> setFlag('C', 1)
            // TODO: Test
            // If flag C equals 1, jump steps specified by the next byte, if flag C equals 0, proceed to next instruction normally
            0x38 -> {
                if(getFlag('C') == 1) {
                    regPC += memory[regPC].toInt()
                } else {
                    regPC += 0x01
                }
            }
            // Add contents of Stack Pointer register to registers HL
            //0x39 ->
            // Load memory[regHL] into Accumulator register and add -1 to registers HL
            0x3a -> {
                regAF[0] = memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt()
                addToRegisters("HL", -1)
            }
            // Decrease Stack Pointer register by 1
            0x3b -> {
                addToRegisters("SP", -1)
            }
            // TODO: Test
            // Increase Accumulator register by 1
            0x3c -> regAF[0] = intToBinaryAdditionHex(regAF[0] , 1)
            // TODO: Test
            // Decrease Accumulator register by 1
            0x3d -> regAF[0] = intToBinarySubtractionHex(regAF[0], 1)
            // Load immediate operand into Accumulator register
            0x3e -> {
                regAF[0] = memory[regPC].toUByte().toInt()
                regPC += 0x01
            }
            // Flip flag C
            0x3f -> {
                if(getFlag('C') == 0) {
                    setFlag('C', 1)
                } else {
                    setFlag('C', 0)
                }
            }

            // Load register B into register B
            0x40 -> regBC[0] = regBC[0]
            // Load register C into register B
            0x41 -> regBC[0] = regBC[1]
            // Load register D into register B
            0x42 -> regBC[0] = regDE[0]
            // Load register E into register B
            0x43 -> regBC[0] = regDE[1]
            // Load register H into register B
            0x44 -> regBC[0] = regHL[0]
            // Load register L into register B
            0x45 -> regBC[0] = regHL[1]
            // Load memory[regHL] into register B
            0x46 -> regBC[0] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register B
            0x47 -> regBC[0] = regAF[0]
            // Load register B into register C
            0x48 -> regBC[1] = regBC[0]
            // Load register C into register C
            0x49 -> regBC[1] = regBC[1]
            // Load register D into register C
            0x4a -> regBC[1] = regDE[0]
            // Load register E into register C
            0x4b -> regBC[1] = regDE[1]
            // Load register H into register C
            0x4c -> regBC[1] = regHL[0]
            // Load register L into register C
            0x4d -> regBC[1] = regHL[1]
            // Load memory[regHL] into register C
            0x4e -> regBC[1] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register C
            0x4f -> regBC[1] = regAF[0]

            // Load register B into register D
            0x50 -> regDE[0] = regBC[0]
            // Load register C into register D
            0x51 -> regDE[0] = regBC[1]
            // Load register D into register D
            0x52 -> regDE[0] = regDE[0]
            // Load register E into register D
            0x53 -> regDE[0] = regDE[1]
            // Load register H into register D
            0x54 -> regDE[0] = regHL[0]
            // Load register L into register D
            0x55 -> regDE[0] = regHL[1]
            // Load memory[regHL] into register D
            0x56 -> regDE[0] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register D
            0x57 -> regDE[0] = regAF[0]
            // Load register B into register E
            0x58 -> regDE[1] = regBC[0]
            // Load register C into register E
            0x59 -> regDE[1] = regBC[1]
            // Load register D into register E
            0x5a -> regDE[1] = regDE[0]
            // Load register E into register E
            0x5b -> regDE[1] = regDE[1]
            // Load register H into register E
            0x5c -> regDE[1] = regHL[0]
            // Load register L into register E
            0x5d -> regDE[1] = regHL[1]
            // Load memory[regHL] into register E
            0x5e -> regDE[1] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register E
            0x5f -> regDE[1] = regAF[0]

            // Load register B into register H
            0x60 -> regHL[0] = regBC[0]
            // Load register C into register H
            0x61 -> regHL[0] = regBC[1]
            // Load register D into register H
            0x62 -> regHL[0] = regDE[0]
            // Load register E into register H
            0x63 -> regHL[0] = regDE[1]
            // Load register H into register H
            0x64 -> regHL[0] = regHL[0]
            // Load register L into register H
            0x65 -> regHL[0] = regHL[1]
            // Load memory[regHL] into register H
            0x66 -> regHL[0] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register H
            0x67 -> regHL[0] = regAF[0]
            // Load register B into register L
            0x68 -> regHL[1] = regBC[0]
            // Load register C into register L
            0x69 -> regHL[1] = regBC[1]
            // Load register D into register L
            0x6a -> regHL[1] = regDE[0]
            // Load register E into register L
            0x6b -> regHL[1] = regDE[1]
            // Load register H into register L
            0x6c -> regHL[1] = regHL[0]
            // Load register L into register L
            0x6d -> regHL[1] = regHL[1]
            // Load memory[regHL] into register L
            0x6e -> regHL[1] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Register L
            0x6f -> regHL[1] = regAF[0]

            // Load register B into memory[regHL]
            0x70 -> memory[bytesToWord(regHL[0],regHL[1])] = regBC[0].toUByte().toByte()
            // Load register C into memory[regHL]
            0x71 -> memory[bytesToWord(regHL[0],regHL[1])] = regBC[1].toUByte().toByte()
            // Load register D into memory[regHL]
            0x72 -> memory[bytesToWord(regHL[0],regHL[1])] = regDE[0].toUByte().toByte()
            // Load register E into memory[regHL]
            0x73 -> memory[bytesToWord(regHL[0],regHL[1])] = regDE[1].toUByte().toByte()
            // Load register H into memory[regHL]
            0x74 -> memory[bytesToWord(regHL[0],regHL[1])] = regHL[0].toUByte().toByte()
            // Load register L into memory[regHL]
            0x75 -> memory[bytesToWord(regHL[0],regHL[1])] = regHL[1].toUByte().toByte()
            // HALT
            //0x76 ->
            // Load Accumulator Register into memory[regHL]
            0x77 -> memory[bytesToWord(regHL[0],regHL[1])] = regAF[0].toUByte().toByte()
            // Load Register B into Accumulator Register
            0x78 -> regAF[0] = regBC[0]
            // Load Register C into Accumulator Register
            0x79 -> regAF[0] = regBC[1]
            // Load Register D into Accumulator Register
            0x7a -> regAF[0] = regDE[0]
            // Load Register E into Accumulator Register
            0x7b -> regAF[0] = regDE[1]
            // Load Register H into Accumulator Register
            0x7c -> regAF[0] = regHL[0]
            // Load Register L into Accumulator Register
            0x7d -> regAF[0] = regHL[1]
            // Load memory[regHL] into Accumulator Register
            0x7e -> regAF[0] = memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()
            // Load Accumulator Register into Accumulator Register
            0x7f -> regAF[0] = regAF[0]

            // ADD instructions (Without existing carry)
            // A = A+B
            0x80 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regBC[0])
            // A = A+C
            0x81 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regBC[1])
            // A = A+D
            0x82 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regDE[0])
            // A = A+E
            0x83 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regDE[1])
            // A = A+H
            0x84 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regHL[0])
            // A = A+L
            0x85 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regHL[1])
            // A = A+memory[regHL]
            0x86 -> regAF[0] = intToBinaryAdditionHex(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A = A+A
            0x87 -> regAF[0] = intToBinaryAdditionHex(regAF[0], regAF[0])
            // ADD Instructions (With existing carry)
            // A = A+(B+carry)
            0x88 -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regBC[0], getFlag('C')))
            // A = A+(C+carry)
            0x89 -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regBC[1], getFlag('C')))
            // A = A+(D+carry)
            0x8a -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regDE[0], getFlag('C')))
            // A = A+(E+carry)
            0x8b -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regDE[1], getFlag('C')))
            // A = A+(H+carry)
            0x8c -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regHL[0], getFlag('C')))
            // A = A+(L+carry)
            0x8d -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regHL[1], getFlag('C')))
            // A = A+(memory[regHL]+carry)
            0x8e -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt(), getFlag('C')))
            // A = A+(A+carry)
            0x8f -> regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(regAF[0], getFlag('C')))

            // SUB instructions (Without existing carry)
            // A = A-B
            0x90 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regBC[0])
            // A = A-C
            0x91 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regBC[1])
            // A = A-D
            0x92 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regDE[0])
            // A = A-E
            0x93 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regDE[1])
            // A = A-H
            0x94 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regHL[0])
            // A = A-L
            0x95 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regHL[1])
            // A = A-memory[regHL]
            0x96 -> regAF[0] = intToBinarySubtractionHex(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A = A-A
            0x97 -> regAF[0] = intToBinarySubtractionHex(regAF[0], regAF[0])
            // SUB Instructions (With existing carry)
            // A = A-(B+carry)
            0x98 -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regBC[0], getFlag('C')))
            // A = A-(C+carry)
            0x99 -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regBC[1], getFlag('C')))
            // A = A-(D+carry)
            0x9a -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regDE[0], getFlag('C')))
            // A = A-(E+carry)
            0x9b -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regDE[1], getFlag('C')))
            // A = A-(H+carry)
            0x9c -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regHL[0], getFlag('C')))
            // A = A-(L+carry)
            0x9d -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regHL[1], getFlag('C')))
            // A = A-(memory[regHL]+carry)
            0x9e -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt(), getFlag('C')))
            // A = A-(A+carry)
            0x9f -> regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(regAF[0], getFlag('C')))

            // AND Instructions
            // A = A AND B
            0xa0 -> regAF[0] = andCalculation(regAF[0], regBC[0])
            // A = A AND C
            0xa1 -> regAF[0] = andCalculation(regAF[0], regBC[1])
            // A = A AND D
            0xa2 -> regAF[0] = andCalculation(regAF[0], regDE[0])
            // A = A AND E
            0xa3 -> regAF[0] = andCalculation(regAF[0], regDE[1])
            // A = A AND H
            0xa4 -> regAF[0] = andCalculation(regAF[0], regHL[0])
            // A = A AND L
            0xa5 -> regAF[0] = andCalculation(regAF[0], regHL[1])
            // A = A AND memory[regHL]
            0xa6 -> regAF[0] = andCalculation(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A = A AND A
            0xa7 -> regAF[0] = andCalculation(regAF[0], regAF[0])
            // XOR Instructions
            // A = A XOR B
            0xa8 -> regAF[0] = xorCalculation(regAF[0], regBC[0])
            // A = A XOR C
            0xa9 -> regAF[0] = xorCalculation(regAF[0], regBC[1])
            // A = A XOR D
            0xaa -> regAF[0] = xorCalculation(regAF[0], regDE[0])
            // A = A XOR E
            0xab -> regAF[0] = xorCalculation(regAF[0], regDE[1])
            // A = A XOR H
            0xac -> regAF[0] = xorCalculation(regAF[0], regHL[0])
            // A = A XOR L
            0xad -> regAF[0] = xorCalculation(regAF[0], regHL[1])
            // A = A XOR memory[regHL]
            0xae -> regAF[0] = xorCalculation(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A = A XOR A
            0xaf -> regAF[0] = xorCalculation(regAF[0], regAF[0])

            // OR Instructions
            // A = A OR B
            0xb0 -> regAF[0] = orCalculation(regAF[0], regBC[0])
            // A = A OR C
            0xb1 -> regAF[0] = orCalculation(regAF[0], regBC[1])
            // A = A OR D
            0xb2 -> regAF[0] = orCalculation(regAF[0], regDE[0])
            // A = A OR E
            0xb3 -> regAF[0] = orCalculation(regAF[0], regDE[1])
            // A = A OR H
            0xb4 -> regAF[0] = orCalculation(regAF[0], regHL[0])
            // A = A OR L
            0xb5 -> regAF[0] = orCalculation(regAF[0], regHL[1])
            // A = A OR memory[regHL]
            0xb6 -> regAF[0] = orCalculation(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A = A OR A
            0xb7 -> regAF[0] = orCalculation(regAF[0], regAF[0])
            // Compare Instructions (Subtract two integers and set flags)
            // A-B
            0xb8 -> intToBinarySubtractionHex(regAF[0], regBC[0])
            // A-C
            0xb9 -> intToBinarySubtractionHex(regAF[0], regBC[1])
            // A-D
            0xba -> intToBinarySubtractionHex(regAF[0], regDE[0])
            // A-E
            0xbb -> intToBinarySubtractionHex(regAF[0], regDE[1])
            // A-H
            0xbc -> intToBinarySubtractionHex(regAF[0], regHL[0])
            // A-L
            0xbd -> intToBinarySubtractionHex(regAF[0], regHL[1])
            // A-memory[regHL]
            0xbe -> intToBinarySubtractionHex(regAF[0], memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt())
            // A-A
            0xbf -> intToBinarySubtractionHex(regAF[0], regAF[0])

            // Check flag Z, if it equals 0 then pop Program Counter from stack
            0xc0 -> {
                if(getFlag('Z') == 0) {
                    regPC = getNextTwoBytes(regSP)
                    memory[regSP] = 0
                    regSP += 0x01
                    memory[regSP] = 0
                    regSP += 0x01
                }
            }
            // Pop from stack into registers BC
            0xc1 -> {
                regBC[1] = memory[regSP].toUByte().toInt()
                memory[regSP] = 0
                regSP += 0x01
                regBC[0] = memory[regSP].toUByte().toInt()
                regSP += 0x01
            }
            // If flag Z is 0, load next two bytes into Program Counter
            0xc2 -> {
                if(getFlag('Z') == 0) {
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // Load next two bytes to Program Counter
            0xc3 -> regPC = getNextTwoBytes(regPC)
            // TODO: Check if correct
            // If flag Z is 0, push Program Counter to stack and load next two bytes from memory into Program Counter
            0xc4 -> {
                if(getFlag('Z') == 0) {
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 1)
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 2)
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // Push registers BC to stack
            0xc5 -> {
                regSP -= 0x01
                memory[regSP] = regBC[0].toUByte().toByte()
                regSP -= 0x01
                memory[regSP] = regBC[1].toUByte().toByte()
            }
            // A = A + next byte
            0xc6 -> {
                regAF[0] = intToBinaryAdditionHex(regAF[0], memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // RST 0 - Push Program Counter to stack and set Program Counter to 0x0000
            0xc7 -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0000
            }
            // TODO: Check if 0xc8 or 0xc9 implementation is better for popping
            // If flag Z is 1, pop Program Counter from stack
            0xc8 -> {
                if(getFlag('Z') == 1) {
                    regPC = bytesToWord(memory[regSP+1].toUByte().toInt(), memory[regSP].toUByte().toInt())
                    memory[regSP] = 0
                    regSP += 0x01
                    memory[regSP] = 0
                    regSP += 0x01
                }
            }
            // Return - Pop stored Program Counter value from stack back into Program Counter to return from subroutine
            0xc9 -> {
                regPC = getNextTwoBytes(regSP)
                memory[regSP] = 0
                regSP += 0x01
                memory[regSP] = 0
                regSP += 0x01
            }
            // If flag Z is 1, load next two bytes into Program Counter
            0xca -> {
                if(getFlag('Z') == 1) {
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // CB instructions
            0xcb -> {
                cbInstructions(memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // If flag Z is 1, push Program Counter to stack and load next two bytes in memory[regPC] to Program Counter
            0xcc -> {
                if(getFlag('Z') == 1) {
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 1)
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 2)
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // Call - Push Program Counter to Stack, set new Stack Pointer and load next two bytes to Program Counter to call a subroutine
            0xcd -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = getNextTwoBytes(regPC)
                /*
                regPC += 0x02
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = getNextTwoBytes(regPC-0x02)
            */
            }
            // A = A + (next byte + carry)
            0xce -> {
                regAF[0] = intToBinaryAdditionHex(regAF[0], intToBinaryAdditionHex(memory[regPC].toUByte().toInt(), getFlag('C')))
                regPC += 0x01
            }
            // RST 1 - Push Program Counter to stack and set Program Counter to 0x0008
            0xcf -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0008
            }

            // Check flag C, if it equals 0 then pop Program Counter from stack
            0xd0 -> {
                if(getFlag('C') == 0) {
                    regPC = getNextTwoBytes(regSP)
                    memory[regSP] = 0
                    regSP += 0x01
                    memory[regSP] = 0
                    regSP += 0x01
                }
            }
            // Pop from stack into registers DE
            0xd1 -> {
                regDE[1] = memory[regSP].toUByte().toInt()
                memory[regSP] = 0
                regSP += 0x01
                regDE[0] = memory[regSP].toUByte().toInt()
                regSP += 0x01
            }
            // If flag C is 0, load next two bytes into Program Counter
            0xd2 -> {
                if(getFlag('C') == 0) {
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // TODO: Check if correct
            // If flag C is 0, push Program Counter to stack and load next two bytes from memory into Program Counter
            0xd4 -> {
                if(getFlag('C') == 0) {
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 1)
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 2)
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // TODO: Check if stack pointer is correct
            // Push registers DE into stack
            0xd5 -> {
                regSP -= 0x01
                memory[regSP] = regDE[0].toUByte().toByte()
                regSP -= 0x01
                memory[regSP] = regDE[1].toUByte().toByte()
            }
            // A = A - next byte
            0xd6 -> {
                regAF[0] = intToBinarySubtractionHex(regAF[0], memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // RST 2 - Push Program Counter to stack and set Program Counter to 0x0010
            0xd7 -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0010
            }
            // TODO: Check if 0xc8 or 0xc9 implementation is better for popping
            // If flag C is 1, pop Program Counter from stack
            0xd8 -> {
                if(getFlag('C') == 1) {
                    regPC = bytesToWord(memory[regSP+1].toUByte().toInt(), memory[regSP].toUByte().toInt())
                    memory[regSP] = 0
                    regSP += 0x01
                    memory[regSP] = 0
                    regSP += 0x01
                }
            }
            // TODO: Check Interrupt Flag
            // Return Interrupt - Pop stored Program Counter value from stack back into Program Counter to return from subroutine
            0xd9 -> {
                regPC = getNextTwoBytes(regSP)
                memory[regSP] = 0
                regSP += 0x01
                memory[regSP] = 0
                regSP += 0x01
                imeFlag = 0
            }
            // If flag C is 1, load next two bytes into Program Counter
            0xda -> {
                if(getFlag('C') == 1) {
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // If flag C is 1, push Program Counter to stack and load next two bytes in memory[regPC] to Program Counter
            0xdc -> {
                if(getFlag('C') == 1) {
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 1)
                    regSP -= 0x01
                    memory[regSP] = splitToBytes(regPC, 2)
                    regPC = getNextTwoBytes(regPC)
                } else {
                    regPC += 0x02
                }
            }
            // A = A - (next byte + carry)
            0xde -> {
                regAF[0] = intToBinarySubtractionHex(regAF[0], intToBinaryAdditionHex(memory[regPC].toUByte().toInt(), getFlag('C')))
                regPC += 0x01
            }
            // RST 3 - Push Program Counter to stack and set Program Counter to 0x0018
            0xdf -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0018
            }

            // Store Accumulator Register value in memory address (0xff00+immediate byte)
            0xe0 -> {
                memory[0xff00+memory[regPC]] = regAF[0].toByte()
                regPC += 0x01
            }
            // Pop stack into registers HL
            0xe1 -> {
                regHL[0] = memory[regSP+0x01].toUByte().toInt()
                regHL[1] = memory[regSP].toUByte().toInt()
                regSP += 0x02
            }
            // Store Accumulator Register value in memory address (0xff00+register C)
            0xe2 -> memory[0xff00+regBC[1]] = regAF[0].toByte()
            // Push registers HL into stack
            0xe5 -> {
                memory[regSP-0x01] = regHL[0].toUByte().toByte()
                memory[regSP-0x02] = regHL[1].toUByte().toByte()
                regSP -= 0x02
            }
            // Store into Accumulator Register the results of (Accumulator Register AND immediate byte)
            0xe6 -> {
                regAF[0] = andCalculation(regAF[0], memory[regPC].toUByte().toInt())
                regPC += 0x01
                /*
                regAF[0] = (regAF[0] and memory[regPC].toUByte().toInt())
                regPC += 0x01
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 1)
                setFlag('C', 0)
                */
            }
            // RST 4 - Push Program Counter to stack and set Program Counter to 0x0020
            0xe7 -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0020
            }
            // regSP = next byte (signed) + regSP
            //0xe8 ->
            // Load registers HL to Program Counter
            0xe9 -> regPC = bytesToWord(regHL[0], regHL[1])
            // Store Accumulator Register values to memory address (or register) pointed by next two bytes
            0xea -> {
                memory[getNextTwoBytes(regPC)] = regAF[0].toByte()
                regPC += 0x02
            }
            // A = A XOR next byte
            0xee -> {
                regAF[0] = xorCalculation(regAF[0], memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // RST 5 - Push Program Counter to stack and set Program Counter to 0x0028
            0xef -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0028
            }

            // Load into Accumulator Registor the contents of memory address (0xff00+immediate byte)
            0xf0 -> {
                print("regAF[0]=" + regAF[0])
                regAF[0] = memory[0xff00+memory[regPC]].toUByte().toInt()
                print("regAF[0]=" + regAF[0] + " memory[6c]: " + Integer.toHexString(memory[regPC].toUByte().toInt()) + " 0xff00+" + Integer.toHexString(memory[regPC].toUByte().toInt()) + ": " + Integer.toHexString(memory[0xff00+memory[regPC]].toUByte().toInt()))
                regPC += 0x01
            }
            // Pop stack into registers AF
            0xf1 -> {
                regAF[0] = memory[regSP+0x01].toUByte().toInt()
                regAF[1] = memory[regSP].toUByte().toInt()
                regSP += 0x02
            }
            // Load into Accumulator register memory[0xff00+register C]
            0xf2 -> {
                regAF[0] = memory[0xff00+regBC[1]].toUByte().toInt()
            }
            // Reset Interrupt Master Enable flag
            0xf3 -> imeFlag = 0
            // Push Accumulator Register and Flag Register to memory, using Stack Pointer
            0xf5 -> {
                regSP -= 0x01
                memory[regSP] = regAF[0].toUByte().toByte()
                regSP -= 0x01
                memory[regSP] = regAF[1].toUByte().toByte()
            }
            // A = A OR next byte
            0xf6 -> {
                regAF[0] = orCalculation(regAF[0], memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // RST 6 - Push Program Counter to stack and set Program Counter to 0x0030
            0xf7 -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0030
            }
            // HL = next byte(signed) + regSP
            //0xf8 ->
            // Load registers HL into regSP
            0xf9 -> regSP = bytesToWord(regHL[0], regHL[1])
            // Load into Accumulator Register the contents of memory[two immediate bytes]
            0xfa -> {
                regAF[0] = memory[getNextTwoBytes(regPC)].toUByte().toInt()
                print("loading memory[" + Integer.toHexString(getNextTwoBytes(regPC)) + "]=0x" + Integer.toHexString(memory[getNextTwoBytes(regPC)].toUByte().toInt()) + "\n")
                regPC += 0x02
            }
            // TODO: Check Interrupts
            // Set Interrupt Master Enable flag and enable interrupts
            0xfb -> imeFlag = 1
            //0xfc ->
            //0xfd ->
            // TODO: FLAGS
            // If the (Accumulator Register - immediate byte) == 0, set flag Z to true
            0xfe -> {
                intToBinarySubtractionHex(regAF[0], memory[regPC].toInt())
                //performCalculation(regAF[0], memory[regPC].toInt(), "SUB")
                //if(regAF[0] == memory[regPC].toInt()) setFlag('Z', 1)
                //val lol = performCalculation(regAF[0], memory[regPC].toUByte().toInt(), "ADD")
                regPC += 0x01
            }
            // RST 7 - Push Program Counter to stack and set Program Counter to 0x0038
            0xff -> {
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = 0x0038
            }

            else -> {
                printRegisterStatus()
                haltflag = 1
            }
        }
    }
    // CB Instructions
    fun cbInstructions(operand: Int) {
        when(operand) {
            // Rotate registers to the left
            0x00 -> {
                rotateBits('B', "left")
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x01 -> {
                rotateBits('C', "left")
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x02 -> {
                rotateBits('D', "left")
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x03 -> {
                rotateBits('E', "left")
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x04 -> {
                rotateBits('H', "left")
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x05 -> {
                rotateBits('L', "left")
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x06 -> {
                rotateBits('M', "left")
                if(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                }
            }
            0x07 -> {
                rotateBits('A', "left")
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            // Rotate registers to the right
            0x08 -> {
                rotateBits('B', "right")
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x09 -> {
                rotateBits('C', "right")
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0a -> {
                rotateBits('D', "right")
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0b -> {
                rotateBits('E', "right")
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0c -> {
                rotateBits('H', "right")
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0d -> {
                rotateBits('L', "right")
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0e -> {
                rotateBits('M', "right")
                if(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                }
            }
            0x0f -> {
                rotateBits('A', "right")
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            // Rotate registers to the left through Carry flag
            0x10 -> {
                rotateBitsThroughCarry('B', "left")
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x11 -> {
                rotateBitsThroughCarry('C', "left")
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x12 -> {
                rotateBitsThroughCarry('D', "left")
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x13 -> {
                rotateBitsThroughCarry('E', "left")
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x14 -> {
                rotateBitsThroughCarry('H', "left")
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x15 -> {
                rotateBitsThroughCarry('L', "left")
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x16 -> {
                rotateBitsThroughCarry('M', "left")
                if(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                }
            }
            0x17 -> {
                rotateBitsThroughCarry('A', "left")
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            // Rotate registers to the right through Carry flag
            0x18 -> {
                rotateBitsThroughCarry('B', "right")
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x19 -> {
                rotateBitsThroughCarry('C', "right")
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1a -> {
                rotateBitsThroughCarry('D', "right")
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1b -> {
                rotateBitsThroughCarry('E', "right")
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1c -> {
                rotateBitsThroughCarry('H', "right")
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1d -> {
                rotateBitsThroughCarry('L', "right")
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1e -> {
                rotateBitsThroughCarry('M', "right")
                if(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                }
            }
            0x1f -> {
                rotateBitsThroughCarry('A', "right")
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                }
            }

            // Shift bits of registers to the left through Carry flag
            0x20 -> shiftBits('B', "left")
            0x21 -> shiftBits('C', "left")
            0x22 -> shiftBits('D', "left")
            0x23 -> shiftBits('E', "left")
            0x24 -> shiftBits('H', "left")
            0x25 -> shiftBits('L', "left")
            0x26 -> shiftBits('M', "left")
            0x27 -> shiftBits('A', "left")
            // Shift bits of registers to the right through Carry flag, first bit is unchanged
            0x28 -> shiftBits('B', "right")
            0x29 -> shiftBits('C', "right")
            0x2a -> shiftBits('D', "right")
            0x2b -> shiftBits('E', "right")
            0x2c -> shiftBits('H', "right")
            0x2d -> shiftBits('L', "right")
            0x2e -> shiftBits('M', "right")
            0x2f -> shiftBits('A', "right")

            // Swap bits 0-3 with bits 4-7
            0x30 -> swapBits('B')
            0x31 -> swapBits('C')
            0x32 -> swapBits('D')
            0x33 -> swapBits('E')
            0x34 -> swapBits('H')
            0x35 -> swapBits('L')
            0x36 -> swapBits('M')
            0x37 -> swapBits('A')
            // Shift bits of registers to the right through Carry flag, first bit changes to 0
            0x38 -> shiftBitsRightWithZero('B')
            0x39 -> shiftBitsRightWithZero('C')
            0x3a -> shiftBitsRightWithZero('D')
            0x3b -> shiftBitsRightWithZero('E')
            0x3c -> shiftBitsRightWithZero('H')
            0x3d -> shiftBitsRightWithZero('L')
            0x3e -> shiftBitsRightWithZero('M')
            0x3f -> shiftBitsRightWithZero('A')

            // Reset specified bit to 0 in specified register
            0x80 -> setBit(false, 7, 'B')
            0x81 -> setBit(false, 7, 'C')
            0x82 -> setBit(false, 7, 'D')
            0x83 -> setBit(false, 7, 'E')
            0x84 -> setBit(false, 7, 'H')
            0x85 -> setBit(false, 7, 'L')
            0x86 -> setBit(false, 7, 'M')
            0x87 -> setBit(false, 6, 'A')
            0x88 -> setBit(false, 6, 'B')
            0x89 -> setBit(false, 6, 'C')
            0x8a -> setBit(false, 6, 'D')
            0x8b -> setBit(false, 6, 'E')
            0x8c -> setBit(false, 6, 'H')
            0x8d -> setBit(false, 6, 'L')
            0x8e -> setBit(false, 6, 'M')
            0x8f -> setBit(false, 6, 'A')

            0x90 -> setBit(false, 5, 'B')
            0x91 -> setBit(false, 5, 'C')
            0x92 -> setBit(false, 5, 'D')
            0x93 -> setBit(false, 5, 'E')
            0x94 -> setBit(false, 5, 'H')
            0x95 -> setBit(false, 5, 'L')
            0x96 -> setBit(false, 5, 'M')
            0x97 -> setBit(false, 5, 'A')
            0x98 -> setBit(false, 4, 'B')
            0x99 -> setBit(false, 4, 'C')
            0x9a -> setBit(false, 4, 'D')
            0x9b -> setBit(false, 4, 'E')
            0x9c -> setBit(false, 4, 'H')
            0x9d -> setBit(false, 4, 'L')
            0x9e -> setBit(false, 4, 'M')
            0x9f -> setBit(false, 4, 'A')

            0xa0 -> setBit(false, 3, 'B')
            0xa1 -> setBit(false, 3, 'C')
            0xa2 -> setBit(false, 3, 'D')
            0xa3 -> setBit(false, 3, 'E')
            0xa4 -> setBit(false, 3, 'H')
            0xa5 -> setBit(false, 3, 'L')
            0xa6 -> setBit(false, 3, 'M')
            0xa7 -> setBit(false, 3, 'A')
            0xa8 -> setBit(false, 2, 'B')
            0xa9 -> setBit(false, 2, 'C')
            0xaa -> setBit(false, 2, 'D')
            0xab -> setBit(false, 2, 'E')
            0xac -> setBit(false, 2, 'H')
            0xad -> setBit(false, 2, 'L')
            0xae -> setBit(false, 2, 'M')
            0xaf -> setBit(false, 2, 'A')

            0xb0 -> setBit(false, 1, 'B')
            0xb1 -> setBit(false, 1, 'C')
            0xb2 -> setBit(false, 1, 'D')
            0xb3 -> setBit(false, 1, 'E')
            0xb4 -> setBit(false, 1, 'H')
            0xb5 -> setBit(false, 1, 'L')
            0xb6 -> setBit(false, 1, 'M')
            0xb7 -> setBit(false, 1, 'A')
            0xb8 -> setBit(false, 0, 'B')
            0xb9 -> setBit(false, 0, 'C')
            0xba -> setBit(false, 0, 'D')
            0xbb -> setBit(false, 0, 'E')
            0xbc -> setBit(false, 0, 'H')
            0xbd -> setBit(false, 0, 'L')
            0xbe -> setBit(false, 0, 'M')
            0xbf -> setBit(false, 0, 'A')

            // Set specified bit to 1 in specified register
            0xc0 -> setBit(true, 7, 'B')
            0xc1 -> setBit(true, 7, 'C')
            0xc2 -> setBit(true, 7, 'D')
            0xc3 -> setBit(true, 7, 'E')
            0xc4 -> setBit(true, 7, 'H')
            0xc5 -> setBit(true, 7, 'L')
            0xc6 -> setBit(true, 7, 'M')
            0xc7 -> setBit(true, 6, 'A')
            0xc8 -> setBit(true, 6, 'B')
            0xc9 -> setBit(true, 6, 'C')
            0xca -> setBit(true, 6, 'D')
            0xcb -> setBit(true, 6, 'E')
            0xcc -> setBit(true, 6, 'H')
            0xcd -> setBit(true, 6, 'L')
            0xce -> setBit(true, 6, 'M')
            0xcf -> setBit(true, 6, 'A')

            0xd0 -> setBit(true, 5, 'B')
            0xd1 -> setBit(true, 5, 'C')
            0xd2 -> setBit(true, 5, 'D')
            0xd3 -> setBit(true, 5, 'E')
            0xd4 -> setBit(true, 5, 'H')
            0xd5 -> setBit(true, 5, 'L')
            0xd6 -> setBit(true, 5, 'M')
            0xd7 -> setBit(true, 5, 'A')
            0xd8 -> setBit(true, 4, 'B')
            0xd9 -> setBit(true, 4, 'C')
            0xda -> setBit(true, 4, 'D')
            0xdb -> setBit(true, 4, 'E')
            0xdc -> setBit(true, 4, 'H')
            0xdd -> setBit(true, 4, 'L')
            0xde -> setBit(true, 4, 'M')
            0xdf -> setBit(true, 4, 'A')

            0xe0 -> setBit(true, 3, 'B')
            0xe1 -> setBit(true, 3, 'C')
            0xe2 -> setBit(true, 3, 'D')
            0xe3 -> setBit(true, 3, 'E')
            0xe4 -> setBit(true, 3, 'H')
            0xe5 -> setBit(true, 3, 'L')
            0xe6 -> setBit(true, 3, 'M')
            0xe7 -> setBit(true, 3, 'A')
            0xe8 -> setBit(true, 2, 'B')
            0xe9 -> setBit(true, 2, 'C')
            0xea -> setBit(true, 2, 'D')
            0xeb -> setBit(true, 2, 'E')
            0xec -> setBit(true, 2, 'H')
            0xed -> setBit(true, 2, 'L')
            0xee -> setBit(true, 2, 'M')
            0xef -> setBit(true, 2, 'A')

            0xf0 -> setBit(true, 1, 'B')
            0xf1 -> setBit(true, 1, 'C')
            0xf2 -> setBit(true, 1, 'D')
            0xf3 -> setBit(true, 1, 'E')
            0xf4 -> setBit(true, 1, 'H')
            0xf5 -> setBit(true, 1, 'L')
            0xf6 -> setBit(true, 1, 'M')
            0xf7 -> setBit(true, 1, 'A')
            0xf8 -> setBit(true, 0, 'B')
            0xf9 -> setBit(true, 0, 'C')
            0xfa -> setBit(true, 0, 'D')
            0xfb -> setBit(true, 0, 'E')
            0xfc -> setBit(true, 0, 'H')
            0xfd -> setBit(true, 0, 'L')
            0xfe -> setBit(true, 0, 'M')
            0xff -> setBit(true, 0, 'A')

            else -> {
                print("CB instruction 0x" + Integer.toHexString(operand.toUByte().toInt()) + " not implemented\n")
            }
        }
    }
    // Load ROM into memory
    fun loadRom(rompath: String) {
        val file = File(rompath)
        memory = file.readBytes()
    }
    // Check if given operation between two given operands produces a Half Carry Flag
    /*fun checkHalfCarry(op1: Int, op2: Int, operation: String) : Boolean {
        when(operation) {
            "ADD","add","+" -> {

            }
            "SUB","sub","-" -> {

            }
            "LD","ld","LOAD","load" -> {

            }
        }
    } */
    // Set the Flag register according to flag name and bit
    fun setFlag(flagName: Char, flagBit: Int) {
        if (flagBit != 0 && flagBit != 1) {
            print("Flag bit input: " + flagBit + "\nFlag must contain value of either '1' or '0'.\n")
            print("Flag was not set.\n")
            return
        }
        // Convert integer in Flag Register to binary string
        var register = Integer.toBinaryString(regAF[1]).padStart(8, '0')
        // Convert boolean input to char
        var bit : Char
        if (flagBit == 1) {
            bit = '1'
        } else {
            bit = '0'
        }
        // Set which index in binary string to change according to input flag name
        val index : Int
        when (flagName) {
            'Z','z' -> index = 0
            'N','n' -> index = 1
            'H','h' -> index = 2
            'C','c' -> index = 3
            else -> {
                print("Wrong flag name input. Flag names are Z,N,H,C.\n")
                return
            }
        }
        // Change selected bit and set Flag Register to new value
        register = register.substring(0, index) + bit + register.substring(index+1)
        regAF[1] = Integer.parseInt(register, 2)
        return
    }
    // Check input flag and return its value
    fun getFlag(flagName: Char) : Int {
        when (flagName) {
            'Z', 'z' -> {
                val flag = Integer.toBinaryString(regAF[1]).padStart(8, '0').get(0)
                if (flag.equals('1')) {
                    return 1
                } else if (flag.equals('0')) {
                    return 0
                }
            }
            'N', 'n' -> {
                val flag = Integer.toBinaryString(regAF[1]).padStart(8, '0').get(1)
                if (flag.equals('1')) {
                    return 1
                } else if (flag.equals('0')) {
                    return 0
                }
            }
            'H', 'h' -> {
                val flag = Integer.toBinaryString(regAF[1]).padStart(8, '0').get(2)
                if (flag.equals('1')) {
                    return 1
                } else if (flag.equals('0')) {
                    return 0
                }
            }
            'C', 'c' -> {
                val flag = Integer.toBinaryString(regAF[1]).padStart(8, '0').get(3)
                if (flag.equals('1')) {
                    return 1
                } else if (flag.equals('0')) {
                    return 0
                }
            }
        }
        print("Wrong flag name given. Flags are Z,N,H,C.\n")
        return 0
    }
    // Little Endian - Get byte contents from next two memory cells and return them as an integer (Converting to unsigned byte first)
    fun getNextTwoBytes(index: Int) : Int {
        var op1 = memory[index+1].toUByte() * 0x100u
        var op2 = memory[index].toUByte()
        var result = (op1.plus(op2)).toInt()
        return result
    }
    // Concatenate two bytes to form a word and return as integer
    fun bytesToWord(highByte: Int, lowByte: Int) : Int {
        var op1 = highByte.toUByte() * 0x100u
        var op2 = lowByte.toUByte()
        var result = (op1.plus(op2)).toInt()
        return result
    }
    /*
    // Big Endian -  Get byte contents from next two memory cells and return them as an integer (Converting to unsigned byte first)
    fun getNextTwoBytesBigEndian(index: Int) : Int {
        var op1 = memory[index].toUByte() * 0x100u
        var op2 = memory[index+1].toUByte()
        var result = (op1.plus(op2)).toInt()
        return result
    }
    */
    // Split word into two bytes and select which one to return
    fun splitToBytes(word: Int, selection: Int) : Byte {
        var op1 : Byte
        var op2 : Byte
        op1 = word.shr(8).toByte()
        op2 = word.toByte()
        when(selection) {
            1 -> return op1
            2 -> return op2
            else -> {
                print("Wrong selection. Enter 1 or 2.\n")
                return op1
            }
        }
    }
    // Return an Integer array containing the 16 bits of two given operands
    // Operand 1 is placed into positions Array[0] to Array[7] and operand 2 into Array[8] to Array[15]
    fun convertToBits(op1: Int, op2:Int) : IntArray {
        // Convert operands to binary format xxxxxxxx
        val binary1 = String.format("%"+8+"s", Integer.toBinaryString(op1)).replace(" ".toRegex(), "0")
        val binary2 = String.format("%"+8+"s", Integer.toBinaryString(op2)).replace(" ".toRegex(), "0")
        val array = binary1 + binary2
        // Split string into 16 bits and convert to Integer array
        return array.chunked(1).map{it.toInt()}.toIntArray()
    }
    // Converts a binary number (in IntArray form) to integer
    fun binaryToInteger(binary: IntArray) : Int {
        var sum = 0
        var counter = 0
        // Calculate decimal from powers of 2
        for (i in (0..binary.lastIndex).reversed()) {
            sum += binary[i] * 2.toDouble().pow(counter).toInt()
            counter += 1
        }
        // Return result
        return sum
    }
    // TODO: Implement overflow
    // Converts two integers to binary and adds them
    // Returns array containing halfcarry, carry and binary addition result
    fun intToBinaryAddition(num1: Int, num2: Int) : IntArray {
        var inputarray = convertToBits(num1, num2)
        // resultarray[0] contains halfcarry, resultarray[1] contains carry and addition result is in positions resultarray[2] to resultarray[9]
        var resultarray = intArrayOf(0,0,0,0,0,0,0,0,0,0)
        var carry = 0
        var halfcarry = 0
        var counter = 9
        for (i in 7 downTo 0) {
            // Result = Op2 + (Op1 + Carry)
            resultarray[counter] = inputarray[i+8] + (inputarray[i] + carry)
            //print(resultarray[counter].toString() + "=" + inputarray[i+8].toString() + "+(" + inputarray[i].toString() + "+" + carry.toString() + ")\n")
            // If addition is 1+1 then result=0 and carry=1
            if (resultarray[counter] == 2) {
                resultarray[counter] = 0
                carry = 1
            } else if (resultarray[counter] == 3) {
                resultarray[counter] = 1
                carry = 1
            } else {
                carry = 0
            }
            // Check for halfcarry between bits 4 and 5
            if (i == 4) {
                if (carry == 1) {
                    halfcarry = 1
                } else {
                    halfcarry = 0
                }
            }
            counter -= 1
        }
        resultarray[1] = carry
        resultarray[0] = halfcarry

        return resultarray
    }
    // Converts two integers to binary and adds them with existing carry
    // Returns array containing halfcarry, carry and binary addition result
    fun intToBinaryAdditionWithCarry(num1: Int, num2: Int) : IntArray {
        var inputarray = convertToBits(num1, num2)
        // resultarray[0] contains halfcarry, resultarray[1] contains carry and addition result is in positions resultarray[2] to resultarray[9]
        var resultarray = intArrayOf(0,0,0,0,0,0,0,0,0,0)
        var carry = 1
        var halfcarry = 0
        var counter = 9
        for (i in 7 downTo 0) {
            // Result = Op2 + (Op1 + Carry)
            resultarray[counter] = inputarray[i+8] + (inputarray[i] + carry)
            //print(resultarray[counter].toString() + "=" + inputarray[i+8].toString() + "+(" + inputarray[i].toString() + "+" + carry.toString() + ")\n")
            // If addition is 1+1 then result=0 and carry=1
            if (resultarray[counter] == 2) {
                resultarray[counter] = 0
                carry = 1
            } else if (resultarray[counter] == 3) {
                resultarray[counter] = 1
                carry = 1
            } else {
                carry = 0
            }
            // Check for halfcarry between bits 4 and 5
            if (i == 4) {
                if (carry == 1) {
                    halfcarry = 1
                } else {
                    halfcarry = 0
                }
            }
            counter -= 1
        }
        resultarray[1] = carry
        resultarray[0] = halfcarry

        return resultarray
    }
    // Performs addition of two numbers and returns result
    fun intToBinaryAdditionHex(num1: Int, num2: Int) : Int {
        var result = num1 + num2
        print("Num 1 is: " + Integer.toHexString(num1) + " Num2 is: " + Integer.toHexString(num2) + " result: " + Integer.toHexString(result))
        if(result>255) {
            result = result - 256
            setFlag('C' , 1)
        } else {
            setFlag('C' , 0)
        }
        if (result == 0) {
            setFlag('Z', 1)
        } else {
            setFlag('Z', 0)
        }
        setFlag('N', 0)
        if ((num1.and(0xf)+(num2.and(0xf))) > 0xf) {
            setFlag('H', 1)
        } else {
            setFlag('H', 0)
        }
        //print("Carry: " + getFlag('C') + "HalfCarry: " + getFlag('H'))
        //readLine()
        return result
    }
    // Performs addition of two numbers and returns result without changing flags
    fun intToBinaryAdditionHexNoFlag(num1: Int, num2: Int) : Int {
        var result = num1 + num2
        print("Num 1 is: " + Integer.toHexString(num1) + " Num2 is: " + Integer.toHexString(num2) + " result: " + Integer.toHexString(result))
        if(result>255) {
            result = result - 256
            //setFlag('C' , 1)
        } else {
            //setFlag('C' , 0)
        }
        if (result == 0) {
            //setFlag('Z', 1)
        } else {
            //setFlag('Z', 0)
        }
        //setFlag('N', 0)
        if ((num1.and(0xf)+(num2.and(0xf))) > 0xf) {
            //setFlag('H', 1)
        } else {
            //setFlag('H', 0)
        }
        print("Carry: " + getFlag('C') + "HalfCarry: " + getFlag('H'))
        readLine()
        return result
    }
    fun intToBinarySubtractionHex(num1: Int, num2: Int) : Int {
        var result = num1 - num2
        //print("Num 1 is: " + Integer.toHexString(num1) + " Num2 is: " + Integer.toHexString(num2))
        if(result<0) {
            result = result + 256
            setFlag('C' , 1)
        } else {
            setFlag('C' , 0)
        }
        //print("result: " + Integer.toHexString(result))
        if (result == 0) {
            setFlag('Z', 1)
        } else {
            setFlag('Z', 0)
        }
        setFlag('N', 1)
        if ((num1.and(0xf)+(num2.and(0xf))) > 0xf) {
            setFlag('H', 1)
        } else {
            setFlag('H', 0)
        }
        //print("Carry: " + getFlag('C') + "HalfCarry: " + getFlag('H'))
        //readLine()
        return result
    }
    // Performs AND calculation between two integers, and sets flags
    fun andCalculation(num1: Int, num2: Int) : Int {
        val result = num1.and(num2)
        if(result == 0) {
            setFlag('Z', 1)
        } else {
            setFlag('Z', 0)
        }
        setFlag('N', 0)
        setFlag('H', 1)
        setFlag('C', 0)
        return result
    }
    // Performs XOR calculation between two integers, and sets flags
    fun xorCalculation(num1: Int, num2: Int) : Int {
        val result = num1.xor(num2)
        if(result == 0) {
            setFlag('Z', 1)
        } else {
            setFlag('Z', 0)
        }
        setFlag('N', 0)
        setFlag('H', 0)
        setFlag('C', 0)
        return result
    }
    // Performs OR calculation between two integers, and sets flags
    fun orCalculation(num1: Int, num2: Int) : Int {
        val result = num1.or(num2)
        if(result == 0) {
            setFlag('Z', 1)
        } else {
            setFlag('Z', 0)
        }
        setFlag('N', 0)
        setFlag('H', 0)
        setFlag('C', 0)
        return result
    }
    // Converts two integers to binary and subtracts them
    // Returns array containing halfcarry, carry and binary subtraction result
    fun intToBinarySubtraction(num1: Int, num2: Int) : IntArray {
        // Convert operands to bits
        var subtractionarray = convertToBits(num1, num2)
        // resultarray[0] contains halfcarry, resultarray[1] contains carry and addition result is in positions resultarray[2] to resultarray[9]
        var resultarray = intArrayOf(0,0,0,0,0,0,0,0,0,0)
        var carry = 0
        var halfcarry = 0
        // Flag to check if underflow happened (Underflow = 1, no underflow = 0)
        var underflow = 0
        // Counter for resultarray index which is different size than subtractionarray
        var counter = 9
        // Subtract the two binary numbers contained in subtractionarray
        for (i in 7 downTo 0) {
            // Perform subtraction and place result into resultarray
            resultarray[counter] = subtractionarray[i] - subtractionarray[i+8]
            // If a carry is necessary, search higher order digits for '1'
            if (resultarray[counter] == -1) {
                // If underflow has already occurred, change subtraction result to '0'
                if (underflow == 1) {
                    resultarray[counter] = 0
                } else {
                    // If underflow hasn't occurred, find higher order '1', change it to '0'
                    // Then, change minuend digits between current position and higher order '1' from '0' to '1'
                    for (j in (i-1) downTo 0) {
                        // If '1' is found in a minuend position, change it to '0'
                        if (subtractionarray[j] == 1) {
                            subtractionarray[j] = 0
                            // Change all digits between current position and starting position to '1'
                            for (k in (j+1) until (i)) {
                                subtractionarray[k] = 1
                            }
                            carry = 1
                            if ((carry == 1) && (i == 4)) {
                                halfcarry = 1
                            }
                            break
                        }
                    }
                    // If '1' isn't found, set underflow flag
                    if (carry == 0) {
                        underflow = 1
                    }
                    // Reset carry
                    carry = 0
                    // Change result
                    resultarray[counter] = 1
                }
            }
            counter -= 1
        }
        resultarray[1] = carry
        resultarray[0] = halfcarry

        return resultarray
    }
    // TODO: Complete subtraction with carry/Add case where result is -2 in the first subtraction due to carry
    // Converts two integers to binary and subtracts them with existing carry (Num1 - Num2 - carry)
    // Returns array containing halfcarry, carry and binary subtraction result
    fun intToBinarySubtractionWithCarry(num1: Int, num2: Int) : IntArray {
        // Convert operands to bits
        var subtractionarray = convertToBits(num1, num2)
        // resultarray[0] contains halfcarry, resultarray[1] contains carry and addition result is in positions resultarray[2] to resultarray[9]
        var resultarray = intArrayOf(0,0,0,0,0,0,0,0,0,0)
        var carry = 1
        var halfcarry = 0
        // Flag to check if underflow happened (Underflow = 1, no underflow = 0)
        var underflow = 0
        // Counter for resultarray index which is different size than subtractionarray
        var counter = 9
        // Subtract the two binary numbers contained in subtractionarray
        for (i in 7 downTo 0) {
            // Subtract existing carry in first subtraction
            if(i != 7) {
                // Perform subtraction and place result into resultarray
                resultarray[counter] = subtractionarray[i] - subtractionarray[i+8]
            } else {
                resultarray[counter] = subtractionarray[i] - subtractionarray[i+8] - carry
            }
            // If a carry is necessary, search higher order digits for '1'
            if (resultarray[counter] == -1) {
                // If underflow has already occurred, change subtraction result to '0'
                if (underflow == 1) {
                    resultarray[counter] = 0
                } else {
                    // If underflow hasn't occurred, find higher order '1', change it to '0'
                    // Then, change minuend digits between current position and higher order '1' from '0' to '1'
                    for (j in (i-1) downTo 0) {
                        // If '1' is found in a minuend position, change it to '0'
                        if (subtractionarray[j] == 1) {
                            subtractionarray[j] = 0
                            // Change all digits between current position and starting position to '1'
                            for (k in (j+1) until (i)) {
                                subtractionarray[k] = 1
                            }
                            carry = 1
                            if ((carry == 1) && (i == 4)) {
                                halfcarry = 1
                            }
                            break
                        }
                    }
                    // If '1' isn't found, set underflow flag
                    if (carry == 0) {
                        underflow = 1
                    }
                    // Reset carry
                    carry = 0
                    // Change result
                    resultarray[counter] = 1
                }
            }
            counter -= 1
        }
        resultarray[1] = carry
        resultarray[0] = halfcarry

        return resultarray
    }
    // Perform given operation (add, sub, ld) and update flags
    fun performCalculation(op1: Int, op2: Int, operation: String) : IntArray {
        var resultarray = intArrayOf(0,0,0,0,0,0,0,0)
        // Get current flags
        var carry = getFlag('C')
        var halfcarry = getFlag('H')
        var zero = getFlag('Z')
        // Perform input operation
        when(operation) {
            "ADD","add","+" -> {
                // Perform addition
                var additionarray = intToBinaryAddition(op1, op2)
                // Save carry and halfcarry flags from resultarray
                carry = additionarray[1]
                halfcarry = additionarray[0]
                // Copy result to new array
                resultarray = additionarray.copyOfRange(2, additionarray.size)
                // Replace carry and halfcarry in resultarray with 0 to prepare for zero flag calculation
                additionarray[0] = 0
                additionarray[1] = 0
                // Calculate resultarray sum for zero flag
                if (resultarray.sum() == 0) {
                    zero = 1
                } else {
                    zero = 0
                }
                // Update flags
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', carry)
                setFlag('H', halfcarry)

                //return resultarray
                /*
                carry = 0
                for (i in 7 downTo 0) {
                    // Result = Op2 + (Op1 + Carry)
                    resultarray[i] = inputarray[i+8] + (inputarray[i] + carry)
                    if (resultarray[i] == 2) {
                        resultarray[i] = 0
                        carry = 1
                    } else {
                        carry = 0
                    }
                    // Check for half carry (Half carry can occur when going from bit[4] to bit[3], counting down from bit[7])
                    if (i == 4) {
                        if (carry == 1) {
                            halfcarry == 1
                        } else {
                            halfcarry == 0
                        }
                    }
                }
                // Check for zero flag (Result == 0)
                if (resultarray.sum() == 0) {
                    zero = 1
                } else {
                    zero = 0
                }
                // Update flags
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', carry)
                setFlag('H', halfcarry)
                 */

            }
            "ADC", "adc" -> {
                var additionarray : IntArray
                // Check for existing carry and perform addition
                if(getFlag('C') == 1) {
                    additionarray = intToBinaryAdditionWithCarry(op1, op2)
                } else {
                    additionarray = intToBinaryAddition(op1, op2)
                }
                val carry = additionarray[1]
                val halfcarry = additionarray[0]
                // Copy result to new array
                resultarray = additionarray.copyOfRange(2, additionarray.size)
                // Calculate resultarray sum for zero flag
                if (resultarray.sum() == 0) {
                    zero = 1
                } else {
                    zero = 0
                }
                // Update flags
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', carry)
                setFlag('H', halfcarry)
            }
            "SUB","sub","-" -> {
                // Convert operands to bits
                // var subtractionarray = intToBinarySubtraction(op1, op2) //convertToBits(op1, op2)
                var result = intToBinarySubtractionHex(op1, op2) //convertToBits(op1, op2)
                //return result
                //val carry = subtractionarray[1]
                //val halfcarry = subtractionarray[0]
                //resultarray = subtractionarray.copyOfRange(2, subtractionarray.size)
                // Calculate resultarray sum for zero flag
                //if (resultarray.sum() == 0) {
                //    zero = 1
                //} else {
                //    zero = 0
                //}
                // Update flags
                //setFlag('Z', zero)
                //setFlag('N', 1)
                //setFlag('H', halfcarry)
                //setFlag('C', carry)
            /*
                // Flag to check if underflow happened (Underflow = 1, no underflow = 0)
                var underflow = 0
                // Counter for resultarray index which is different size than subtractionarray
                var counter = 7
                // Subtract the two binary numbers contained in subtractionarray
                for (i in 7 downTo 0) {
                    // Perform subtraction and place result into resultarray
                    resultarray[counter] = subtractionarray[i] - subtractionarray[i+8]
                    // If a carry is necessary, search higher order digits for '1'
                    if (resultarray[counter] == -1) {
                        // If underflow has already occurred, change subtraction result to '0'
                        if (underflow == 1) {
                            resultarray[counter] = 0
                        } else {
                            // If underflow hasn't occurred, find higher order '1', change it to '0'
                            // Then, change minuend digits between current position and higher order '1' from '0' to '1'
                            for (j in (i-1) downTo 0) {
                                // If '1' is found in a minuend position, change it to '0'
                                if (subtractionarray[j] == 1) {
                                    subtractionarray[j] = 0
                                    // Change all digits between current position and starting position to '1'
                                    for (k in (j+1) until (i)) {
                                        subtractionarray[k] = 1
                                    }
                                    carry = 1
                                    if ((carry == 1) && (i == 4)) {
                                        halfcarry = 1
                                    }
                                    break
                                }
                            }
                            // If '1' isn't found, set underflow flag
                            if (carry == 0) {
                                underflow = 1
                            }
                            // Reset carry
                            carry = 0
                            // Change result
                            resultarray[counter] = 1
                        }
                    }
                    counter -= 1
                }*/
            }
            "SBC", "sbc" -> {
                //TODO: Implement subtraction with carry
                var subtractionarray : IntArray
                // Check for existing carry and perform addition
                if(getFlag('C') == 1) {
                    subtractionarray = intToBinarySubtractionWithCarry(op1, op2)
                } else {
                    subtractionarray = intToBinarySubtraction(op1, op2)
                }
                val carry = subtractionarray[1]
                val halfcarry = subtractionarray[0]
                resultarray = subtractionarray.copyOfRange(2, subtractionarray.size)
                // Calculate resultarray sum for zero flag
                if (resultarray.sum() == 0) {
                    zero = 1
                } else {
                    zero = 0
                }
                // Update flags
                setFlag('Z', zero)
                setFlag('N', 1)
                setFlag('C', carry)
                setFlag('H', halfcarry)
            }
            "LD","ld","LOAD","load" -> {

            }
            "AND", "and" -> {
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', 1)
                setFlag('H', 0)
            }
            "OR", "or" -> {
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', 0)
                setFlag('H', 0)
            }
            "XOR", "xor" -> {
                setFlag('Z', zero)
                setFlag('N', 0)
                setFlag('C', 0)
                setFlag('H', 0)
            }
            "INC", "inc", "increase" -> {

            }
            "DEC", "dec", "decrease" -> {

            }
            "CP", "cp", "copy" -> {

            }
            "CPL", "cpl" -> {

            }
            "CPF", "cpf" -> {

            }
            "DAA", "daa" -> {

            }
            "SCF", "scf" -> {

            }

        }
        return resultarray
    }
    // Add input value to selected register (AF, BC, DE, HL)
    fun addToRegisters(registers: String, value: Int) {
        // Join both register bytes to a word and add given value. Then split word into two bytes to add back to the registers.
        when(registers) {
            "AF", "af" -> {
                regAF[0] = splitToBytes(bytesToWord(regAF[0], regAF[1]) + value, 1).toUByte().toInt()
                regAF[1] = splitToBytes(bytesToWord(regAF[0], regAF[1]) + value, 2).toUByte().toInt()
                return
            }
            "BC", "bc" -> {
                regBC[0] = splitToBytes(bytesToWord(regBC[0], regBC[1]) + value, 1).toUByte().toInt()
                regBC[1] = splitToBytes(bytesToWord(regBC[0], regBC[1]) + value, 2).toUByte().toInt()
                return
            }
            "DE", "de" -> {
                regDE[0] = splitToBytes(bytesToWord(regDE[0], regDE[1]) + value, 1).toUByte().toInt()
                regDE[1] = splitToBytes(bytesToWord(regDE[0], regDE[1]) + value, 2).toUByte().toInt()
                return
            }
            "HL", "hl" -> {
                regHL[0] = splitToBytes(bytesToWord(regHL[0], regHL[1]) + value, 1).toUByte().toInt()
                regHL[1] = splitToBytes(bytesToWord(regHL[0], regHL[1]) + value, 2).toUByte().toInt()
                return
            }
            "SP", "sp" -> {
                regSP = regSP + value
                return
            }
        }
    }
    // Set a bit - Inputs are value (0 or 1), index (0-7) and register (A-F or M for memory[regHL])
    fun setBit(bitValue: Boolean, bitIndex: Int, register: Char) {
        // Convert boolean input to 0 and 1
        var bit: Int
        if(bitValue) {
            bit = 1
        } else {
            bit = 0
        }
        // Place input bit at selected index of selected register
        when(register) {
            'A', 'a' -> {
                var register = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regAF[0] = Integer.parseInt(register, 2)
            }
            'B', 'b' -> {
                var register = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regBC[0] = Integer.parseInt(register, 2)
            }
            'C', 'c' -> {
                var register = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regBC[1] = Integer.parseInt(register, 2)
            }
            'D', 'd' -> {
                var register = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regDE[0] = Integer.parseInt(register, 2)
            }
            'E', 'e' -> {
                var register = Integer.toBinaryString(regDE[1]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regDE[1] = Integer.parseInt(register, 2)
            }
            'H', 'h' -> {
                var register = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regHL[0] = Integer.parseInt(register, 2)
            }
            'L', 'l' -> {
                var register = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                regAF[1] = Integer.parseInt(register, 2)
            }
            'M', 'm' -> {
                var register = Integer.toBinaryString(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()).padStart(8, '0')
                register = register.substring(0, bitIndex) + bit + register.substring(bitIndex + 1)
                memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(register, 2).toUByte().toByte()
            }
        }
    }
    // Rotates bits of selected register to selected direction
    // Carry flag gets set with first bit if rotating to the left, and last bit if rotating to the right
    fun rotateBits(registerName: Char, direction: String) {
        when (registerName) {
            'A', 'a' -> {
                val torotate = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regAF[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regAF[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'B', 'b' -> {
                val torotate = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regBC[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regBC[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'C', 'c' -> {
                val torotate = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regBC[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regBC[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'D', 'd' -> {
                val torotate = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regDE[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regDE[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'E', 'e' -> {
                val torotate = Integer.toBinaryString(regDE[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regDE[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regDE[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'H', 'h' -> {
                val torotate = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regHL[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regHL[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'L', 'l' -> {
                val torotate = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        regHL[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regHL[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'M', 'm' -> {
                val torotate = Integer.toBinaryString(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(torotate.substring(0, 1))
                        memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(rotated, 2).toUByte().toByte()
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(torotate.substring(6, 7))
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(rotated, 2).toUByte().toByte()
                    }
                }
            }
        }
    }
    // Rotates bits of selected register to selected direction through the Carry flag
    // Carry flag gets set with first bit if rotating to the left, and last bit if rotating to the right
    // Last bit gets set with Carry flag if rotating to the left, and first bit is set with Carry flag if rotating to the right
    fun rotateBitsThroughCarry(registerName: Char, direction: String) {
        val carry = getFlag('C')
        when (registerName) {
            'A', 'a' -> {
                val torotate = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regAF[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regAF[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'B', 'b' -> {
                val torotate = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regBC[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regBC[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'C', 'c' -> {
                val torotate = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regBC[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regBC[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'D', 'd' -> {
                val torotate = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regDE[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regDE[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'E', 'e' -> {
                val torotate = Integer.toBinaryString(regDE[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regDE[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regDE[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'H', 'h' -> {
                val torotate = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regHL[0] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regHL[0] = Integer.parseInt(rotated, 2)
                    }
                }
            }
            'L', 'l' -> {
                val torotate = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(0, 1).toInt())
                        var rotated = ""
                        for (i in 1..7) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        rotated = rotated.plus(carry)
                        regHL[1] = Integer.parseInt(rotated, 2)
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', torotate.substring(6, 7).toInt())
                        var rotated = ""
                        rotated = rotated.plus(carry)
                        for (i in 0..6) {
                            rotated = rotated.plus(torotate.substring(i, i + 1))
                        }
                        regHL[1] = Integer.parseInt(rotated, 2)
                    }
                }
            }
        }
    }
    // Shifts bits of selected register to selected direction
    // Carry flag gets set with first bit if rotating to the left, and last bit if rotating to the right
    // First bit is unchanged if shifting to the right, last bit is 0 if shifting to the left
    fun shiftBits(registerName: Char, direction: String) {
        when (registerName) {
            'A', 'a' -> {
                val toshift = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regAF[0] = Integer.parseInt(shifted, 2)
                        if(regAF[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regAF[0] = Integer.parseInt(shifted, 2)
                        if(regAF[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'B', 'b' -> {
                val toshift = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regBC[0] = Integer.parseInt(shifted, 2)
                        if(regBC[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regBC[0] = Integer.parseInt(shifted, 2)
                        if(regBC[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'C', 'c' -> {
                val toshift = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regBC[1] = Integer.parseInt(shifted, 2)
                        if(regBC[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regBC[1] = Integer.parseInt(shifted, 2)
                        if(regBC[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'D', 'd' -> {
                val toshift = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regDE[0] = Integer.parseInt(shifted, 2)
                        if(regDE[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regDE[0] = Integer.parseInt(shifted, 2)
                        if(regDE[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'E', 'e' -> {
                val toshift = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regDE[1] = Integer.parseInt(shifted, 2)
                        if(regDE[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regDE[1] = Integer.parseInt(shifted, 2)
                        if(regDE[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'H', 'h' -> {
                val toshift = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regHL[0] = Integer.parseInt(shifted, 2)
                        if(regHL[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regHL[0] = Integer.parseInt(shifted, 2)
                        if(regHL[0] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'L', 'l' -> {
                val toshift = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        regHL[1] = Integer.parseInt(shifted, 2)
                        if(regHL[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        regHL[1] = Integer.parseInt(shifted, 2)
                        if(regHL[1] == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
            'M', 'm' -> {
                val toshift = Integer.toBinaryString(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()).padStart(8, '0')
                when (direction) {
                    "left" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(0, 1).toInt())
                        var shifted = ""
                        for (i in 1..7) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        shifted = shifted.plus("0")
                        memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(shifted, 2).toUByte().toByte()
                        if(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt() == 0) {
                            setFlag('Z', 1)
                        }
                    }
                    "right" -> {
                        setFlag('Z', 0)
                        setFlag('N', 0)
                        setFlag('H', 0)
                        setFlag('C', toshift.substring(6, 7).toInt())
                        var shifted = ""
                        shifted = shifted.plus(toshift.substring(0, 1))
                        for (i in 0..6) {
                            shifted = shifted.plus(toshift.substring(i, i + 1))
                        }
                        memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(shifted, 2).toUByte().toByte()
                        if(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt() == 0) {
                            setFlag('Z', 1)
                        }
                    }
                }
            }
        }
    }
    // Shifts bits of selected register to the right
    // Carry flag gets set with last bit
    // First bit is set to 0
    fun shiftBitsRightWithZero(registerName: Char) {
        when (registerName) {
            'A', 'a' -> {
                val toshift = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regAF[0] = Integer.parseInt(shifted, 2)
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            'B', 'b' -> {
                val toshift = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regBC[0] = Integer.parseInt(shifted, 2)
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            'C', 'c' -> {
                val toshift = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regBC[1] = Integer.parseInt(shifted, 2)
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            'D', 'd' -> {
                val toshift = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regDE[0] = Integer.parseInt(shifted, 2)
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            'E', 'e' -> {
                val toshift = Integer.toBinaryString(regDE[1]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regDE[1] = Integer.parseInt(shifted, 2)
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            'H', 'h' -> {
                val toshift = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regHL[0] = Integer.parseInt(shifted, 2)
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                }
            }
            'L', 'l' -> {
                val toshift = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                regHL[1] = Integer.parseInt(shifted, 2)
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                }
            }
            'M', 'm' -> {
                val toshift = Integer.toBinaryString(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt()).padStart(8, '0')
                setFlag('Z', 0)
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', toshift.substring(6, 7).toInt())
                var shifted = ""
                shifted = shifted.plus("0")
                for (i in 0..6) {
                    shifted = shifted.plus(toshift.substring(i, i + 1))
                }
                memory[bytesToWord(regHL[0],regHL[1])] = Integer.parseInt(shifted, 2).toUByte().toByte()
                if(memory[bytesToWord(regHL[0],regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                }
            }
        }
    }
    // Swap bits 0-3 with bits 4-7 in selected register
    fun swapBits(register: Char) {
        when(register) {
            'A','a' -> {
                if(regAF[0] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regAF[0]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regAF[0] = Integer.parseInt(swapped, 2)
            }
            'B','b' -> {
                if(regBC[0] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regBC[0]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regBC[0] = Integer.parseInt(swapped, 2)
            }
            'C','c' -> {
                if(regBC[1] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regBC[1]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regBC[1] = Integer.parseInt(swapped, 2)
            }
            'D','d' -> {
                if(regDE[0] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regDE[0]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regDE[0] = Integer.parseInt(swapped, 2)
            }
            'E','e' -> {
                if(regDE[1] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regDE[1]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regDE[1] = Integer.parseInt(swapped, 2)
            }
            'H','h' -> {
                if(regHL[0] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regHL[0]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regHL[0] = Integer.parseInt(swapped, 2)
            }
            'L','l' -> {
                if(regHL[1] == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(regHL[1]).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                regHL[1] = Integer.parseInt(swapped, 2)
            }
            'M','m' -> {
                if(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt() == 0) {
                    setFlag('Z', 1)
                } else {
                    setFlag('Z', 0)
                }
                setFlag('N', 0)
                setFlag('H', 0)
                setFlag('C', 0)
                var swapped = ""
                val preswap = Integer.toBinaryString(memory[bytesToWord(regHL[0], regHL[1])].toUByte().toInt()).padStart(8, '0')
                swapped = swapped.plus(preswap.substring(4,8))
                swapped = swapped.plus(preswap.substring(0,4))
                memory[bytesToWord(regHL[0], regHL[1])] = Integer.parseInt(swapped, 2).toUByte().toByte()
            }
        }
    }
    // Print memory contents to csv file for debugging
    fun printMemoryToFile() {
        print("Writing to file...\n")
        var progress = 0
        val file = File("app/src/main/assets/memorylog.csv")
        val start = Instant.now()
        var end = Instant.now()
        var flag = 0
        FileOutputStream(file).use {
            for (i in memory.indices) {
                if (i % 52428 == 0 && i != 0) {
                    if (flag == 0) {
                        end = Instant.now()
                        flag = 1
                    }
                    progress += 5
                    val duration = Duration.between(start, end)
                    val remaining =
                        duration.multipliedBy(((100 - progress) / 5).toLong()).toSeconds()
                    print(progress)
                    print("% done (")
                    print(remaining)
                    print(" seconds remaining)\n")
                }
                if (i % 30 == 0) {
                    file.appendText("\n")
                }
                file.appendText("0x" + Integer.toHexString(i).padStart(5, '0'))
                file.appendText("=>0x")
                file.appendText(Integer.toHexString(memory[i].toUByte().toInt()).padStart(2, '0'))
                file.appendText(",")
            }
        }
    }
    // Print memory contents to console for debugging
    fun printMemoryToConsole() {
        for ((index, i) in memory.withIndex()) {
            if (index % 20 == 0) {
                println()
            }
            print("\t\t\t")
            print(Integer.toHexString(index))
            print("=>")
            print(Integer.toHexString(i.toUByte().toInt())) // Convert Byte -> uByte -> Integer -> Hex String for print() to print unsigned bytes
        }
    }
    // Print opcodes to txt file for debugging
    fun printOpcodesToFile() {
        val file = File("app/src/main/assets/opcodes.txt")
        file.appendText("PC\t=> Opcode\n")
        try {
            while (true) {
                file.appendText("0x" + Integer.toHexString(this.regPC).padStart(5, '0'))
                file.appendText("\t=> ")
                this.fetch()
                file.appendText("0x" + this.opcode.toUByte().toString(16).padStart(2, '0') + "\n"
                )
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            print("Reached the end of file\n")
        }
    }/*
    fun printOutputToFile() {

    }*/
    // Print status of registers for debugging
    fun printRegisterStatus() {
        print("-------------------------------------\n")
        print("|\t\tREGISTERS\t\t|\tFLAGS\t|\n")
        print("-------------------------------------\n")
        print("|\tA: 0x" + Integer.toHexString(regAF[0]).padStart(2, '0') + "\tF: 0x" + Integer.toHexString(regAF[1]).padStart(2, '0') + "\t\t|\t Z: " + getFlag('Z') + "\t|\n")
        print("|\tB: 0x" + Integer.toHexString(regBC[0]).padStart(2, '0') + "\tC: 0x" + Integer.toHexString(regBC[1]).padStart(2, '0') + "\t\t|\t N: " + getFlag('N') + "\t|\n")
        print("|\tD: 0x" + Integer.toHexString(regDE[0]).padStart(2, '0') + "\tE: 0x" + Integer.toHexString(regDE[1]).padStart(2, '0') + "\t\t|\t H: " + getFlag('H') + "\t|\n")
        print("|\tH: 0x" + Integer.toHexString(regHL[0]).padStart(2, '0') + "\tL: 0x" + Integer.toHexString(regHL[1]).padStart(2, '0') + "\t\t|\t C: " + getFlag('C') + "\t|\n")
        print("-------------------------------------\n")
        print(" Stack Pointer is: 0x" + Integer.toHexString(regSP) + "\n")
        print(" Program Counter is: 0x" + Integer.toHexString(regPC) + "\n")
        print(" Instruction 0x" + Integer.toHexString(opcode.toUByte().toInt()).padStart(2, '0') + " at memory address 0x" + Integer.toHexString(regPC-1).padStart(5, '0') + " not implemented\n")
        print("-------------------------------------------------------------\n")
    }
}
