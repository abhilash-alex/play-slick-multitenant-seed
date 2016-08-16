package models.common

import com.fasterxml.jackson.annotation.JsonIgnore
import dao.{BaseEntity, BaseTable}
import org.joda.time.DateTime
import slick.lifted.ProvenShape
import slick.profile.SqlProfile.ColumnOption.{NotNull, Nullable}
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.user.User
import play.api.libs.json.{JsValue, Json, Writes}
import slick.driver.PostgresDriver.api._

/**
  * Created by abhilash on 12/7/16.
  */
case class Database(dbName:String, dbHost:String, dbPort:String)

sealed trait ErrorType
case object Unauthorised extends ErrorType
case object Validation extends ErrorType
case object NotFound extends ErrorType
case object BadRequest extends ErrorType

case class AppError(message:String, code:String, errType:ErrorType)
object AppError{

  implicit object nbcErrorWrites extends Writes[AppError] {
    override def writes(o: AppError): JsValue = Json.obj(
      "c" -> o.code,
      "m" -> o.message
    )
  }
}



