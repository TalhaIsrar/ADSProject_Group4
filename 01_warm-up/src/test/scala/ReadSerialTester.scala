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
        /*** check non-consecutive input ***/
        { 
          // assert rst and set rxd
          dut.io.valid.expect(0.B)
          dut.io.rxd.poke(1.B)
          dut.io.reset_n.poke(1.B)
          dut.clock.step(1) 

          // deassert rst and reset rxd 
          dut.io.valid.expect(0.B) 
          dut.io.rxd.poke(0.B)
          dut.io.reset_n.poke(0.B)
          dut.clock.step(1)      

          // 8 bit data 0xA5 serial in rxd   
          val number = 100
          for(i <- (0 to 7).reverse){
            dut.io.valid.expect(0.B)
            val bit = (number >> i) & 1
            dut.io.rxd.poke(bit.B)
            dut.clock.step(1)
          }

          // stop transmitting
          dut.io.rxd.poke(1.B)

          // check data and valid 
          dut.io.valid.expect(1.B)
          dut.io.data.expect(number.U)

          // check valid down
          dut.clock.step(1)
          dut.io.valid.expect(0.B)
        }
        /*** check consecutive input ***/
        { 
          // set rst and rxd
          dut.io.valid.expect(0.B)
          dut.io.rxd.poke(1.B)
          dut.io.reset_n.poke(1.B)
          dut.clock.step(1) 

          // reset rst and rxd 
          dut.io.valid.expect(0.B) 
          dut.io.reset_n.poke(0.B)
          dut.io.rxd.poke(0.B)
          dut.clock.step(1)

          for(number <- 0 to 255)
          {
              // 8 bit data serial in rxd   
              for(i <- 7 to 0 by -1){
                dut.io.valid.expect(0.B)
                val bit = (number >> i) & 1
                dut.io.rxd.poke(bit.B)
                dut.clock.step(1)
              }      
              //continue to transmit data    
              dut.io.rxd.poke(0.B)
              // check data and valid 
              dut.io.valid.expect(1.B)
              dut.io.data.expect(number.U)
              dut.clock.step(1)
          }
        }
        /*** check reset during transmission ***/
        {          
          // transmit 0x45
          for(i <- 7 to 1 by -1){
            dut.io.valid.expect(0.B)
            val bit = ((0x45) >> i) & 1
            dut.io.rxd.poke(bit.B)
            dut.clock.step(1)
          }
          // assert reset
          dut.io.valid.expect(0.B)
          dut.io.reset_n.poke(1.B)
          dut.clock.step(1)
          // deassert reset 
          dut.io.valid.expect(0.B)
          dut.io.reset_n.poke(0.B)
          dut.io.rxd.poke(0.B)
          dut.clock.step(1)
          //transmit new number 0xFA
          for(i <- 7 to 0 by -1){
            dut.io.valid.expect(0.B)
            val bit = ((0xFA) >> i) & 1
            dut.io.rxd.poke(bit.B)
            dut.clock.step(1)
          }  
          // check data and valid 
          dut.io.valid.expect(1.B)
          dut.io.data.expect((0xFA).U)
        }
        }
    } 
}

