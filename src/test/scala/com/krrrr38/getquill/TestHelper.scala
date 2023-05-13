package com.krrrr38.getquill

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.util.control.Exception.ultimately

trait TestHelper {

  def logCapture[A](f: => A): (String, A) = {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sysOut = System.out
    val ret = ultimately(System.setOut(sysOut)) {
      System.setOut(printStream)
      Console.withOut(printStream)(f)
    }
    val logs = outputStream.toString
    println(logs)
    (logs, ret)
  }

}
