// package tileink
// import caravan.bus.tileink.{TilelinkConfig}
// import chisel3._
// import org.scalatest._
// import chiseltest._
// import chiseltest.ChiselScalatestTester
// import chiseltest.internal.VerilatorBackendAnnotation
// import chiseltest.experimental.TestOptionBuilder._
// import org.scalatest.FreeSpec

// trait MemoryDumpFileHelper { self: FreeSpec with ChiselScalatestTester =>
//   def getFile: Option[String] = {
//     if (scalaTestContext.value.get.configMap.contains("memFile")) {
//       Some(scalaTestContext.value.get.configMap("memFile").toString)
//     } else {
//       None
//     }
//   }
// }