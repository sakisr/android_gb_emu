package com.example.emulator

fun main() {
    val gb = GameBoy()
    gb.loadRom("app/src/main/assets/Pokemon Red.gb")
    //gb.printOpcodesToFile()
    //gb.printMemoryToConsole()
    //gb.printMemoryToFile()
    while(gb.haltflag == 0){
        gb.fetch()
        gb.decode()
    }

    print(gb.convertToBits(0x43,0x32).contentToString())
    println()
    print(gb.performCalculation(0x43,0x32, "ADD").contentToString())

}