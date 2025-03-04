package btb

import chisel3._
import chisel3.util._

// Read request for branch target buffer file
class btbReadReq extends Bundle {
    val index  = Input(UInt(3.W)) // 2^3 = 8 Possible sets 
}

// Read responce for branch target buffer file
class btbReadResp extends Bundle {
    val set  = Output(UInt(128.W)) // Set of 2 enteries: 64 bits each
}

// Write request for branch target buffer file
class btbWriteReq extends Bundle {
    val index  = Input(UInt(3.W))
    val set  = Input(UInt(128.W))
    val update = Input(UInt(1.W))
}

// Branct target buffer file implementation
class btbFile extends Module {
  val io = IO(new Bundle {
    // Read port for IF stage read access
    val readReq  = new btbReadReq
    val readResp = new btbReadResp

    // Read port for EX stage update request
    val updateReq  = new btbReadReq
    val updateResp = new btbReadResp

    // Write port for EX stage write
    val writeReq  = new btbWriteReq
})

  // Register-based storage for BTB (8 sets, 128 bits each)
  val btbFile = RegInit(VecInit(Seq.fill(8)(0.U(128.W))))

  // Read requests
  // Forwarding if we have read and write to same PC address
  when((io.writeReq.update).asBool && (io.readReq.index === io.writeReq.index))
  { 
    io.readResp.set := io.writeReq.set
  }.otherwise
  {
    io.readResp.set := btbFile(io.readReq.index)
  }

  io.updateResp.set := btbFile(io.updateReq.index)

  // Write request
  when((io.writeReq.update).asBool)
  {
    btbFile(io.writeReq.index) := io.writeReq.set
  }

}

// FSM for 2 bit dynamic branch perdiction
class FSM extends Module {
  val io = IO(new Bundle {
    val currentState = Input(UInt(2.W))   // Current state, 2 bits to represent 4 states
    val input        = Input(Bool())      // 1-bit input (mis-prediction)
    val nextState    = Output(UInt(2.W))  // Next state, 2 bits
  })

  // Next state logic using Mux for combinational state transition
  io.nextState := MuxCase(0.U, Array(
    (io.currentState === 0.U) -> Mux(io.input, 1.U, 0.U),  // From 00 - strongNotTaken
    (io.currentState === 1.U) -> Mux(io.input, 2.U, 0.U),  // From 01 - weakNotTaken
    (io.currentState === 2.U) -> Mux(io.input, 3.U, 2.U),  // From 10 - strongTaken
    (io.currentState === 3.U) -> Mux(io.input, 0.U, 2.U)   // From 11 - weakTaken
  ))
}

// Branch target Buffer module
class btb extends Module{
  
  val io = IO(new Bundle {
    // Inputs
    val PC = Input(UInt(32.W))           // 32 bit PC for current instruction       - IF stage
    val update = Input(UInt(1.W))        // 1 bit update for modifying btb          - EX stage
    val updatePC = Input(UInt(32.W))     // 32 bit PC for updating btb              - EX stage
    val updateTarget = Input(UInt(32.W)) // 32 bit address for updatePC             - EX stage
    val mispredicted = Input(UInt(1.W))  // 1 bit mispredicted for wrong prediction - EX stage

    // Outputs
    val valid = Output(UInt(1.W))           // 1 bit valid to show entry exists in btb - IF stage
    val target = Output(UInt(32.W))         // 32 bit target address for valid entry   - IF stage
    val predictedTaken = Output(UInt(1.W))  // 1 bit to show branch taken or not       - IF stage
    })

  // Initialize FSM for write modules
  val fsm_branch1 = Module(new FSM())
  val fsm_branch2 = Module(new FSM())

  // Initialize FSM for branch target buffer file
  val btbFile = Module(new btbFile)

  // LRU tracking for 8 set 2 way btb 
  // Since each entry has only 2 ways, we can represent each by only 1 bit
  // 0 means 64 bits of MSB were used recently .. replace LSB bits next
  // 1 means 64 bits of LSB were used recently .. replace MSB bits next
  val LRU = RegInit(0.U(8.W))

  // ------------ IF stage operations ------------

  // Initialize signals
  val index = Wire(UInt(3.W))
  val tag = Wire(UInt(27.W))
  val set = Wire(UInt(128.W))
  
  // Extract index and tag from PC
  // PC (32 bits) = Tag (27 bits) + Index (3 bits) + Byte offset (2 bits)
  index := io.PC(4, 2)
  tag := io.PC(31, 5)

