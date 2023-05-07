package com.krrrr38.getquill

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import munit.FunSuite

import java.time.LocalDateTime
import java.util.UUID

class FinagleMysqlContextSpecSelectSuite extends FunSuite {

  import TestContext.*
  import ctx.*

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cleanup()
  }

  test("select all") {
    (for {
      _ <- factory.book()
      _ <- factory.book()
    } yield ()).value
    val allBooks = ctx.run(quote(query[Book])).value
    assertEquals(allBooks.length, 2)
  }

  test("select by title") {
    val book2 = (for {
      _ <- factory.book()
      book2 <- factory.book()
    } yield book2).value
    val allBooks = ctx
      .run(quote {
        query[Book].filter(_.title == lift(book2.title))
      })
      .value
    assertEquals(allBooks, List(book2))
  }

  test("select with order/limit") {
    val (id1, id2, id3, id4) = ("id1", "id2", "id3", "id4")
    val books = (for {
      book2 <- factory.book(id = id2)
      _ <- factory.book(id = id1)
      _ <- factory.book(id = id4)
      book3 <- factory.book(id = id3)
    } yield (book2, book3)).value
    val (book2, book3) = books
    val allBooks = ctx
      .run(quote {
        query[Book].sortBy(_.id).drop(1).take(2)
      })
      .value
    assertEquals(allBooks, List(book2, book3))
  }

  test("select from multiple tables") {
    val (b1, b2, c1, c2) = (for {
      b1 <- factory.book()
      b2 <- factory.book()
      c1 <- factory.category()
      c2 <- factory.category()
      _ <- factory.bookCategories(b1, List(c1, c2))
      _ <- factory.bookCategories(b2, List(c2))
    } yield (b1, b2, c1, c2)).value

    def categoryBookIds(categoryName: String): Set[String] = {
      val books = ctx
        .run(quote {
          for {
            b <- query[Book]
            c <- query[Category]
            bc <- query[BookCategory]
            if b.id == bc.bookId
            if c.id == bc.categoryId
            if c.name == lift(categoryName)
          } yield b
        })
        .value
      books.map(_.id).toSet
    }

    assertEquals(categoryBookIds(c1.name), Set(b1.id))
    assertEquals(categoryBookIds(c2.name), Set(b1.id, b2.id))
  }

  test("select join") {
    val (b1, b2, c1, c2) = (for {
      b1 <- factory.book()
      b2 <- factory.book()
      c1 <- factory.category()
      c2 <- factory.category()
      _ <- factory.bookCategories(b1, List(c1, c2))
      _ <- factory.bookCategories(b2, List(c2))
    } yield (b1, b2, c1, c2)).value

    def categoryBookIds(categoryName: String): Set[String] = {
      val bookIds = ctx
        .run(quote {
          query[Book]
            .join(query[BookCategory])
            .on((b, bc) => b.id == bc.bookId)
            .join(query[Category])
            .on((r, c) => r._2.categoryId == c.id)
            .filter((_, c) => c.name == lift(categoryName))
            .map(_._1._1.id)
        })
        .value
      bookIds.toSet
    }

    assertEquals(categoryBookIds(c1.name), Set(b1.id))
    assertEquals(categoryBookIds(c2.name), Set(b1.id, b2.id))
  }

  // FIXME https://github.com/zio/zio-protoquill/issues/268
  //  test("aggregation subquery") {
  //    val now = LocalDateTime.now()
  //    val data = (for {
  //      b1 <- factory.book(ownedAt = now)
  //      b2 <- factory.book(ownedAt = now.plusMinutes(1L))
  //      b3 <- factory.book(ownedAt = now.plusMinutes(2L))
  //      c1 <- factory.category()
  //      c2 <- factory.category()
  //      _ <- factory.bookCategories(b1, List(c1))
  //      _ <- factory.bookCategories(b2, List(c1, c2))
  //      _ <- factory.bookCategories(b3, List(c2))
  //    } yield (b1, b2, b3, c1, c2)).value
  //    val (b1, b2, b3, c1, c2) = data
  //    // select bc.category_id, MAX(b.owned_at)
  //    // from book_category bc
  //    // join book b on bc.book_id = b.id
  //    // group by bc.category_id
  //    inline def aggQuery = query[BookCategory]
  //      .join(query[Book])
  //      .on((bc, b) => bc.bookId == b.id)
  //      .groupByMap(r => r._1.categoryId)(r => (r._1.categoryId, max(r._2.ownedAt)))
  //    val ret = ctx.run(quote(aggQuery)).value
  //    assert(ret.nonEmpty)
  //
  //    // select category.name, book.id
  //    // from book, agg_query, category
  //    // where book.owned_at = agg_query.owned_at
  //    //   and category.id = agg_query.category_id
  //    val result = ctx.run(quote {
  //      for {
  //        b <- query[Book]
  //        agg <- aggQuery
  //        c <- query[Category]
  //        if b.ownedAt == agg._2
  //        if c.id == agg._1
  //      } yield (c.name, b.id)
  //    }).value.toMap
  //    assertEquals(result.size, 2)
  //    assertEquals(result.get(c1.name), b2.id)
  //    assertEquals(result.get(c2.name), b3.id)
  //  }
}
