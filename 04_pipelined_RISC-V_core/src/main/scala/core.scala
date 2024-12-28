// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

/*
The goal of this task is to extend the 5-stage multi-cycle 32-bit RISC-V core from the previous task to a pipelined processor. 
All steps and stages have the same functionality as in the multi-cycle version from task 03, but are supposed to handle different instructions in each stage simultaneously.
This design implements a pipelined RISC-V 32-bit core with five stages: IF (Fetch), ID (Decode), EX (Execute), MEM (Memory), and WB (Writeback).

    Data Types:
        The uopc enumeration data type (enum) defines micro-operation codes representing ALU operations according to the RV32I subset used in the previous tasks.

    Register File (regFile):
        The regFile module represents the register file, which has read and write ports.
        It consists of a 32-entry register file (x0 is hard-wired to zero).
        Reading from and writing to the register file is controlled by the read request (regFileReadReq), read response (regFileReadResp), and write request (regFileWriteReq) interfaces.

    Fetch Stage (IF Module):
        The IF module represents the instruction fetch stage.
        It includes an instruction memory (IMem) of size 4096 words (32-bit each).
        Instructions are loaded from a binary file (provided to the testbench as a parameter) during initialization.
        The program counter (PC) is used as an address to access the instruction memory, and one instruction is fetched in each cycle.

    Decode Stage (ID Module):
        The ID module performs instruction decoding and generates control signals.
        It extracts opcode, operands, and immediate values from the instruction.
        It uses the uopc (micro-operation code) Enum to determine the micro-operation (uop) and sets control signals accordingly.
        The register file requests are generated based on the operands in the instruction.

    Execute Stage (EX Module):
        The EX module performs the arithmetic or logic operation based on the micro-operation code.
        It takes two operands and produces the result (aluResult).

    Memory Stage (MEM Module):
        The MEM module does not perform any memory operations in this basic CPU design.

    Writeback Stage (WB Module):
        The WB module writes the result back to the register file.

    IF, ID, EX, MEM, WB Barriers:
        IFBarrier, IDBarrier, EXBarrier, MEMBarrier, and WBBarrier modules serve as pipeline registers to separate the pipeline stages.
        They hold the intermediate results of each stage until the next clock cycle.

    PipelinedRV32Icore (PipelinedRV32Icore Module):
        The top-level module that connects all the pipeline stages, barriers and the register file.
        It interfaces with the external world through check_res, which is the result produced by the core.

Overall Execution Flow:

    1) Instructions are fetched from the instruction memory in the IF stage.
    2) The fetched instruction is decoded in the ID stage, and the corresponding micro-operation code is determined.
    3) The EX stage executes the operation using the operands.
    4) The MEM stage does not perform any memory operations in this design.
    5) The result is written back to the register file in the WB stage.

Note that this design only represents a simplified RISC-V pipeline. The structure could be equipped with further instructions and extension to support a real RISC-V ISA.
*/

package core_tile

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile


// -----------------------------------------
// Global Definitions and Data Types
// -----------------------------------------

object uopc extends ChiselEnum {

  val isADD   = Value(0x01.U)
  val isSUB   = Value(0x02.U)
  val isXOR   = Value(0x03.U)
  val isOR    = Value(0x04.U)
  val isAND   = Value(0x05.U)
  val isSLL   = Value(0x06.U)
  val isSRL   = Value(0x07.U)
  val isSRA   = Value(0x08.U)
  val isSLT   = Value(0x09.U)
  val isSLTU  = Value(0x0A.U)
  val isADDI  = Value(0x10.U)
  val invalid = Value(0xFF.U)
}

import uopc._

// -----------------------------------------
// Register File
// -----------------------------------------

class regFileReadReq extends Bundle {
    // what signals does a read request need?
  val addr1 = Input(UInt(5.W))  // Read address for first operand (rs1)
  val addr2 = Input(UInt(5.W))  // Read address for second operand (rs2)
}

class regFileReadResp extends Bundle {
    // what signals does a read response need?
  val data1 = Output(UInt(32.W))  // Data read from rs1
  val data2 = Output(UInt(32.W))  // Data read from rs2
}

