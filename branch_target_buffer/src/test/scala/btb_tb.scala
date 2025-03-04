package btb
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class btbTester extends AnyFlatSpec with ChiselScalatestTester {

  "btb" should "work" in {
    test(new btb).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        val PC_set0_0  = "h000A0000".U 
        val Tar_Add0_0 = "h000B0000".U 
        val PC_set1_0  = "h000A0004".U 
        val Tar_Add1_0 = "h000B0004".U 
        val PC_set2_0  = "h000A0008".U 
        val Tar_Add2_0 = "h000B0008".U 
        val PC_set3_0  = "h000A000C".U 
        val Tar_Add3_0 = "h000B000C".U 
        val PC_set4_0  = "h000A0010".U 
        val Tar_Add4_0 = "h000B0010".U 
        val PC_set5_0  = "h000A0014".U 
        val Tar_Add5_0 = "h000B0014".U 
        val PC_set6_0  = "h000A0018".U 
        val Tar_Add6_0 = "h000B0018".U 
        val PC_set7_0  = "h000A001C".U 
        val Tar_Add7_0 = "h000B001C".U 

        val PC_set0_1  = "h000A0020".U 
        val Tar_Add0_1 = "h000B0020".U 
        val PC_set1_1  = "h000A0024".U 
        val Tar_Add1_1 = "h000B0024".U 
        val PC_set2_1  = "h000A0028".U 
        val Tar_Add2_1 = "h000B0028".U 
        val PC_set3_1  = "h000A002C".U 
        val Tar_Add3_1 = "h000B002C".U 
        val PC_set4_1  = "h000A0030".U 
        val Tar_Add4_1 = "h000B0030".U 
        val PC_set5_1  = "h000A0034".U 
        val Tar_Add5_1 = "h000B0034".U 
        val PC_set6_1  = "h000A0038".U 
        val Tar_Add6_1 = "h000B0038".U 
        val PC_set7_1  = "h000A003C".U 
        val Tar_Add7_1 = "h000B003C".U 

        val PC_set0_2  = "h000A0040".U 
        val Tar_Add0_2 = "h000B0040".U 
        val PC_set1_2  = "h000A0044".U 
        val Tar_Add1_2 = "h000B0044".U 
        val PC_set2_2  = "h000A0048".U 
        val Tar_Add2_2 = "h000B0048".U 
        val PC_set3_2  = "h000A004C".U 
        val Tar_Add3_2 = "h000B004C".U 
        val PC_set4_2  = "h000A0050".U 
        val Tar_Add4_2 = "h000B0050".U 
        val PC_set5_2  = "h000A0054".U 
        val Tar_Add5_2 = "h000B0054".U 
        val PC_set6_2  = "h000A0058".U 
        val Tar_Add6_2 = "h000B0058".U 
        val PC_set7_2  = "h000A005C".U 
        val Tar_Add7_2 = "h000B005C".U 


      // Test Case 1: Applying initial values and checking the outputs
      dut.io.PC.poke(0x0000B000.U)           // Set PC to some initial value
      dut.io.update.poke(0.U)                // Set update signal to 1 (indicating update)

      // Apply a clock cycle
      dut.clock.step(1)
      dut.io.valid.expect(0.U)              // Expect valid to be 0

      dut.io.update.poke(1.U)                // Set update signal to 1 (indicating update)
      dut.io.updatePC.poke(0x0000B000.U)     // Set updatePC
      dut.io.updateTarget.poke(0x1000C000.U) // Set update target address
      dut.io.mispredicted.poke(1.U)          // Indicate that the branch was mispredicted (taken)
            
      // Apply a clock cycle
      dut.clock.step(1)

      // Check expected results
      dut.io.valid.expect(1.U)              // Expect valid to be 1 (updated)
      dut.io.target.expect(0x1000C000.U)    // Expect the target to be 0x1000C000
      dut.io.predictedTaken.expect(0.U)     // Expect predictedTaken to be 1 (branch taken)

      // Test Case 2: Changing inputs and checking updated results
      dut.io.update.poke(1.U)               // Set update signal to 0 (no update)
      dut.io.updatePC.poke(0x0000D000.U)    // Set a new updatePC
      dut.io.updateTarget.poke(0x2000D000.U) // Set a new target address
      dut.io.mispredicted.poke(0.U)         // Indicate that the branch was not mispredicted (not taken)
      dut.io.PC.poke(0x0000D000.U)          

      // Apply another clock cycle
      dut.clock.step(1)

      // Check expected results after the change
      dut.io.valid.expect(1.U)              // Expect valid to be 0 (no update)
      dut.io.target.expect(0x2000D000.U)    // Expect target to be 0x2000D000
      dut.io.predictedTaken.expect(0.U)     // Expect predictedTaken to be 0 (branch not taken)
      
      // Test Case 3: Apply different values for valid and mispredicted signals
      dut.io.update.poke(1.U)               // Set update signal to 1
      dut.io.updatePC.poke(0x0000E000.U)    // Set another updatePC
      dut.io.updateTarget.poke(0x3000E000.U) // Set another target address
      dut.io.mispredicted.poke(1.U)         // Indicate that the branch was mispredicted (taken)
      dut.io.PC.poke(0x0000E000.U)           // Set PC to some initial value

      // Apply another clock cycle
      dut.clock.step(1)

      // Test Case 3: Apply different values for valid and mispredicted signals
      dut.io.update.poke(1.U)               // Set update signal to 1
      dut.io.updatePC.poke(0x0000E000.U)    // Set another updatePC
      dut.io.updateTarget.poke(0x3000E000.U) // Set another target address
      dut.io.mispredicted.poke(1.U)         // Indicate that the branch was mispredicted (taken)
      dut.io.PC.poke(0x0000E000.U)           // Set PC to some initial value
      dut.io.predictedTaken.expect(0.U)     // Expect predictedTaken to be 0(branch not taken)

      // Apply another clock cycle
      dut.clock.step(1)
      dut.io.update.poke(0.U)               // Set update signal to 0

      // Apply another clock cycle
      dut.clock.step(1)

      // Check expected results after this change
      dut.io.valid.expect(1.U)              // Expect valid to be 1 (updated)
      dut.io.target.expect(0x3000E000.U)    // Expect target to be 0x3000E000
      dut.io.predictedTaken.expect(1.U)     // Expect predictedTaken to be 1 (branch taken)
    

        /**** Test set 0 ****/

        // 2 cycles at the beginning
        dut.io.PC.poke(PC_set0_0)
        dut.io.update.poke(0.U)
        dut.io.updatePC.poke(0.U)
        dut.io.updateTarget.poke(0.U)
        dut.io.mispredicted.poke(0.U)
        dut.io.valid.expect(0.U)
        dut.clock.step(1)

        dut.io.PC.poke(PC_set1_0)
        dut.io.valid.expect(0.U)
        dut.clock.step(1)

        // write set0_0 and not branch
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_0)
        dut.io.updateTarget.poke(Tar_Add0_0)
        dut.io.mispredicted.poke(0.U)
        dut.clock.step(1)
        dut.io.update.poke(0.U)

        // check if set0_0 is available
        dut.io.PC.poke(PC_set0_0)
        dut.clock.step(1)
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(0.U)
        dut.clock.step(1)

        // write set0_1 but update is false
        dut.io.update.poke(0.U)
        dut.io.updatePC.poke(PC_set0_1)
        dut.io.updateTarget.poke(Tar_Add0_1)
        dut.io.mispredicted.poke(0.U)
        dut.clock.step(1)

        // check if update is false, no entry is written
        dut.io.PC.poke(PC_set0_1)
        dut.clock.step(1)
        dut.io.valid.expect(0.U)
        dut.clock.step(1)

        // branch at set0_0 for 2 times, then BTB is in strongTaken
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_0)
        dut.io.updateTarget.poke(Tar_Add0_0)
        dut.io.mispredicted.poke(1.U)
        dut.clock.step(2)
        dut.io.PC.poke(PC_set0_0)
        dut.io.update.poke(0.U)
        dut.clock.step(1)

        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(1.U)
        dut.clock.step(1)

        // not branch at set0_0, then BTB is in weakTaken
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_0)
        dut.io.updateTarget.poke(Tar_Add0_0)
        dut.io.mispredicted.poke(1.U)
        dut.io.PC.poke(PC_set0_0)
        dut.clock.step(1)

        dut.io.update.poke(0.U)
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(1.U)
        dut.clock.step(1)

        // not branch at set0_0, then BTB is in strongNotTaken
        dut.io.update.poke(1.U)
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(1.U)
        dut.clock.step(1)
        dut.io.predictedTaken.expect(0.U)

        // branch at set0_0, then BTB is in weakNotTaken
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_0)
        dut.io.updateTarget.poke(Tar_Add0_0)
        dut.io.mispredicted.poke(1.U)
        dut.clock.step(1)

        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(0.U)
        dut.io.PC.poke(PC_set0_0)
        dut.clock.step(1)

        // branch at set0_0, then BTB is in strongTaken
 
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_0)
        dut.io.predictedTaken.expect(1.U)
        dut.clock.step(1)

        // write set0_1 and not branch
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_1)
        dut.io.updateTarget.poke(Tar_Add0_1)
        dut.io.mispredicted.poke(0.U)
        dut.clock.step(1)
        dut.io.update.poke(0.U)

        // check if set0_1 is available
        dut.io.PC.poke(PC_set0_1)
        dut.clock.step(1)
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_1)
        dut.clock.step(1)

        // write set0_2 and not branch
        dut.io.update.poke(1.U)
        dut.io.updatePC.poke(PC_set0_2)
        dut.io.updateTarget.poke(Tar_Add0_2)
        dut.io.mispredicted.poke(0.U)
        dut.clock.step(1)
        dut.io.update.poke(0.U)

        // check if set0_2 is available
        dut.io.PC.poke(PC_set0_2)
        dut.clock.step(1)
        dut.io.valid.expect(1.U)
        dut.io.target.expect(Tar_Add0_2)
        dut.clock.step(1)

        // check if set0_0 is not available
        dut.io.PC.poke(PC_set0_0)
        dut.clock.step(1)
        dut.io.valid.expect(0.U)
        dut.clock.step(1) 


    }
  } 
}

