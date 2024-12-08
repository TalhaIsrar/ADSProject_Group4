// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

/** controller class */
class Controller extends Module{
  
  val io = IO(new Bundle {
    val reset_n = Input(UInt(1.W))
    val rxd = Input(UInt(1.W))
    val cnt_s = Input(UInt(1.W))
    val valid = Output(UInt(1.W))
    val shift = Output(UInt(1.W))
    val cnt_en = Output(UInt(1.W))
    })

  // Object to save states
  object State extends ChiselEnum {
    val sRst, sShift, sValid = Value
  }

  // Set Initial State
  val state = RegInit(State.sRst)

  // Default values for outputs
  io.valid := 0.U
  io.shift := 0.U
  io.cnt_en := 0.U

  // State Transition Logic
  switch(state) {
    is(State.sRst) {
      when(io.reset_n === 0.U & io.rxd === 0.U) {
        state := State.sShift
        io.valid := 0.U
        io.shift := 1.U
        io.cnt_en := 1.U
      }.otherwise {
        state := State.sRst
        io.valid := 0.U
        io.shift := 0.U
        io.cnt_en := 0.U
      }
    }
    is(State.sShift) {
      when(io.reset_n === 1.U) {
        state := State.sRst
        io.shift := 0.U
        io.cnt_en := 0.U
      }.elsewhen(io.cnt_s  === 1.U & io.rxd === 0.U) {
        state := State.sShift
        io.valid := 1.U
        io.shift := 0.U
        io.cnt_en := 1.U
      }.elsewhen(io.cnt_s  === 1.U) {
        state := State.sValid
        io.valid := 1.U
        io.shift := 0.U
        io.cnt_en := 0.U
      }.otherwise{
        state := State.sShift
        io.shift := 1.U
        io.cnt_en := 0.U   
      }
    }
    is(State.sValid) {
      when(io.reset_n === 0.U & io.rxd === 0.U) {
        state := State.sShift
        io.valid := 0.U
        io.shift := 1.U
        io.cnt_en := 1.U
      }.otherwise {
        state := State.sRst
        io.valid := 0.U
        io.shift := 0.U
      }
    }
  }
}

/** counter class */
class Counter extends Module{
  
  val io = IO(new Bundle {
    val reset_n = Input(UInt(1.W))
    val cnt_en = Input(UInt(1.W))
    val cnt_s = Output(UInt(1.W))
    })

  // Object to save states
  object State extends ChiselEnum {
    val sRst, sCnt, sDone = Value
  }

  // Set Initial State
  val state = RegInit(State.sRst)

  // Internal Variable
  val Count = RegInit(0.U(3.W))

    // Default values for outputs
  io.cnt_s := 0.U

  // State Transition Logic
  switch(state) {
    is(State.sRst) {
      when(io.reset_n === 0.U & io.cnt_en === 1.U) {
        state := State.sCnt
        io.cnt_s := 0.U
      }.otherwise {
        state := State.sRst
        io.cnt_s := 0.U
        Count := 0.U
      }
    }
    is(State.sCnt) {
      when(io.reset_n === 1.U) {
        state := State.sRst
        Count := 0.U
      }.elsewhen(Count >= 7.U) {
        state := State.sDone
        io.cnt_s := 1.U
        Count := 0.U
      }.otherwise{
        state := State.sCnt
        Count := Count + 1.U    
      }
    }
    is(State.sDone) {
      when(io.reset_n === 0.U & io.cnt_en === 1.U) {
        state := State.sCnt
        io.cnt_s := 0.U
      }.otherwise {
        state := State.sRst
        io.cnt_s := 0.U
      }  
    }
  }
}

/** shift register class */
class ShiftRegister extends Module{
  
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val shift = Input(UInt(1.W))
    val data = Output(UInt(8.W))
    })

  // internal variables - An 8 bit register to save current value in register
  val register = RegInit(0.U(8.W))

  // if the shift input is 1, on the clock cycle new value is inserted at LSB
  when(io.shift === 1.U) {
    register := Cat(register(6, 0), io.rxd)
  }

  // Output the current value of the register
  io.data := register
}

/** 
  * The last warm-up task deals with a more complex component. Your goal is to design a serial receiver.
  * It scans an input line (“serial bus”) named rxd for serial transmissions of data bytes. A transmission 
  * begins with a start bit ‘0’ followed by 8 data bits. The most significant bit (MSB) is transmitted first. 
  * There is no parity bit and no stop bit. After the last data bit has been transferred a new transmission 
  * (beginning with a start bit, ‘0’) may immediately follow. If there is no new transmission the bus line 
  * goes high (‘1’, this is considered the “idle” bus signal). In this case the receiver waits until the next 
  * transmission begins. The outputs of the design are an 8-bit parallel data signal and a valid signal. 
  * The valid signal goes high (‘1’) for one clock cycle after the last serial bit has been transmitted, 
  * indicating that a new data byte is ready.
  */
class ReadSerial extends Module{
  
  val io = IO(new Bundle {
    val reset_n = Input(UInt(1.W))
    val rxd = Input(UInt(1.W))
    val valid = Output(UInt(1.W))
    val data = Output(UInt(8.W))
    })


  // instanciation of modules
  val Controller = Module(new Controller())
  val Counter = Module(new Counter())
  val ShiftRegister = Module(new ShiftRegister())

  // Buffer cnt_s from Counter with a register to break the combinational loop
  val cnt_s_reg = RegNext(Counter.io.cnt_s, 0.U) // Register for cnt_s with an initial value of 0

  Controller.io.cnt_s := cnt_s_reg // Use the buffered version of cnt_s
  Counter.io.cnt_en := Controller.io.cnt_en

  // connections between modules
  Controller.io.reset_n := io.reset_n
  Controller.io.rxd := io.rxd

  Counter.io.reset_n := io.reset_n

  ShiftRegister.io.rxd := io.rxd
  ShiftRegister.io.shift := Controller.io.shift

  // global I/O 
  io.valid := Controller.io.valid
  io.data := ShiftRegister.io.data
}
