package models.team

import com.github.tototoshi.slick.PostgresJodaSupport._
import dao.{BaseEntity, BaseTable}
import models.common.Database
import models.user.User
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import slick.profile.SqlProfile.ColumnOption.{NotNull, Nullable}

/**
  * Created by abhilash on 12/7/16.
  */
case class Client(id:Option[Long], name:String, code:String, description:String, email:String, db:Database, createdBy: Long,
                  createdOn: DateTime = DateTime.now, updatedBy:Option[Long], updatedOn: Option[DateTime], isEnabled:Boolean) extends BaseEntity

object Client {
  val teamTable = TableQuery[ClientTable]
}

class ClientTable(tag:Tag) extends BaseTable[Client](tag, Some("team"), "TEAM") {

  def name = column[String]("NAME", O.Length(75, varying = true), NotNull)
  def code = column[String]("CODE", O.Length(75, varying = true), NotNull) //unique
  def description = column[String]("DESCRIPTION", O.Length(150, varying = true), NotNull)
  def email = column[String]("EMAIL", NotNull)

  def dbName = column[String]("DB_NAME", NotNull)
  def dbHost = column[String]("DB_HOST", NotNull)
  def dbPort = column[String]("DB_PORT", NotNull)

  def createdBy = column[Long]("CREATED_BY", NotNull)
  def createdOn = column[DateTime]("CREATED_ON", NotNull)
  def updatedBy = column[Option[Long]]("UPDATED_BY", Nullable)
  def updatedOn = column[Option[DateTime]]("UPDATED_ON", Nullable)
  def isEnabled = column[Boolean]("IS_ENABLED", NotNull)

  // constraints
  // foreign keys
  def createdByFk = foreignKey("TEAM_CR_FK", createdBy, User.userTable)(_.id)
  def updatedByFk = foreignKey("TEAM_UP_FK", updatedBy, User.userTable)(_.id.?)
  // unique
  def uniqueName = index("team_uniqueCode", code, unique=true)

  override def * =
    (id.?, name, code, description, email, (dbName, dbHost, dbPort), createdBy, createdOn, updatedBy, updatedOn, isEnabled).shaped.<> (
        {
          case (id, name, code ,description, email, db, createdBy, createdOn, updatedBy, updatedOn, isEnabled) =>
            Client(id, name, code, description, email, Database.tupled.apply(db), createdBy, createdOn, updatedBy, updatedOn, isEnabled)
        },
        {
          t: Client =>
            Some( (t.id, t.name, t.code, t.description, t.email, Database.unapply(t.db).get, t.createdBy, t.createdOn, t.updatedBy, t.updatedOn, t.isEnabled))
        }
      )
}