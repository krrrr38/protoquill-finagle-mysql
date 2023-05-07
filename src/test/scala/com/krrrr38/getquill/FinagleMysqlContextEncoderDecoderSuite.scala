package com.krrrr38.getquill

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.*
import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{Calendar, Date, UUID}
import scala.math.BigDecimal.RoundingMode

class FinagleMysqlContextEncoderDecoderSuite extends ScalaCheckSuite {
  // override def scalaCheckInitialSeed = "change me for debugging"

  import TestContext.*
  import ctx._

  override def beforeAll(): Unit = {
    super.beforeAll()
    ctx.run(query[EncoderDecoder].delete).value
  }

  val valueGen = for {
    string <- arbitrary[String].suchThat(_.length < 255)
    bigDecimal <- arbitrary[BigDecimal]
      .suchThat(_.abs < BigDecimal("9999999999999999"))
      .map(_.setScale(9, RoundingMode.FLOOR))
    boolean <- arbitrary[Boolean]
    int <- arbitrary[Int]
    long <- arbitrary[Long]
    float <- arbitrary[Float]
    double <- arbitrary[Double]
    ldt <- arbitrary[LocalDateTime]
    // mysql DATETIME(6) doesn't support nano
    localDateTime =
      if (ldt.getYear < 1970 || ldt.getYear > 3000)
        ldt.withYear(2000).withNano(0)
      else ldt.withNano(0)
    localDate = localDateTime.toLocalDate
    date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant)
    uuid <- arbitrary[UUID]
  } yield EncoderDecoder(
    id = UUID.randomUUID().toString,
    stringVarchar = string,
    bigDecimalDecimal = bigDecimal,
    booleanTinyint = boolean,
    booleanBigint = boolean,
    booleanBit = boolean,
    intInt = int,
    longBigint = long,
    floatFloat = float,
    doubleDouble = double,
    dateTimestamp = date,
    localDateTimestamp = localDate,
    localDateDate = localDate,
    localDateTimeTimestamp = localDateTime,
    localDateTimeDatetime = localDateTime,
    uuidVarchar = uuid
  )

  property("encode and decode success") {
    forAll(valueGen) { (value: EncoderDecoder) =>
      val inserted =
        ctx.run(query[EncoderDecoder].insertValue(lift(value))).value
      assertEquals(inserted, 1L)
      val ret =
        ctx.run(query[EncoderDecoder].filter(_.id == lift(value.id))).value
      assertEquals(ret, List(value))
    }
  }
}
