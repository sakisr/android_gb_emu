package com.example.emulator

import java.io.File

fun main() {
    val gb = GameBoy()
    gb.loadRom("app\\src\\main\\assets\\Pokemon Red.gb")
    gb.printMemoryToFile()
    /*
    while(true){//gb.regPC != 1000) {
        gb.fetch()
        gb.decode()
    }
    */
}