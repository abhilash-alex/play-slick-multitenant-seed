package dao.master

import dao.MasterDao
import models.common.Database
import models.team._
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by abhilash on 12/7/16.
  */
class ClientDao(implicit dbContext:ExecutionContext) extends MasterDao[ClientTable, Client]( Client.teamTable ){

  // implement additional when needed

  protected def getDbByCodeQuery(code: String) = {
    query.filter( _.code === code) map( x => (x.dbName, x.dbHost, x.dbPort) )
  }

  def getDbByCode(code: String, databaseName:Option[String]):Future[Option[Database]] = {
    getDb(databaseName).run(getDbByCodeQuery(code).result.headOption).map( x => x match {
      case Some(t) => Some(Database(t._1,t._2,t._3))
      case None => None
    })
  }


}

