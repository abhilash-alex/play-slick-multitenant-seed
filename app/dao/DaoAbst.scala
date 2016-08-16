package dao

import config.ConfigUtils
import models.user.User
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver

import scala.reflect.ClassTag
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend.DatabaseFactoryDef
import slick.lifted.CanBeQueryCondition

import scala.reflect._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by abhilash on 10/7/16.
  */
trait BaseEntity {
  val id: Option[Long]
}

abstract class BaseTable[E:ClassTag](tag:Tag, schemaName:Option[String], tableName:String)
  extends Table[E](tag, None, tableName){
  val classOfEntity = classTag[E].runtimeClass
  val id: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
}

sealed trait BaseTableOperations[T <: BaseTable[E], E <: BaseEntity] {
  protected def getById(id: Long, databaseName:Option[String]) : Future[Option[E]]
  protected def getAll(databaseName:Option[String]) : Future[Seq[E]]
  protected def filter[C <: Rep[_]](expr: T => C, databaseName:Option[String])(implicit wt: CanBeQueryCondition[C]): Future[Seq[E]]
  protected def save(row: E, databaseName:Option[String]) : Future[E]
  protected def deleteById(id: Long, databaseName:Option[String]) : Future[Int]
  protected def updateById(id: Long, row: E, databaseName:Option[String]) : Future[Int]
}

trait BaseTableQuery[T <: BaseTable[E], E <: BaseEntity]{
  val query: PostgresDriver.api.type#TableQuery[T]

  protected def getAllQuery = {
    query
  }

  protected def getByIdQuery(id: Long) = {
    query.filter(_.id === id)
  }

  protected def filterQuery[C <: Rep[_]](expr: T => C)(implicit wt: CanBeQueryCondition[C]) = {
    query.filter(expr)
  }

  protected def saveQuery(row: E) = {
    query returning query += row
  }

  protected def deleteByIdQuery(id: Long) = {
    query.filter(_.id === id).delete
  }

  protected def updateByIdQuery(id: Long, row: E) = {
    query.filter(_.id === id).update(row)
  }
}

sealed abstract class BaseDao[T <: BaseTable[E], E <: BaseEntity : ClassTag](clazz: TableQuery[T])(implicit dbContext:ExecutionContext) extends BaseTableQuery[T, E] with BaseTableOperations[T,E] {
  val clazzTable: TableQuery[T] = clazz
  lazy val clazzEntity = classTag[E].runtimeClass
  override val query: PostgresDriver.api.type#TableQuery[T] = clazz

  protected def getDbUrl(dbName:Option[String]):String = {
    val dbHost = "localhost" //todo
    val dbPort = "5432" //todo
    s"jdbc:postgresql://$dbHost:$dbPort/${dbName.getOrElse("master")}"
  }

  protected def getDb(databaseName:Option[String] = None): PostgresDriver.backend.DatabaseDef

  def getAll(databaseName:Option[String]): Future[Seq[E]] = {
    getDb(databaseName).run(getAllQuery.result)
  }

  def getById(id: Long, databaseName:Option[String]): Future[Option[E]] = {
    getDb(databaseName).run(getByIdQuery(id).result.headOption)
  }

  def filter[C <: Rep[_]](expr: T => C, databaseName:Option[String])(implicit wt: CanBeQueryCondition[C]) = {
    getDb(databaseName).run(filterQuery(expr).result)
  }

  def save(row: E, databaseName:Option[String]) = {
    getDb(databaseName).run(saveQuery(row))
  }

  def updateById(id: Long, row: E, databaseName:Option[String]) = {
    getDb(databaseName).run(updateByIdQuery(id, row))
  }

  def deleteById(id: Long, databaseName:Option[String]) = {
    getDb(databaseName).run(deleteByIdQuery(id))
  }
}

class MasterDao[T <: BaseTable[E], E <: BaseEntity : ClassTag](clazz: TableQuery[T])(implicit dbContext:ExecutionContext) extends BaseDao[T, E](clazz){
  override protected def getDb(databaseName: Option[String]) =
    Database.forConfig("master") //todo
}

class TenantDao[T <: BaseTable[E], E <: BaseEntity : ClassTag](clazz: TableQuery[T])(implicit dbContext:ExecutionContext) extends BaseDao[T, E](clazz){
  override protected def getDb(databaseName: Option[String]) =
    Database.forURL(getDbUrl(databaseName), "abhilash", "guessme321", driver = "org.postgresql.Driver") //todo
}