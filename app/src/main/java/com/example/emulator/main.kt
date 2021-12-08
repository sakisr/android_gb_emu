package com.example.emulator

import java.io.File

fun main() {
    var gb = GameBoy()
    gb.loadRom("C:\\Users\\skpun\\Desktop\\Emulator\\app\\src\\main\\assets\\Pokemon Red.gb")
    //gb.printMemoryToFile()
    //gb.printMemoryToConsole()
    val file = File("C:\\Users\\skpun\\Desktop\\opcodes.csv")

    while(true){//gb.regPC != 1000) {
        file.appendText("Program Counter: 0x" + java.lang.Integer.toHexString(gb.regPC).padStart(5,'0'))
        file.appendText(" Opcode: 0x" + gb.opcode[0].toUByte().toString(16).padStart(2,'0') + gb.opcode[1].toUByte().toString(16).padStart(2,'0') + "\n")
        gb.fetch()
        gb.decode()
    }
}