class regFileWriteReq extends Bundle {
    // what signals does a write request need?
  val addr    = Input(UInt(5.W))   // Write register address (rd)
  val data    = Input(UInt(32.W))  // Data to write to the register
  val writeEn = Input(Bool())      // Control signal to enable writing
}

class regFile extends Module {
  val io = IO(new Bundle {
    val req      = Input(new regFileReadReq)
    val resp     = Output(new regFileReadResp)
    // how many read and write ports do you need to handle all requests
    // from the pipeline to the register file simultaneously?
    val writeReq = Input(new regFileWriteReq)
  })
   
  val regFile = Mem(32, UInt(32.W)) 
  regFile(0.U) := 0.U

  // Handle read requests
  io.resp.data1 := regFile(io.req.addr1) // Read the first register
  io.resp.data2 := regFile(io.req.addr2) // Read the second register

  // Handle write requests (if enabled and addr != 0)
  when(io.writeReq.writeEn && io.writeReq.addr =/= 0.U) {
    regFile(io.writeReq.addr) := io.writeReq.data
  }
}

// -----------------------------------------
// Fetch Stage
// -----------------------------------------

class IF (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val pc_old = Input(UInt(16.W))
    val instruction = Output(UInt(32.W))
    val pc = Output(UInt(16.W))
  })

  // Initialize IMEM  to handle instruction fetch
  val IMem = Mem(4096, UInt(32.W))
  loadMemoryFromFile(IMem, BinaryFile)

  // Connect outputs to IO interface
  io.instruction := IMem(io.pc_old>>2.U)
  io.pc := io.pc_old + 4.U
}

// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val instruction = Input(UInt(32.W))

    val uopc_out = Output(uopc())
    val immediate = Output(SInt(32.W))
    val rd = Output(UInt(5.W))
    val rs1 = Output(UInt(5.W))         
    val rs2 = Output(UInt(5.W))
  })

  // Internal Signals
  val opcode = Wire(UInt(7.W))
  val funct3 = Wire(UInt(3.W))
  val funct7 = Wire(UInt(7.W))
  val rs2 = Wire(UInt(5.W))
  val rs1 = Wire(UInt(5.W))
  val extracted_imm = Wire(SInt(12.W))
  val rd = Wire(UInt(5.W))
  val immediate = Wire(SInt(32.W))

  opcode := io.instruction(6, 0)
  funct3 := io.instruction(14, 12)
  funct7 := io.instruction(31, 25)
  rs2 := io.instruction(24, 20)
  rs1 := io.instruction(19,15)
  rd := io.instruction(11, 7)

  // Assign values to output
  io.rd := rd
  io.rs1 := rs1
  io.rs2 := rs2

  // Identify the instruction using opcode, func3 and func7
  when(opcode === "b0010011".U){
    when(funct3 === "b000".U){
      io.uopc_out := isADDI
    }
    .otherwise{
      io.uopc_out := invalid
    } 
  }
  .elsewhen(opcode === "b0110011".U){
    when(funct7 === "b0000000".U){
      when(funct3 === "b000".U){
        io.uopc_out := isADD
      }
      .elsewhen(funct3 === "b010".U){
        io.uopc_out := isSLT
      }
      .elsewhen(funct3 === "b011".U){
        io.uopc_out := isSLTU
      }
      .elsewhen(funct3 === "b001".U){
        io.uopc_out := isSLL
      }
      .elsewhen(funct3 === "b101".U){
        io.uopc_out := isSRL
      }
      .elsewhen(funct3 === "b111".U){
        io.uopc_out := isAND
      }
      .elsewhen(funct3 === "b110".U){
        io.uopc_out := isOR
      }
      .elsewhen(funct3 === "b100".U){
        io.uopc_out := isXOR
      }
      .otherwise{
        io.uopc_out := invalid
      } 
    }
    .elsewhen(funct7 === "b0100000".U){
      when(funct3 === "b000".U){
        io.uopc_out := isSUB
      }
      .elsewhen(funct3 === "b101".U){
        io.uopc_out := isSRA
      }
      .otherwise{
        io.uopc_out := invalid
      } 
    }
    .otherwise{
      io.uopc_out := invalid
    } 
  }
  .otherwise{
    io.uopc_out := invalid
  }

  // Find the immediate and send to output
  extracted_imm := io.instruction(31, 20).asSInt
  immediate := extracted_imm.asSInt
  io.immediate := immediate

  // RegFile operations are done outside the block
  // The flow of data is   IFBarrier -> ID -> Regfile -> IDBarrier
}

