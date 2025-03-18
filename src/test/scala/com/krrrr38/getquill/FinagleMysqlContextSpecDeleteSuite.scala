package com.krrrr38.getquill

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import munit.FunSuite

import java.time.LocalDateTime
import java.util.UUID

class FinagleMysqlContextSpecDeleteSuite extends FunSuite {

  import TestContext.*
  import ctx.*

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cleanup()
  }

  test("delete all") {
    val book = factory.book().value
    ctx.run(query[Book].delete).value
    val ret = ctx.run(query[Book]).value
    assert(ret.isEmpty)
  }

  test("delete with filter") {
    val book1 = factory.book().value
    val book2 = factory.book().value
    ctx.run(query[Book].filter(_.id == lift(book1.id)).delete).value
    val ret = ctx.run(query[Book]).value
    assertEquals(ret, List(book2))
  }
}
