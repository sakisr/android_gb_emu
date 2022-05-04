package com.example.emulator

fun main() {
    val gb = GameBoy()
    //gb.loadRom("app/src/main/assets/Pokemon Red.gb")
    gb.loadRom("app/src/main/assets/gb-test-roms-master/cpu_instrs/cpu_instrs.gb")
    //gb.printOpcodesToFile()
    //gb.printMemoryToConsole()
    //gb.printMemoryToFile()
    while(gb.haltflag == 0){
        gb.fetch()
        gb.decode()
        //readLine()
        //gb.printOutputToFile()
    }
}