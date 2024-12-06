// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._


/** 
  * Half Adder Class 
  * 
  * Your task is to implement a basic half adder as presented in the lecture.
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class HalfAdder extends Module{
  
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val s = Output(UInt(1.W))
    val co = Output(UInt(1.W))
    })

  io.s := io.a ^ io.b // XOR between inputs to get sum
  io.co := io.a & io.b // AND between inputs to get carry

}

/** 
  * Full Adder Class 
  * 
  * Your task is to implement a basic full adder. The component's behaviour should 
  * match the characteristics presented in the lecture. In addition, you are only allowed 
  * to use two half adders (use the class that you already implemented) and basic logic 
  * operators (AND, OR, ...).
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FullAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val cin = Input(UInt(1.W))
    val s = Output(UInt(1.W))
    val co = Output(UInt(1.W))
    })


  // Re-use Half Adder to build new full adder
  val HA1 = Module(new HalfAdder())
  val HA2 = Module(new HalfAdder())

  // HA1 takes input a, b
  HA1.io.a := io.a
  HA1.io.b := io.b

  // HA2 takes input sum of HA1, cin
  HA2.io.a := HA1.io.s
  HA2.io.b := io.cin

  // Sum is s of HA 2
  // Carry is or between carry of HA1, HA2
  io.s := HA2.io.s
  io.co := HA1.io.co | HA2.io.co
}

/** 
  * 4-bit Adder class 
  * 
  * Your task is to implement a 4-bit ripple-carry-adder. The component's behaviour should 
  * match the characteristics presented in the lecture.  Remember: An n-bit adder can be 
  * build using one half adder and n-1 full adders.
  * The inputs and the result should all be 4-bit wide, the carry-out only needs one bit.
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FourBitAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val s = Output(UInt(4.W))
    val co = Output(UInt(1.W))
    })

    // Instantiate 1 HA and 3 FA
    val HA1 = Module(new HalfAdder())
    val FA1 = Module(new FullAdder())
    val FA2 = Module(new FullAdder())
    val FA3 = Module(new FullAdder())

    // Attach the modules
    HA1.io.a := io.a(0)
    HA1.io.b := io.b(0)

    FA1.io.a := io.a(1)
    FA1.io.b := io.b(1)
    FA1.io.cin := HA1.io.co

    FA2.io.a := io.a(2)
    FA2.io.b := io.b(2)
    FA2.io.cin := FA1.io.co

    FA3.io.a := io.a(3)
    FA3.io.b := io.b(3)
    FA3.io.cin := FA2.io.co

    io.s := Cat(FA3.io.s, FA2.io.s, FA1.io.s, HA1.io.s)
    io.co := FA3.io.co
}
