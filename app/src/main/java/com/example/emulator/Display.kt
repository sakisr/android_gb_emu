package com.example.emulator

import android.graphics.Bitmap

const val SCREEN_WIDTH = 160
const val SCREEN_HEIGHT = 144

class Display {
    // 160x144 Screen
    var screen = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)

    // PPU Registers
    // LCD Control Register
    var regLCDC = 0x00
    // LCD Status Register
    var regSTAT = 0x00
    // Vertical Scroll Register
    var regSCY = 0x00
    // Horizontal Scroll Register
    var regSCX = 0x00
    // Scanline Register
    var regLY = 0x00
    // Scanline Compare Register
    var regLYC = 0x00
}