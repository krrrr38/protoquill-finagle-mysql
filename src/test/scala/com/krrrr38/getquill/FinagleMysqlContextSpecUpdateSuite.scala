package com.krrrr38.getquill

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import munit.FunSuite

import java.time.LocalDateTime
import java.util.UUID

class FinagleMysqlContextSpecUpdateSuite extends FunSuite {

  import TestContext.*
  import ctx.*

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cleanup()
  }

  test("update all") {
    val book = factory.book().value
    ctx.run(query[Book].update(_.title -> "updated")).value
    val ret = ctx
      .run(quote(query[Book].filter(_.id == lift(book.id)).map(_.title)))
      .value
    assertEquals(ret, List("updated"))
  }

  test("update with filter") {
    val book1 = factory.book().value
    val book2 = factory.book().value
    ctx
      .run(
        query[Book].filter(_.id == lift(book1.id)).update(_.title -> "updated")
      )
      .value
    val ret = ctx.run(quote(query[Book].map(b => (b.id, b.title)))).value.toMap
    assertEquals(
      ret,
      Map(
        book1.id -> "updated",
        book2.id -> book2.title
      )
    )
  }
}
