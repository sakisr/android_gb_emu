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
        print("Running opcode: 0x" + Integer.toHexString(memory[regPC].toUByte().toInt()) + " at memory address: 0x" + Integer.toHexString(regPC) + " | regA is: 0x" + Integer.toHexString(regAF[0]) + "\n")
        opcode = (memory[regPC].toInt())
        regPC += 0x01
    }
    // TODO: Implement flags
    // Decode current instruction (opcode)
    fun decode() {
        when(opcode.toUByte().toInt()) {
            // nn -> immediate data values (8-bit/16-bit)
            // (nn) -> immediate address values (8-bit/16-bit)

            0x00 -> return
            // Load next two bytes into registers BC
            0x01 -> {
                regBC[0] = memory[regPC+1].toUByte().toInt()
                regBC[1] = memory[regPC].toUByte().toInt()
                regPC += 0x02
            }
            // Load next byte to register B
            0x06 -> {
                regBC[0] = memory[regPC].toInt()
                regPC += 0x01
            }
            // Rotate contents of register A to the left
            //0x07 -> {
            //    rotateBits('A', "left")
            //}
            // Decrease registers BC by 1
            0x0b -> {
                addToRegisters("BC", -1)
            }
            // Load next two bytes into registers DE
            0x11 -> {
                regDE[0] = memory[regPC+1].toUByte().toInt()
                regDE[1] = memory[regPC].toUByte().toInt()
                regPC += 0x02
            }
            // Increase Program Counter by immediate byte value
            0x18 -> regPC += memory[regPC].toInt()
            // Check flag Z, if it equals 0 then add immediate byte to Program Counter, else do nothing
            0x20 -> {
                if(getFlag('Z') == 0) {
                    regPC += 0x01
                    regPC += (memory[regPC-0x01].toUByte().toInt())
                } else {
                    regPC += 0x01
                }
            }
            // Load next two bytes into registers HL
            0x21 -> {
                regHL[0] = splitToBytes(getNextTwoBytes(regPC), 1).toUByte().toInt()
                regHL[1] = splitToBytes(getNextTwoBytes(regPC), 2).toUByte().toInt()
                regPC += 0x02
            }
            // Increase registers HL by 1
            0x23 -> {
                addToRegisters("HL", 1)
            }
            // Check flag Z, if it equals 1 then add immediate byte to Program Counter, else do nothing
            0x28 -> {
                if(getFlag('Z') == 1) {
                    regPC += 0x01
                    regPC += (memory[regPC-0x01].toUByte().toInt())
                } else {
                    regPC += 0x01
                }
            }
            // Load next two bytes into Stack Pointer
            0x31 -> {
                regSP = getNextTwoBytes(regPC)
                regPC += 0x02
            }
            // Load immediate byte into memory address stored in registers HL
            0x36 -> {
                memory[bytesToWord(regHL[0], regHL[1])] = memory[regPC]
                regPC += 0x01
            }
            // Load contents of Register B into Accumulator Register
            0x78 -> regAF[0] = regBC[0]
            // Load immediate byte into Accumulator Register
            0x3e -> {
                regAF[0] = memory[regPC].toUByte().toInt()
                regPC += 0x01
            }
            // Load contents of Accumulator Register into Register B
            0x47 -> regBC[0] = regAF[0]
            // TODO: FLAGS
            // Accumulator Register = Accumulator Register AND Accumulator Register
            0xa7 -> regAF[0] = regAF[0].and(regAF[0])
            // TODO: FLAGS
            // Accumulator Register = Accumulator Register XOR Accumulator Register (Resets Accumulator Register)
            0xaf -> regAF[0] = regAF[0].xor(regAF[0])
            // TODO: FLAGS
            // Accumulator Register = Register C OR Accumulator Register
            0xb1 -> regAF[0] = regBC[1].or(regAF[0])
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
            // Load next two bytes to Program Counter
            0xc3 -> regPC = getNextTwoBytes(regPC)
            // Return - Pop stored Program Counter value from stack back into Program Counter to return from subroutine
            0xc9 -> {
                regPC = getNextTwoBytes(regSP)
                memory[regSP] = 0
                regSP += 0x01
                memory[regSP] = 0
                regSP += 0x01
            }
            // CB instructions
            0xcb -> {
                cbInstructions(memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // Call - Push Program Counter to Stack, set new Stack Pointer and load next two bytes to Program Counter to call a subroutine
            0xcd -> {
                regPC += 0x02
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 1)
                regSP -= 0x01
                memory[regSP] = splitToBytes(regPC, 2)
                regPC = getNextTwoBytes(regPC-0x02)
            }
            // Push registers DE into stack
            0xd5 -> {
                regSP -= 0x01
                memory[regSP] = regDE[0].toUByte().toByte()
                regSP -= 0x01
                memory[regSP] = regDE[1].toUByte().toByte()
            }
            // Store Accumulator Register value in memory address (0xff00+immediate byte)
            0xe0 ->{
                memory[0xff00+memory[regPC]] = regAF[0].toByte()
                regPC += 0x01
            }
            // TODO: FLAGS
            // Store into Accumulator Register the results of (Accumulator Register AND immediate byte)
            0xe6 -> {
                regAF[0] = (regAF[0] and memory[regPC].toUByte().toInt())
                regPC += 0x01
            }
            // Store Accumulator Register values to memory address (or register) pointed by next two bytes
            0xea -> {
                memory[getNextTwoBytes(regPC)] = regAF[0].toByte()
                regPC += 0x02
            }
            // Load into Accumulator Registor the contents of memory address (0xff00+immediate byte)
            0xf0 -> {
                regAF[0] = memory[0xff00+memory[regPC]].toUByte().toInt()
                regPC += 0x01
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
            // Load into Accumulator Register the contents of memory[two immediate bytes]
            0xfa -> {
                regAF[0] = memory[getNextTwoBytes(regPC)].toUByte().toInt()
                print("loading memory[" + Integer.toHexString(getNextTwoBytes(regPC)) + "]=0x" + Integer.toHexString(memory[getNextTwoBytes(regPC)].toUByte().toInt()) + "\n")
                regPC += 0x02
            }
            // TODO: FLAGS
            // If the (Accumulator Register - immediate byte) == 0, set flag Z to true
            0xfe -> {
                if(regAF[0] == memory[regPC].toInt()) setFlag('Z', 1)
                val lol = performCalculation(regAF[0], memory[regPC].toUByte().toInt(), "ADD")
                regPC += 0x01
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
            // Set register A bit 0 to 0
            0x87 -> {
                setBit(false, 7, 'A')
            }
            // Set register A bit 6 to 0
            0xb7 -> {
                setBit(false, 1, 'A')
            }
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
                if (additionarray.sum() == 0) {
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
            // TODO: Complete subtraction
            "SUB","sub","-" -> {
                // Convert operands to bits
                var subtractionarray = convertToBits(op1, op2)
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
                }
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
        }
    }
    // Set a bit - Inputs are value (0 or 1), index (0-7) and register (A-F)
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
        }
    }
    // TODO
    // Rotates bits of selected register to selected direction
    fun rotateBits(registerName: Char, direction: String) {
        when(registerName) {
            'A','a' -> {
                when(direction) {
                    "left" -> {
                        regAF[0] = 0xe4
                        regAF[0] = regAF[0].shl(1)
                        print("regA is: " + Integer.toHexString(regAF[0]) + "\n")
                    }
                    //"right" -> {

                    //}
                }
            }
            //'B','b' -> {

            //}
            //'C','c' -> {

            //}
            //'D','d' -> {

            //}
            //'E','e' -> {

            //}
            //'H','h' -> {

            //}
            //'L','l' -> {

            //}
            //"right" ->
            else -> {
                print("Wrong direction. Enter left or right.\n")
                return
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
    }
    // Print status of registers for debugging
    fun printRegisterStatus() {
        print("-------------------------\n")
        print("|\t\tREGISTERS\t\t|\n")
        print("-------------------------\n")
        print("|\tA: 0x" + Integer.toHexString(regAF[0]).padStart(2, '0') + "\tF: 0x" + Integer.toHexString(regAF[1]).padStart(2, '0') + "\t\t|\n")
        print("|\tB: 0x" + Integer.toHexString(regBC[0]).padStart(2, '0') + "\tC: 0x" + Integer.toHexString(regBC[1]).padStart(2, '0') + "\t\t|\n")
        print("|\tD: 0x" + Integer.toHexString(regDE[0]).padStart(2, '0') + "\tE: 0x" + Integer.toHexString(regDE[1]).padStart(2, '0') + "\t\t|\n")
        print("|\tH: 0x" + Integer.toHexString(regHL[0]).padStart(2, '0') + "\tL: 0x" + Integer.toHexString(regHL[1]).padStart(2, '0') + "\t\t|\n")
        print("-------------------------\n")
        print(" Stack Pointer is: 0x" + Integer.toHexString(regSP) + "\n")
        print(" Program Counter is: 0x" + Integer.toHexString(regPC) + "\n")
        print(" Instruction 0x" + Integer.toHexString(opcode.toUByte().toInt()).padStart(2, '0') + " at memory address 0x" + Integer.toHexString(regPC-1).padStart(5, '0') + " not implemented\n")
        print("-------------------------------------------------------------\n")
    }
}
