package com.krrrr38.getquill

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import munit.FunSuite

import java.time.LocalDateTime
import java.util.UUID

class FinagleMysqlContextSpecInsertSuite extends FunSuite with TestHelper {

  import TestContext.*
  import ctx.*

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cleanup()
  }

  test("insert single") {
    val bookId = UUID.randomUUID().toString
    val book = Book(bookId, "title", Some("desc"), LocalDateTime.now())
    val ret = ctx.run(quote(query[Book].insertValue(lift(book)))).value
    assertEquals(ret, 1L)
  }

  test("insert bulk") {
    val count = 2
    val books = (0 until count).map(i => {
      val bookId = UUID.randomUUID().toString
      Book(bookId, s"title-$i", Some("desc-$i"), LocalDateTime.now())
    })
    val (logs, ret) = logCapture {
      ctx
        .run(ctx.liftQuery(books).foreach { d =>
          query[Book].insertValue(d)
        })
        .value
    }
    assertEquals(ret.sum, count.toLong)
    assert(
      logs.contains(
        "INSERT INTO book (id,title,description,owned_at) VALUES (?, ?, ?, ?), (?, ?, ?, ?)"
      )
    )
  }
}
