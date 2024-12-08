// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/** 
  *read serial tester
  */
class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {

  "ReadSerial" should "work" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.reset_n.poke(0.U)
        dut.io.rxd.poke(1.U)

        dut.clock.step(1)
        dut.io.reset_n.poke(1.U)
        dut.clock.step(1)
        dut.io.reset_n.poke(0.U)

        dut.io.rxd.poke(0.U)
        dut.clock.step(1)

        //INPUT
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)

        //OUTPUT
        dut.io.valid.expect(1.U)
        dut.io.data.expect("b10001010".U) 

        
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)

        //INPUT
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)

        //OUTPUT
        dut.io.valid.expect(1.U)
        dut.io.data.expect("b10011001".U)

        
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)

        //INPUT
        dut.io.rxd.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.io.reset_n.poke(1.U)
        dut.clock.step(1)
        dut.io.rxd.poke(0.U)
        dut.clock.step(1)
        }
    } 
}