  // Extract a set from branch target buffer file using index (3 bits)
  btbFile.io.readReq.index := index
  set := btbFile.io.readResp.set

  // Extract each branch details (64 bits)from within the set (128 bits)
  val branch1 = Wire(UInt(64.W))
  val branch2 = Wire(UInt(64.W))

  // Initialize signals to extract from BTB set
  // Set (128 bits) = Branch1 Details (64 bits) + Branch2 Details (64 bits)
  val valid1 = Wire(UInt(1.W))
  val valid2 = Wire(UInt(1.W))

  val tag1 = Wire(UInt(27.W))
  val tag2 = Wire(UInt(27.W))

  val target1 = Wire(UInt(32.W))
  val target2 = Wire(UInt(32.W))

  val fsm1 = Wire(UInt(2.W))
  val fsm2 = Wire(UInt(2.W))

  // Branch (64 bits) = Valid (1 bit) + Tag (27 bits) + Target Addess (32 bits) + Dynamic 2 bit prediction (2 bits)
  // Branch 1 = 64 bits of MSB
  // Branch 2 = 64 bits of LSB
  branch1 := set(127, 64)
  branch2 := set(63, 0)

  valid1 := branch1(63)
  valid2 := branch2(63)

  tag1 := branch1(62, 36)
  tag2 := branch2(62, 36)

  target1 := branch1(35, 4)
  target2 := branch2(35, 4)

  fsm1 := branch1(3, 2)
  fsm2 := branch2(3, 2)

  // Check for each branch in set
  val check_branch1 = Wire(Bool())
  val check_branch2 = Wire(Bool())

  // Comparator + AND Gate to check if required tag exists in branch and if value is valid
  check_branch1 := valid1.asBool && (tag === tag1)
  check_branch2 := valid2.asBool && (tag === tag2)

  // Valid signals checks if any branch has tag
  io.valid := (check_branch1 || check_branch2).asUInt

  // Target signals extracts value from correct branch
  io.target := Mux(check_branch1, target1, target2)

  val current_fsm = Wire(UInt(2.W))

  // Extract prediction using btb
  current_fsm := Mux(check_branch1, fsm1, Mux(check_branch2, fsm2, 0.U))
  
  // predictedTaken is 0 for strongNotTaken(00) && weakNotTaken(01)
  // predictedTaken is 1 for strongTaken(10) && weakTaken(11)
  // This is same as MSB of state
  io.predictedTaken := current_fsm(1)

  // read the LRU value for the current set
  val lru_read = Wire(UInt(1.W))
  lru_read := LRU(index)

  // Update the LRU value
  // If branch 1 was used then LRU = 0, If branch 2 was used then LRU = 1, otherwise keep old value
  val update_lru_read = Wire(UInt(1.W))
  update_lru_read := Mux(check_branch1, 0.U, Mux(check_branch2, 1.U, lru_read))


  // ------------ EX stage operations ------------

  // Initialize signals
  val update_index = Wire(UInt(3.W))
  val update_tag = Wire(UInt(27.W))
  val update_set = Wire(UInt(128.W))

  // Extract index and tag to update from updatePC
  update_index := io.updatePC(4, 2)
  update_tag := io.updatePC(31, 5)

  // Extract a set from branch target buffer file using index (3 bits) to update
  btbFile.io.updateReq.index := update_index
  update_set := btbFile.io.updateResp.set

  // Extract each branch details (64 bits)from within the set (128 bits)
  val update_branch1 = Wire(UInt(64.W))
  val update_branch2 = Wire(UInt(64.W))

  // Initialize signals to extract from BTB set to update
  val update_valid1 = Wire(UInt(1.W))
  val update_valid2 = Wire(UInt(1.W))

  val update_tag1 = Wire(UInt(27.W))
  val update_tag2 = Wire(UInt(27.W))

  val update_target1 = Wire(UInt(32.W))
  val update_target2 = Wire(UInt(32.W))

  val update_fsm1 = Wire(UInt(2.W))
  val update_fsm2 = Wire(UInt(2.W))

  // Branch 1 = 64 bits of MSB to update 
  // Branch 2 = 64 bits of LSB to update
  update_branch1 := update_set(127, 64)
  update_branch2 := update_set(63, 0)

  update_valid1 := update_branch1(63)
  update_valid2 := update_branch2(63)

  update_tag1 := update_branch1(62, 36)
  update_tag2 := update_branch2(62, 36)

  update_target1 := update_branch1(35, 4)
  update_target2 := update_branch2(35, 4)

  update_fsm1 := update_branch1(3, 2)
  update_fsm2 := update_branch2(3, 2)