// -----------------------------------------
// Execute Stage
// -----------------------------------------

class EX extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val operandA = Input(UInt(32.W))       
    val operandB = Input(UInt(32.W))       
    val immediate = Input(SInt(32.W))      
    val uopc_in = Input(uopc())  
    val rd_in = Input(UInt(5.W))    
    val rd_out = Output(UInt(5.W))       
    val aluResult = Output(UInt(32.W))  
  })

  val aluResult = Wire(UInt(32.W))

  // Perform ALU operation according to input function
  when(io.uopc_in === uopc.isADDI){ 
      aluResult := io.immediate.asUInt + io.operandA  
    }.elsewhen(io.uopc_in === uopc.isADD){                            
      aluResult := io.operandA + io.operandB  
    }.elsewhen(io.uopc_in === uopc.isSUB){
      aluResult := io.operandA - io.operandB
    }.elsewhen(io.uopc_in === uopc.isSLT){
      when(io.operandA.asSInt < io.operandB.asSInt){
        aluResult := 1.U
      }.otherwise{
        aluResult := 0.U
      }
    }.elsewhen(io.uopc_in === uopc.isSLTU){
        when(io.operandA < io.operandB){
          aluResult := 1.U
        }.otherwise{
          aluResult := 0.U
        }
    }.elsewhen(io.uopc_in === uopc.isSLL){
      aluResult := io.operandA << (io.operandB(4, 0))
    }.elsewhen(io.uopc_in === uopc.isSRL){
      aluResult := io.operandA >> (io.operandB(4, 0))    
    }.elsewhen(io.uopc_in === uopc.isSRA){
      aluResult := (io.operandA.asSInt >> (io.operandB(4, 0))).asUInt 
    }.elsewhen(io.uopc_in === uopc.isAND){
      aluResult := io.operandA & io.operandB
    }.elsewhen(io.uopc_in === uopc.isOR){
      aluResult := io.operandA | io.operandB
    }.elsewhen(io.uopc_in === uopc.isXOR){
      aluResult := io.operandA ^ io.operandB
    }.otherwise{
      aluResult := "hFFFFFFFF".U //default case
    }

    io.aluResult := aluResult
    io.rd_out := io.rd_in
}

// -----------------------------------------
// Memory Stage
// -----------------------------------------

class MEM extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val alu_result_in = Input(UInt(32.W))    // ALU result from EX
    val rd_in = Input(UInt(5.W))              // rd from EX

    val data_out = Output(UInt(32.W))         // Data to MEMBarrier
    val rd_out = Output(UInt(5.W))            // rd to MEMBarrier
  })

  // No memory operations implemented in this basic CPU
  io.data_out := io.alu_result_in   // Just forward ALU result to MEMBarrier
  io.rd_out := io.rd_in             // Forward rd to MEMBarrier
}


// -----------------------------------------
// Writeback Stage
// -----------------------------------------

class WB extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val aluResult = Input(UInt(32.W)) 
    val rd = Input(UInt(5.W)) 

    val aluResult_out = Output(UInt(32.W))
    val rd_out = Output(UInt(5.W)) 
  })

  // The alu Result is read into writeback
  // RegFile operations are done outside the block
  // The flow of data is   MEM_Barrier -> WB -> Regfile -> WBBarrier
  val writeBackData = Wire(UInt(32.W)) 
  writeBackData := io.aluResult

  io.rd_out := io.rd
  io.aluResult_out := writeBackData
}

// -----------------------------------------
// IF-Barrier
// -----------------------------------------

class IFBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val pc_in = Input(UInt(32.W))            // PC input from the IF stage
    val instruction_in = Input(UInt(32.W))   // Instruction input from the IF stage
    val pc_out = Output(UInt(32.W))           // PC output to the ID stage
    val instruction_out = Output(UInt(32.W)) // Instruction output to the ID stage
  })

  // Registers to store PC value and instruction
  val pc_reg = RegInit(0.U(32.W))
  val instruction_reg = RegInit(0.U(32.W))

  // Store inputs into registers (acting as barrier)
  pc_reg := io.pc_in
  instruction_reg := io.instruction_in

  // Outputs to next stage (ID)
  io.pc_out := pc_reg
  io.instruction_out := instruction_reg
}

// -----------------------------------------
// ID-Barrier
// -----------------------------------------

class IDBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val operandA_in = Input(UInt(32.W))        // Operand A from ID
    val operandB_in = Input(UInt(32.W))        // Operand B from ID
    val immediate_in = Input(SInt(32.W))       // Immediate from ID
    val uopc_in = Input(uopc())             // uopc from ID
    val rd_in = Input(UInt(5.W))               // Destination register (rd) from ID
    val rs1_in = Input(UInt(5.W))              // rs1 from ID (register source 1)
    val rs2_in = Input(UInt(5.W))              // rs2 from ID (register source 2)

    val rs1_out = Output(UInt(5.W))            // rs1 to next stage (EX)
    val rs2_out = Output(UInt(5.W))            // rs2 to next stage (EX)
    val operandA_out = Output(UInt(32.W))      // Operand A to EX
    val operandB_out = Output(UInt(32.W))      // Operand B to EX
    val immediate_out = Output(SInt(32.W))     // Immediate to EX
    val uopc_out = Output(uopc())           // uopc to EX
    val rd_out = Output(UInt(5.W))             // rd to EX
  })

  // Register for ALU inputs
  val operandA_reg = RegInit(0.U(32.W))
  val operandB_reg = RegInit(0.U(32.W))
  val immediate_reg = RegInit(0.S(32.W))
  val uopc_reg = RegInit(uopc.isADD)
  val rd_reg = RegInit(0.U(5.W))
  val rs1_reg = RegInit(0.U(5.W))  
  val rs2_reg = RegInit(0.U(5.W))  

  // Store inputs into registers (acting as barrier)
  operandA_reg := io.operandA_in
  operandB_reg := io.operandB_in
  immediate_reg := io.immediate_in
  uopc_reg := io.uopc_in
  rd_reg := io.rd_in
  rs1_reg := io.rs1_in  
  rs2_reg := io.rs2_in 

  // Outputs to next stage (EX)
  io.operandA_out := operandA_reg
  io.operandB_out := operandB_reg
  io.immediate_out := immediate_reg
  io.uopc_out := uopc_reg
  io.rd_out := rd_reg
  io.rs1_out := rs1_reg
  io.rs2_out := rs2_reg
}

// -----------------------------------------
// EX-Barrier
// -----------------------------------------

class EXBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val alu_result_in = Input(UInt(32.W))    // ALU result from EX
    val rd_in = Input(UInt(5.W))              // rd from EX

    val alu_result_out = Output(UInt(32.W))   // ALU result to MEM
    val rd_out = Output(UInt(5.W))            // rd to MEM
  })

  // Registers to store output and output register
  val alu_result_reg = RegInit(0.U(32.W))
  val rd_reg = RegInit(0.U(5.W))

  // Store inputs into registers (acting as barrier)
  alu_result_reg := io.alu_result_in
  rd_reg := io.rd_in

  // Outputs to next stage (MEM)
  io.alu_result_out := alu_result_reg
  io.rd_out := rd_reg
}

// -----------------------------------------
// MEM-Barrier
// -----------------------------------------

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val data_in = Input(UInt(32.W))       // Data from MEM
    val rd_in = Input(UInt(5.W))           // rd from MEM

    val data_out = Output(UInt(32.W))      // Data to WB
    val rd_out = Output(UInt(5.W))         // rd to WB
  })

  // Registers to pass on data
  val data_reg = RegInit(0.U(32.W))
  val rd_reg = RegInit(0.U(5.W))

  // Store inputs into registers (acting as barrier)
  data_reg := io.data_in
  rd_reg := io.rd_in

  // Outputs to next stage (WB)
  io.data_out := data_reg
  io.rd_out := rd_reg
}


