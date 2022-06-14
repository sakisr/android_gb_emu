package com.example.emulator
/*
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val myCanvas = CanvasView(this)

        setContentView(myCanvas)
    }
}

 */
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