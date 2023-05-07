package com.krrrr38.getquill.context.finagle.mysql

import com.krrrr38.getquill.FinagleMysqlContext

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}
import java.util.{Date, UUID}
import com.twitter.finagle.mysql.CanBeParameter.*
import com.twitter.finagle.mysql.Parameter.wrap
import com.twitter.finagle.mysql.*
import io.getquill.MappedEncoding

trait FinagleMysqlEncoders {
  this: FinagleMysqlContext[_] =>

  type Encoder[T] = FinagleMySqlEncoder[T]

  case class FinagleMySqlEncoder[T](encoder: EncoderMethod[T])
      extends BaseEncoder[T] {
    override def apply(
        index: Int,
        value: T,
        row: PrepareRow,
        session: Session
    ) =
      encoder(index, value, row, session)
  }

  def encoder[T](f: T => Parameter): Encoder[T] =
    FinagleMySqlEncoder((index, value, row, session) => row :+ f(value))

  def encoder[T](implicit cbp: CanBeParameter[T]): Encoder[T] =
    encoder[T]((v: T) => v: Parameter)

  private[this] val nullEncoder = encoder((_: Null) => Parameter.NullParameter)

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    FinagleMySqlEncoder { (index, value, row, session) =>
      value match {
        case None    => nullEncoder.encoder(index, null, row, session)
        case Some(v) => e.encoder(index, v, row, session)
      }
    }

  implicit def mappedEncoder[I, O](implicit
      mapped: MappedEncoding[I, O],
      encoder: Encoder[O]
  ): Encoder[I] =
    FinagleMySqlEncoder(mappedBaseEncoder(mapped, encoder.encoder))

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal] { (value: BigDecimal) =>
      BigDecimalValue(value): Parameter
    }
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date] = encoder[Date] { (value: Date) =>
    timestampValue(new Timestamp(value.getTime)): Parameter
  }
  implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate] {
    (d: LocalDate) => DateValue(java.sql.Date.valueOf(d)): Parameter
  }
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    encoder[LocalDateTime] { (d: LocalDateTime) =>
      timestampValue(
        Timestamp.from(
          d.atZone(injectionTimeZone.toZoneId).toInstant
        )
      ): Parameter
    }
  implicit val uuidEncoder: Encoder[UUID] =
    mappedEncoder(MappedEncoding(_.toString), stringEncoder)
}
