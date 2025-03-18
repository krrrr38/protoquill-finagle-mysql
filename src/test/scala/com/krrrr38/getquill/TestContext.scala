package com.krrrr38.getquill

import com.twitter.util.{Await, Duration, Future}
import com.typesafe.config.ConfigFactory
import io.getquill.*

import java.time.{LocalDate, LocalDateTime}
import java.util.{Date, UUID}

object TestContext {
  val dest = "127.0.0.1:3306"
  val ctx = new FinagleMysqlContext(
    SnakeCase,
    FinagleMysqlContextConfig(
      ConfigFactory.parseString(s"""user = "root"
           |database = "protoquill_test"
           |dest = "$dest"
           |""".stripMargin)
    )
  )

  case class Book(
      id: String,
      title: String,
      description: Option[String],
      ownedAt: LocalDateTime
  )
  case class Category(id: String, name: String)
  case class BookCategory(bookId: String, categoryId: String)
  case class EncoderDecoder(
      id: String,
      stringVarchar: String,
      bigDecimalDecimal: BigDecimal,
      booleanTinyint: Boolean,
      booleanBigint: Boolean,
      booleanBit: Boolean,
      intInt: Int,
      longBigint: Long,
      floatFloat: Float,
      doubleDouble: Double,
      dateTimestamp: Date,
      localDateTimestamp: LocalDate,
      localDateDate: LocalDate,
      localDateTimeTimestamp: LocalDateTime,
      localDateTimeDatetime: LocalDateTime,
      uuidVarchar: UUID
  )

  def cleanup(): Unit = {
    ctx.run(query[BookCategory].delete).value
    ctx.run(query[Category].delete).value
    ctx.run(query[Book].delete).value
    ()
  }

  implicit class RichFuture[A](f: Future[A]) {
    def value: A = Await.result(f, Duration.fromSeconds(10))
  }

  object factory {

    import TestContext.*
    import ctx.*

    def book(
        id: String = "",
        title: String = UUID.randomUUID().toString,
        description: Option[String] = None,
        ownedAt: LocalDateTime = LocalDateTime.now()
    ): Future[Book] = {
      val bookId = if (id.isEmpty) UUID.randomUUID().toString else id
      val book = Book(bookId, title, description, ownedAt)
      ctx.run(quote(query[Book].insertValue(lift(book)))).map(_ => book)
    }

    def category(
        id: String = "",
        name: String = UUID.randomUUID().toString
    ): Future[Category] = {
      val categoryId = if (id.isEmpty) UUID.randomUUID().toString else id
      val category = Category(categoryId, name)
      ctx
        .run(quote(query[Category].insertValue(lift(category))))
        .map(_ => category)
    }

    def bookCategories(
        book: Book,
        categories: List[Category]
    ): Future[Unit] = {
      val data = categories.map(c => BookCategory(book.id, c.id))
      ctx
        .run(ctx.liftQuery(data).foreach { d =>
          query[BookCategory].insertValue(d)
        })
        .map(_ => ())
    }
  }
}
