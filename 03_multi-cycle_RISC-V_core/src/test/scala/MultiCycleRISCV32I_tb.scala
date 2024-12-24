// ADS I Class Project
// Multi-Cycle RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 12/19/2023 by Tobias Jauch (@tojauch)

package MultiCycleRV32I_Tester

import chisel3._
import chiseltest._
import MultiCycleRV32I._
import org.scalatest.flatspec.AnyFlatSpec

class MultiCycleRISCV32ITest extends AnyFlatSpec with ChiselScalatestTester {

"MultiCycleRV32I_Tester" should "work" in {
    test(new MultiCycleRV32I("src/test/programs/BinaryFile")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)
      dut.clock.step(4)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.clock.step(5)
      dut.io.result.expect(4.U)     // ADDI x1, x0, 4
      dut.clock.step(5)
      dut.io.result.expect(5.U)     // ADDI x2, x0, 5
      dut.clock.step(5)
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.clock.step(5)
      dut.io.result.expect(10.U)     // ADDI x1, x0, 10 
      dut.clock.step(5)
      dut.io.result.expect(20.U)     // ADDI x2, x0, 20
      dut.clock.step(5)
      dut.io.result.expect(5.U)      // ADDI x3, x0, 5   
      dut.clock.step(5)
      dut.io.result.expect(15.U)     // ADDI x4, x0, 15  
      dut.clock.step(5)
      dut.io.result.expect(7.U)      // ADDI x5, x0, 7
      dut.clock.step(5)
      dut.io.result.expect(3.U)      // ADDI x6, x0, 3
      dut.clock.step(5)
      dut.io.result.expect(30.U)     // ADD x7, x1, x2
      dut.clock.step(5)
      dut.io.result.expect(10.U)     // SUB x8, x4, x3 
      dut.clock.step(5)
      dut.io.result.expect(0.U)      // SLT x9, x5, x6
      dut.clock.step(5)
      dut.io.result.expect(1.U)      // SLTU x10, x6, x5
      dut.clock.step(5)
      dut.io.result.expect(0.U)      // AND x11, x1, x2
      dut.clock.step(5)
      dut.io.result.expect(15.U)     // OR x12, x1, x3
      dut.clock.step(5)
      dut.io.result.expect(8.U)      // XOR x13, x4, x5
      dut.clock.step(5)
      dut.io.result.expect(40.U)     // SLL x14, x3, x6 
      dut.clock.step(5)
      dut.io.result.expect(1.U)      // SRL x15, x4, x6 
      dut.clock.step(5)
      dut.io.result.expect(1.U)      // SRA x16, x4, x6
      dut.clock.step(5)
      dut.io.result.expect(40.U)     // ADD x17, x7, x8
      dut.clock.step(5)
      dut.io.result.expect(50.U)     // ADDI x18, x17, 10
      dut.clock.step(5)
      dut.io.result.expect(0.U)      // SLTU x19, x0, x1
      dut.clock.step(5)
      dut.io.result.expect(0.U)      // ADDI x0, x0, 0
      dut.clock.step(5)
      dut.io.result.expect(50.U)      // ADDI x0, x18, 0
      dut.clock.step(5)
      dut.io.result.expect(0.U)      // ADDI x1, x0, 0

      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFFF".U)
           
    }
  }
}


