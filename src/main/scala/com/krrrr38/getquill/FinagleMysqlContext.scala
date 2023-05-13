package com.krrrr38.getquill

import com.krrrr38.getquill.context.finagle.mysql.{
  FinagleMysqlDecoders,
  FinagleMysqlEncoders,
  SingleValueRow
}

import java.util.TimeZone
import scala.util.Try
import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.mysql.{
  Client,
  IsolationLevel,
  LongValue,
  NullValue,
  OK,
  Parameter,
  Row,
  TimestampValue,
  Transactions,
  Result as MysqlResult
}
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Local
import io.getquill.*
import com.typesafe.config.Config
import io.getquill.{MySQLDialect, NamingStrategy, Query, Quoted, ReturnAction}
import io.getquill.context.sql.SqlContext
import io.getquill.util.{ContextLogger, LoadConfig}
import io.getquill.util.Messages.fail
import io.getquill.context.{
  Context,
  ContextVerbStream,
  ContextVerbTranslate,
  ExecutionInfo
}

import scala.annotation.targetName

sealed trait OperationType
object OperationType {
  case object Read extends OperationType
  case object Write extends OperationType
}

class FinagleMysqlContext[+N <: NamingStrategy](
    val naming: N,
    client: OperationType => Client with Transactions,
    private[getquill] val injectionTimeZone: TimeZone,
    private[getquill] val extractionTimeZone: TimeZone
) extends Context[MySQLDialect, N]
    with ContextVerbTranslate[MySQLDialect, N]
    with SqlContext[MySQLDialect, N]
    with ContextVerbStream[MySQLDialect, N]
    with FinagleMysqlDecoders
    with FinagleMysqlEncoders {

  import OperationType._

  def this(
      naming: N,
      client: Client with Transactions,
      injectionTimeZone: TimeZone,
      extractionTimeZone: TimeZone
  ) =
    this(naming, _ => client, injectionTimeZone, extractionTimeZone)

  def this(
      naming: N,
      master: Client with Transactions,
      slave: Client with Transactions,
      timeZone: TimeZone
  ) = {
    this(
      naming,
      {
        case OperationType.Read  => slave
        case OperationType.Write => master
      },
      timeZone,
      timeZone
    )
  }

  def this(naming: N, config: FinagleMysqlContextConfig) = this(
    naming,
    config.client,
    config.injectionTimeZone,
    config.extractionTimeZone
  )
  def this(naming: N, config: Config) =
    this(naming, FinagleMysqlContextConfig(config))
  def this(naming: N, configPrefix: String) =
    this(naming, LoadConfig(configPrefix))

  def this(naming: N, client: Client with Transactions, timeZone: TimeZone) =
    this(naming, client, timeZone, timeZone)
  def this(naming: N, config: FinagleMysqlContextConfig, timeZone: TimeZone) =
    this(naming, config.client, timeZone)
  def this(naming: N, config: Config, timeZone: TimeZone) =
    this(naming, FinagleMysqlContextConfig(config), timeZone)
  def this(naming: N, configPrefix: String, timeZone: TimeZone) =
    this(naming, LoadConfig(configPrefix), timeZone)

  def this(
      naming: N,
      config: FinagleMysqlContextConfig,
      injectionTimeZone: TimeZone,
      extractionTimeZone: TimeZone
  ) = this(naming, config.client, injectionTimeZone, extractionTimeZone)
  def this(
      naming: N,
      config: Config,
      injectionTimeZone: TimeZone,
      extractionTimeZone: TimeZone
  ) = this(
    naming,
    FinagleMysqlContextConfig(config),
    injectionTimeZone,
    extractionTimeZone
  )
  def this(
      naming: N,
      configPrefix: String,
      injectionTimeZone: TimeZone,
      extractionTimeZone: TimeZone
  ) = this(
    naming,
    LoadConfig(configPrefix),
    injectionTimeZone,
    extractionTimeZone
  )

  val idiom = MySQLDialect

  private val logger = ContextLogger(classOf[FinagleMysqlContext[_]])

  override type PrepareRow = List[Parameter]
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]
  override type StreamResult[T] = Future[AsyncStream[T]]
  override type Session = Unit
  override type Runner = Unit
  override protected def context: Runner = ()
  override type TranslateRunner = Unit
  override def translateContext: TranslateRunner = ()
  override type NullChecker = LocalNullChecker
  class LocalNullChecker extends BaseNullChecker {
    override def apply(index: Int, row: Row): Boolean = {
      row.values(index) == NullValue
    }
  }
  implicit val nullChecker: LocalNullChecker = new LocalNullChecker()

  // format: off
  @targetName("runQueryDefault")
  inline def run[T](inline quoted: Quoted[Query[T]]): Future[List[T]] = InternalApi.runQueryDefault(quoted)
  @targetName("runQuery")
  inline def run[T](inline quoted: Quoted[Query[T]], inline wrap: OuterSelectWrap): Future[List[T]] = InternalApi.runQuery(quoted, wrap)
  @targetName("runQuerySingle")
  inline def run[T](inline quoted: Quoted[T]): Future[T] = InternalApi.runQuerySingle(quoted)
  @targetName("runAction")
  inline def run[E](inline quoted: Quoted[Action[E]]): Future[Long] = InternalApi.runAction(quoted)
  @targetName("runActionReturning")
  inline def run[E, T](inline quoted: Quoted[ActionReturning[E, T]]): Future[T] = InternalApi.runActionReturning[E, T](quoted)
  @targetName("runActionReturningMany")
  inline def run[E, T](inline quoted: Quoted[ActionReturning[E, List[T]]]): Future[List[T]] = InternalApi.runActionReturningMany[E, T](quoted)
  @targetName("runBatchAction")
  inline def run[I, A <: Action[I] & QAC[I, Nothing]](inline quoted: Quoted[BatchAction[A]], rowsPerBatch: Int): Future[List[Long]] = InternalApi.runBatchAction(quoted, rowsPerBatch)
  @targetName("runBatchActionDefault")
  inline def run[I, A <: Action[I] & QAC[I, Nothing]](inline quoted: Quoted[BatchAction[A]]): Future[List[Long]] = InternalApi.runBatchAction(quoted, 1)
  @targetName("runBatchActionReturning")
  inline def run[I, T, A <: Action[I] & QAC[I, T]](inline quoted: Quoted[BatchAction[A]], rowsPerBatch: Int): Future[List[T]] = InternalApi.runBatchActionReturning(quoted, rowsPerBatch)
  @targetName("runBatchActionReturningDefault")
  inline def run[I, T, A <: Action[I] & QAC[I, T]](inline quoted: Quoted[BatchAction[A]]): Future[List[T]] = InternalApi.runBatchActionReturning(quoted, 1)
  // format: on

  protected val timestampValue =
    new TimestampValue(
      injectionTimeZone,
      extractionTimeZone
    )

  override def close =
    Await.result(
      Future
        .join(
          client(Write).close(),
          client(Read).close()
        )
        .unit
    )

  private val currentClient = new Local[Client]

  def probe(sql: String) =
    Try(Await.result(client(Write).query(sql)))

  def transaction[T](f: => Future[T]) =
    client(Write).transaction { transactional =>
      currentClient.update(transactional)
      f.ensure(currentClient.clear)
    }

  def transactionWithIsolation[T](isolationLevel: IsolationLevel)(
      f: => Future[T]
  ) =
    client(Write).transactionWithIsolation(isolationLevel) { transactional =>
      currentClient.update(transactional)
      f.ensure(currentClient.clear)
    }

  def executeQuery[T](
      sql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): Future[List[T]] = {
    val (params, prepared) = prepare(Nil, ())
    logger.logQuery(sql, params)
    withClient(Read)(
      _.prepare(sql).select(prepared: _*)(row => extractor(row, ()))
    ).map(_.toList)
  }

  def executeQuerySingle[T](
      sql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): Future[T] =
    executeQuery(sql, prepare, extractor)(info, dc).map(
      handleSingleResult(sql, _)
    )

  def executeAction(
      sql: String,
      prepare: Prepare = identityPrepare
  )(info: ExecutionInfo, dc: Runner): Future[Long] = {
    val (params, prepared) = prepare(Nil, ())
    logger.logQuery(sql, params)
    withClient(Write)(_.prepare(sql)(prepared: _*))
      .map(r => toOk(r).affectedRows)
  }

  def executeActionReturning[T](
      sql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T],
      returningAction: ReturnAction
  )(info: ExecutionInfo, dc: Runner): Future[T] = {
    val (params, prepared) = prepare(Nil, ())
    logger.logQuery(sql, params)
    withClient(Write)(_.prepare(sql)(prepared: _*))
      .map(extractReturningValue(_, extractor))
  }

  def executeActionReturningMany[T](
      sql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T],
      returningAction: ReturnAction
  )(info: ExecutionInfo, dc: Runner): Future[List[T]] = {
    fail("returningMany is not supported in Finagle MySQL Context")
  }

  private val batchInsertSql = """^(INSERT .* VALUES) (\(\?(?:, \?)*\))$""".r
  def executeBatchAction(
      groups: List[BatchGroup]
  )(info: ExecutionInfo, dc: Runner): Future[List[Long]] = {
    def executeBatchInsertAction(
        sql: String,
        prepares: List[Prepare]
    ): Future[Long] = {
      val (params, prepared) = prepares.foldLeft(
        (List.empty[Any], List.empty[Parameter])
      )((acc, prepare) => {
        val (params, prepared) = prepare(Nil, ())
        (acc._1 ++ params, acc._2 ++ prepared)
      })
      logger.logQuery(sql, params)
      withClient(Write)(_.prepare(sql)(prepared: _*))
        .map(r => toOk(r).affectedRows)
    }
    def executeBatchOtherAction(
        sql: String,
        prepares: List[Prepare]
    ): Future[List[Long]] = {
      prepares
        .foldLeft(Future.value(List.newBuilder[Long])) { case (acc, prepare) =>
          acc.flatMap { list =>
            executeAction(sql, prepare)(info, dc).map(list += _)
          }
        }
        .map(_.result())
    }

    Future
      .collect {
        groups.map { case BatchGroup(sql, prepares) =>
          batchInsertSql.findFirstMatchIn(sql) match {
            case Some(m) =>
              // optimize batch insert action
              // from: INSERT sometable (col1, col2) VALUES (?, ?); INSERT sometable (col1, col2) VALUES (?, ?);
              // to: INSERT sometable (col1, col2) VALUES (?, ?), (?, ?);
              val actualSql =
                s"${m.group(1)} ${(1 to prepares.length).map(_ => m.group(2)).mkString(", ")}"
              executeBatchInsertAction(actualSql, prepares).map(List(_))
            case None =>
              executeBatchOtherAction(sql, prepares)
          }
        }
      }
      .map(_.flatten.toList)
  }

  def executeBatchActionReturning[T](
      groups: List[BatchGroupReturning],
      extractor: Extractor[T]
  )(info: ExecutionInfo, dc: Runner): Future[List[T]] =
    Future
      .collect {
        groups.map { case BatchGroupReturning(sql, column, prepare) =>
          prepare
            .foldLeft(Future.value(List.newBuilder[T])) { case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column)(
                  info,
                  dc
                ).map(list += _)
              }
            }
            .map(_.result())
        }
      }
      .map(_.flatten.toList)

  def streamQuery[T](
      fetchSize: Option[Int],
      sql: String,
      prepare: Prepare = identityPrepare,
      extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner): Future[AsyncStream[T]] = {
    val rowsPerFetch = fetchSize.getOrElse(20)
    val (params: List[Any], prepared: List[Parameter]) = prepare(Nil, ())
    logger.logQuery(sql, params)

    withClient(Read) { client =>
      client
        .cursor(sql)(rowsPerFetch, prepared: _*)(row => extractor(row, ()))
        .map(_.stream)
    }
  }

  override def prepareParams(
      statement: String,
      prepare: Prepare
  ): Seq[String] = {
    prepare(Nil, ())._2.map(param => prepareParam(param.value))
  }

  private def extractReturningValue[T](
      result: MysqlResult,
      extractor: Extractor[T]
  ) =
    extractor(SingleValueRow(LongValue(toOk(result).insertId)), ())

  protected def toOk(result: MysqlResult) =
    result match {
      case ok: OK => ok
      case error  => fail(error.toString)
    }

  def withClient[T](op: OperationType)(f: Client => T) =
    currentClient()
      .map { client =>
        f(client)
      }
      .getOrElse {
        f(client(op))
      }
}
