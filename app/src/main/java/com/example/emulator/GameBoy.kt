package com.example.emulator
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant

// Gameboy starting address is 0x0100
var START_ADDRESS = 0x0100
/*
var FONTSET_START_ADDRESS = 0x50
var FONTSET_SIZE = 80
var fontset = arrayOf(
    0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
    0x20, 0x60, 0x20, 0x20, 0x70, // 1
    0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
    0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
    0x90, 0x90, 0xF0, 0x10, 0x10, // 4
    0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
    0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
    0xF0, 0x10, 0x20, 0x40, 0x40, // 7
    0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
    0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
    0xF0, 0x90, 0xF0, 0x90, 0x90, // A
    0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
    0xF0, 0x80, 0x80, 0x80, 0xF0, // C
    0xE0, 0x90, 0x90, 0x90, 0xE0, // D
    0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
    0xF0, 0x80, 0xF0, 0x80, 0x80  // F
)
 */
class GameBoy {
    // 64 KiB of memory
    var memory = ByteArray(65536) {0}
    // 6 Registers - Can be used as two 8 bit registers or 1 16 bit register
    // Accumulator & Flags Register (AF Register)
    // The Flags Register is the second byte of the AF Register
    // | Bit  | Flag
    // | 7    | Zero Flag (when result is zero)
    // | 6    | Subtraction Flag (BCD)
    // | 5    | Half Carry Flag (BCD)
    // | 4    | Carry Flag
    var regAF = Array(8) {0}
    var regBC = Array(2) {0}   // BC Register
    var regDE = Array(2) {0}   // DE Register
    var regHL = Array(2) {0}   // HL Register
    var regSP = Array(2) {0}   // Stack Pointer
    var regPC = START_ADDRESS   // Program Counter

    var opcode = Array(2) {0}
    /*
    var memory = Array(4096) {0}     // 4096 bytes of memory (0x000 to 0xFFF)
    var registers = Array(16) {0}    // 16 8-bit registers
    var index = 0x00                        // Index register that stores a memory address
    var pc = START_ADDRESS                  // Program counter that holds the memory address of the next instruction
    var stack = Array(16) {0}        // Stack that holds 16 program counters
    var si = 0x00                            // Stack index
    var opcode = 0x0000                     // Current instruction opcode

    var delaytimer = 0x00                   // 8-bit delay timer
    var soundtimer = 0x00                   // 8-bit sound timer

    var input = Array(16) {0}     // 16 input keys (0 is off - 1 is on)
    var display = Array(2048) {0}    // 64x32 monochrome display memory

    // Load fontset into memory at FONTSET_START_ADDRESS when creating object
    init {
        for( (index,i) in fontset.withIndex() ) {
            memory[FONTSET_START_ADDRESS + index] = i
        }
    }
     */
    // Fetch current instruction (opcode) from memory at the address pointed by the program counter, and increase program counter
    fun fetch() {
        opcode[0] = (memory[regPC].toInt())
        opcode[1] = (memory[regPC+1].toInt())
        // Print current program counter and opcode (for debugging)
        /*
        print("Program Counter: 0x" + java.lang.Integer.toHexString(regPC).padStart(5,'0'))
        print(" Opcode: 0x" + opcode[0].toUByte().toString(16).padStart(2,'0') + opcode[1].toUByte().toString(16).padStart(2,'0') + "\n")
         */
        regPC += 2
    }
    // Decode current instruction (opcode)
    fun decode() {
        //when(opcode[0])
    }
    // Load ROM into memory
    fun loadRom(rompath: String) {
        val file = File(rompath)
        memory = file.readBytes()
    }
    // Print memory contents to csv file for debugging
    fun printMemoryToFile() {
        print("Writing to file...\n")
        var progress = 0
        val file = File("app\\src\\main\\assets\\memorylog.csv")
        val start = Instant.now()
        var end = Instant.now()
        var flag = 0
        FileOutputStream(file).use {
            for ((index, i) in memory.withIndex()) {
                if (index % 52428 == 0 && index != 0) {
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
                if (index % 30 == 0) {
                    //println()
                    file.appendText("\n")
                }
                file.appendText(Integer.toHexString(index))
                file.appendText("=>")
                file.appendText(Integer.toHexString(i.toUByte().toInt()))
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
        val file = File("app\\src\\main\\assets\\opcodes.txt")
        file.appendText("PC\t=> Opcode\n")
        try {
            while (true) {
                file.appendText("0x" + Integer.toHexString(this.regPC).padStart(5, '0'))
                file.appendText("\t=> ")
                this.fetch()
                file.appendText(
                    "0x" + this.opcode[0].toUByte().toString(16)
                        .padStart(2, '0') + this.opcode[1].toUByte().toString(16)
                        .padStart(2, '0') + "\n"
                )
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            print("Reached the end of file\n")
        }
    }
}
