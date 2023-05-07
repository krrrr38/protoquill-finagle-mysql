package com.krrrr38.getquill

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.mysql.{IsolationLevel, Row}
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import io.getquill.context.ExecutionInfo
import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{Calendar, Date, UUID}
import scala.math.BigDecimal.RoundingMode

class FinagleMysqlContextSuite extends FunSuite {
  import TestContext.*
  import ctx.*

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cleanup()
  }

  test("probe") {
    val ret = ctx.probe("SELECT 1")
    assert(ret.isSuccess)
  }

  test("transactionWithIsolation") {
    val ret = ctx
      .transactionWithIsolation(IsolationLevel.Serializable) {
        ctx.run(query[Book])
      }
      .value
    assert(ret.isEmpty) // check no exceptions
  }

  test("executeQuery") {
    factory.book().value
    val ret =
      ctx.executeQuery("SELECT * FROM book")(ExecutionInfo.unknown, ()).value
    assertEquals(ret.length, 1)
  }

  test("executeQuerySingle") {
    val book = factory.book().value
    val ret = ctx
      .executeQuerySingle("SELECT * FROM book LIMIT 1")(
        ExecutionInfo.unknown,
        ()
      )
      .value
    assertEquals(ret.getString("id"), Some(book.id))
  }

  test("executeAction") {
    val book = factory.book().value
    val updated = ctx
      .executeAction("UPDATE book SET title = 'updated'")(
        ExecutionInfo.unknown,
        ()
      )
      .value
    assertEquals(updated, 1L)
    val ret = ctx.run(query[Book]).value
    assertEquals(ret.map(_.title), List("updated"))
  }

  test("executeActionReturning") {
    factory.book().value
    val updated = ctx
      .executeActionReturning(
        sql = "UPDATE book SET title = 'updated'",
        prepare = (p, _) => (Nil, p),
        extractor = (rr, _) => rr,
        returningAction = ReturnAction.ReturnNothing
      )(ExecutionInfo.unknown, ())
      .value
    assert(updated.fields.isEmpty)
  }

  test("executeActionReturningMany") {
    try {
      ctx
        .executeActionReturningMany(
          sql = "UPDATE book SET title = 'updated'",
          prepare = (p, _) => (Nil, p),
          extractor = (rr, _) => rr,
          returningAction = ReturnAction.ReturnNothing
        )(ExecutionInfo.unknown, ())
        .value
      fail("exception must be raised")
    } catch {
      case e: IllegalStateException =>
        assertEquals(
          e.getMessage,
          "returningMany is not supported in Finagle MySQL Context"
        )
      case e => fail(s"unknown error: $e", e)
    }
  }

  test("executeBatchAction") {
    factory.book().value
    factory.book().value
    val updated = ctx
      .executeBatchAction(
        List(
          BatchGroup(
            "UPDATE book SET title = 'updated'",
            List((p, _) => (Nil, p))
          )
        )
      )(ExecutionInfo.unknown, ())
      .value
    assert(updated.nonEmpty)
    assertEquals(updated.head, 2L)
  }

  test("executeBatchActionReturning") {
    factory.book().value
    factory.book().value
    val updated = ctx
      .executeBatchActionReturning(
        List(
          BatchGroupReturning(
            "UPDATE book SET title = 'updated'",
            ReturnAction.ReturnNothing,
            List((p, _) => (Nil, p))
          )
        ),
        (rr, _) => rr
      )(ExecutionInfo.unknown, ())
      .value
    assert(updated.nonEmpty)
  }

  test("streamQuery") {
    val book1 = factory.book().value
    val book2 = factory.book().value
    val ret = ctx
      .streamQuery(
        fetchSize = Option(1),
        sql = "SELECT * FROM book"
      )(ExecutionInfo.unknown, ())
      .value
      .foldLeft(List.empty[Row])((acc, row) => row :: acc)
      .value
    assertEquals(ret.length, 2)
    assertEquals(ret.flatMap(_.getString("id")).toSet, Set(book1.id, book2.id))
  }
}