// -----------------------------------------
// WB-Barrier
// -----------------------------------------

class WBBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val data_in = Input(UInt(32.W))       // Data from WB

    val data_out = Output(UInt(32.W))      // Final check result output
  })

  // Register to store result
  val data_reg = RegInit(0.U(32.W))

  // Store inputs into registers (acting as barrier)
  data_reg := io.data_in

  // Outputs to register file or final result
  io.data_out := data_reg
}


class PipelinedRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
  })

  // Barrier initialization
  val if_barrier = Module(new IFBarrier)
  val id_barrier = Module(new IDBarrier)
  val ex_barrier = Module(new EXBarrier)
  val mem_barrier = Module(new MEMBarrier)
  val wb_barrier = Module(new WBBarrier)

  // Pipeline stage initialization
  val if_stage = Module(new IF(BinaryFile))
  val id_stage = Module(new ID)
  val ex_stage = Module(new EX)
  val mem_stage = Module(new MEM)
  val wb_stage = Module(new WB)

  // Register file initialization
  val regFile = Module(new regFile)

  // IOs need default case
  io.check_res := "h_0000_0000".U

  // Connections between stages, barriers and register file
  if_stage.io.pc_old := if_barrier.io.pc_out

   // Connect IF -> IF Barrier
  if_barrier.io.pc_in := if_stage.io.pc
  if_barrier.io.instruction_in := if_stage.io.instruction

  // Connect IF Barrier -> ID
  id_stage.io.instruction := if_barrier.io.instruction_out

  regFile.io.req.addr1 := id_stage.io.rs1  // Address for reading rs1
  regFile.io.req.addr2 := id_stage.io.rs2  // Address for reading rs2

  // Connect ID -> RegFile
  regFile.io.writeReq.addr := 0.U  // Default write address
  regFile.io.writeReq.data := 0.U  // Default write data
  regFile.io.writeReq.writeEn := false.B  // Disable writing by default

  // Connect ID & RegFile -> ID Barrier
  id_barrier.io.operandA_in := regFile.io.resp.data1
  id_barrier.io.operandB_in := regFile.io.resp.data2 
  id_barrier.io.immediate_in := id_stage.io.immediate
  id_barrier.io.uopc_in := id_stage.io.uopc_out
  id_barrier.io.rd_in := id_stage.io.rd 
  id_barrier.io.rs1_in := id_stage.io.rs1 
  id_barrier.io.rs2_in := id_stage.io.rs2 

  // Connect ID Barrier -> EX
  ex_stage.io.operandA := id_barrier.io.operandA_out
  ex_stage.io.operandB := id_barrier.io.operandB_out
  ex_stage.io.immediate := id_barrier.io.immediate_out
  ex_stage.io.rd_in := id_barrier.io.rd_out
  ex_stage.io.uopc_in := id_barrier.io.uopc_out

  // Connect EX -> EX Barrier
  ex_barrier.io.alu_result_in := ex_stage.io.aluResult
  ex_barrier.io.rd_in := ex_stage.io.rd_out

  // Connect EX Barrier -> MEM
  mem_stage.io.alu_result_in := ex_barrier.io.alu_result_out
  mem_stage.io.rd_in := ex_barrier.io.rd_out
 
  // Connect MEM -> MEM Barrier
  mem_barrier.io.data_in := mem_stage.io.data_out
  mem_barrier.io.rd_in := mem_stage.io.rd_out

  // Connect MEM Barrier -> WB
  wb_stage.io.aluResult := mem_barrier.io.data_out
  wb_stage.io.rd := mem_barrier.io.rd_out

  // Connect WB -> RegFile
  regFile.io.writeReq.addr := wb_stage.io.rd_out            // Destination register (rd)
  regFile.io.writeReq.data := wb_stage.io.aluResult_out     // Data to write to rd
  regFile.io.writeReq.writeEn := true.B                     // Enable writing to register

  // Connect WB -> WB Barrier
  wb_barrier.io.data_in := wb_stage.io.aluResult_out

  // Final output from WB barrier
  io.check_res := wb_barrier.io.data_out
}