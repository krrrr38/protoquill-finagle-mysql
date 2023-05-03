package com.krrrr38.getquill.context.finagle.mysql

import com.twitter.finagle.mysql.{Field, Row, Value}

case class SingleValueRow(value: Value) extends Row {
  override val values: IndexedSeq[Value] = IndexedSeq(value)
  override val fields: IndexedSeq[Field] = IndexedSeq.empty
  override def indexOf(columnName: String): Option[Int] = None
}
