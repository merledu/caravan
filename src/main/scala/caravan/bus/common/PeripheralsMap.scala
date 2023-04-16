package caravan.bus.common

import play.api.libs.json._
import java.io.File
import java.nio.file.{Files, Paths}
import scala.io.Source
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

/*
  * GPIO and DCCM are fixed for now.
  * Future Plans: Dynamicise GPIO and DCCM as well.
  * USAGE:
    - first do `Peripherls.addValuesFromJson(/path/to/json/file)`
    - then simply get the value of each peripheral by `Peripheral.get("peripheralName")`
    - JSON format is in 'caravan/peripherals.json'
*/

object Peripherals {

  private var names: Map[String, UInt] = Map(
    "GPIO" -> 0.U,
    "DCCM" -> 1.U
  )

  // Read JSON file
  def readJsonFile(jsonFilename: String): JsValue = {
    val jsonString: String = Source.fromFile(jsonFilename).getLines.mkString
    Json.parse(jsonString)
  }

  // Parse JSON and add values to Object
  def addValuesFromJson(jsonFilename: String): Unit = {
    val json: JsValue = readJsonFile(jsonFilename)
    val peripherals: List[JsValue] = (json \ "peripherals").as[List[JsValue]]
    println(Peripherals)

    peripherals.foreach { peripheral =>
      println(peripheral)
      val name: String = (peripheral \ "name").as[String]
      val value: Int = (peripheral \ "value").as[Int]
      println(value)
      names += (name -> value.U)
      println(names)
    }
  }

  // Get the value of a peripheral by its name
  def get(value: String): UInt = names.get(value).getOrElse(0.U)
}