  // Check for each branch in set
  val update_check_branch1 = Wire(Bool())
  val update_check_branch2 = Wire(Bool())
  val new_entry_check = Wire(Bool())

  // 2 Possible cases:
  // Tag exists and we only need to update
  // Tag doesnt exist and we need to add new entry in BTB File

  // Comparator + AND Gate to check if required tag exists in branch and if value is valid
  update_check_branch1 := update_valid1.asBool && (update_tag === update_tag1)
  update_check_branch2 := update_valid2.asBool && (update_tag === update_tag2)

  // Check if entry doesnt exist in either branches in a set
  // New entry = 1 else 0 if exists
  new_entry_check := !(update_check_branch1 || update_check_branch2)

  // read the LRU value for the current set
  val lru_write = Wire(UInt(1.W))
  lru_write := LRU(update_index)

  // Initialize path
  val insert_branch1 = Wire(Bool())
  val insert_branch2 = Wire(Bool())

  // Path if ta doesnt exist
  // Use LRU to decide to write in Branch1 or branch0
  insert_branch1 := Mux(new_entry_check, lru_write, 0.U).asBool
  insert_branch2 := Mux(new_entry_check, lru_write, 1.U).asBool

  val take_branch1 = Wire(Bool())
  val take_branch2 = Wire(Bool())

  // Check if branch1 or branch2 is being updated or replaced 
  // Only 1 of these will be 1
  take_branch1 := update_check_branch1 || insert_branch1
  take_branch2 := update_check_branch2 || (!insert_branch2)

  // Initialize final signals we will write in BTB
  val write_valid1 = Wire(UInt(1.W))
  val write_valid2 = Wire(UInt(1.W))

  val write_tag1 = Wire(UInt(27.W))
  val write_tag2 = Wire(UInt(27.W))

  val write_target1 = Wire(UInt(32.W))
  val write_target2 = Wire(UInt(32.W))

  val write_fsm1 = Wire(UInt(2.W))
  val write_fsm2 = Wire(UInt(2.W))

  // Valid remain 1 if it was 1 and if new value is being inserted
  write_valid1 := (update_valid1.asBool || take_branch1).asUInt
  write_valid2 := (update_valid2.asBool || take_branch2).asUInt

  // Mux to select which branch to replace tag of and which to keep as old one
  write_tag1 := Mux(insert_branch1, update_tag, update_tag1)
  write_tag2 := Mux(!insert_branch2, update_tag, update_tag2)

  // Mux to select which branch to write new/updated target into and which to keep as old one
  write_target1 := Mux(take_branch1, io.updateTarget, update_target1)
  write_target2 := Mux(take_branch2, io.updateTarget, update_target2)

  // Use the MUX to check if entry is new/replacement
  // If entry is new value then initialize it with strongNotTaken(00) before passing to FSM
  // FSM will decide on base of old value and mispredicted, the new prediction for the address
  // FSM is using dynamic 2 bit predictor
  fsm_branch1.io.currentState := Mux(new_entry_check, 0.U, update_fsm1)
  fsm_branch1.io.input        := (io.mispredicted).asBool
  write_fsm1 := fsm_branch1.io.nextState

  fsm_branch2.io.currentState := Mux(new_entry_check, 0.U, update_fsm2)
  fsm_branch2.io.input        := (io.mispredicted).asBool
  write_fsm2 := fsm_branch2.io.nextState

  // Initialize the final set which we have to replace in BTB file
  // Set is formed from concationation of all results calculated above
  val write_set = Wire(UInt(128.W))
  write_set := Cat(write_valid1, write_tag1, write_target1, write_fsm1, 0.U(2.W),
                   write_valid2, write_tag2, write_target2, write_fsm2, 0.U(2.W))

  // If update = 1 then write the updated set to address provided by updatePC 
  btbFile.io.writeReq.update := io.update
  btbFile.io.writeReq.index := update_index
  btbFile.io.writeReq.set := write_set  

  // Update the LRU value
  // If branch was inserted/replaced then see if it went into branch 1 or branch 2 and then put 0 or 1 accordingly
  // Otherwise just keep old value
  val update_lru_write = Wire(UInt(1.W))
  update_lru_write := Mux(new_entry_check, Mux(insert_branch1, 0.U, 1.U), lru_write)

  // Mask out the old bit and set the new bit in LRU
  LRU := (LRU & ~(1.U << index)) | (update_lru_read << index)
  LRU := (LRU & ~(1.U << update_index)) | (update_lru_write << update_index)
}