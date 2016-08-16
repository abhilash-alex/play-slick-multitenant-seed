package models.rest

import dao.BaseEntity
import models.common.{GeoCode, AppError$, Validation}
import models.location.LocationType
import models.user.{Gender, ClientRole}
import org.joda.time.DateTime
import play.api.libs.json.Json

import scalaz.{NonEmptyList, Validation, ValidationNel}
import scalaz.syntax.validation._
import scalaz.syntax.apply._
import scala.util.matching.Regex

/**
  * Created by abhilash on 21/7/16.
  */

sealed trait RestEntity{
  val id:Option[Long]
//  def validate:ValidationNel[NBCError, RestEntity]

  type NBCValidation[T] = ValidationNel[AppError, T]
  val emailRegex:Regex = "^[_A-Za-z0-9-\\\\+]+(\\\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\\\.[A-Za-z0-9]+)*(\\\\.[A-Za-z]{2,})$".r
  val phoneRegex:Regex = "^(\\+)*([0-9]{10,15})$".r

  def validateEmailStr [E <: RestEntity](s:String, elem:E):ValidationNel[AppError, E] =
    emailRegex.findFirstIn(s).map{ x=> elem.success}.getOrElse( AppError(s"The email specified $s is invalid.","403",Validation).failureNel )

  def validatePhoneStr [E <: RestEntity](s:String, elem:E):ValidationNel[AppError, E] =
    phoneRegex.findFirstIn(s).map{ x=> elem.success}.getOrElse( AppError(s"The phone number provided $s is invalid.","403",Validation).failureNel )

  def validateId[E <: RestEntity](l:Long, elemStr:String, elem:E):ValidationNel[AppError, E] = if(l > 0) elem.success else  AppError(s"Invalid ID for element $elemStr.","403",Validation).failureNel

  def validateLengthAbove[E <: RestEntity](s:String, l:Int, n:String, elem:E):ValidationNel[AppError, E] =
    if (s.length > l)
      elem.success
    else
      AppError(s"$n is too short.","403",Validation).failureNel

  def validateLengthBelow[E <: RestEntity](s:String, l:Int, n:String, elem:E):ValidationNel[AppError, E] =
    if (s.length < l)
      elem.success
    else
      AppError(s"$n is too lost.","403",Validation).failureNel

  /* createdBy:UserMeta, createdOn:DateTime, updatedBy: Option[UserMeta], updatedOn: Option[DateTime] */

  def validateUserMeta[E <: RestEntity](u:Option[UserMeta], elemStr:String, elem:E) =
    u match {
      case Some(u) => (validateId[elem.type](u.id,elemStr + " UserMeta",elem) |@| validateLengthAbove[elem.type](u.name,3,elemStr+" UserMeta",elem)).tupled
      case None => elem.success
    }

  def validatedUpdatedOn[E <: RestEntity](u:Option[UserMeta], c:DateTime, elem:E, elemStr:String) =
    u match {
      case Some(u) => (validateId[elem.type](u.id,elemStr + " UserMeta",elem) |@| validateLengthAbove[elem.type](u.name,3,elemStr+" UserMeta",elem)).tupled
      case None => elem.success
    }

  def validateCreatedBy[E <: RestEntity](createdBy:UserMeta, eStr:String, elem:E) = validateUserMeta[E](Some(createdBy), eStr, elem)

  def validateUpdatedBy[E <: RestEntity](updatedBy:Option[UserMeta], eStr:String, elem:E) = validateUserMeta[E](updatedBy, "Created By", elem)

}

sealed trait TableRestEntity{
  val id:Option[Long]
}

case class UserMeta(id:Long, name:String)
object UserMeta{
  implicit val userMetaFormats = Json.format[UserMeta]
}




















/*
*
* case class User(id:Option[Long], username:String, password:String, email:Option[String], emailVerified:Option[Boolean], phone:String,
                phoneVerified:Option[Boolean], createdBy:Option[Long], createdOn:DateTime = DateTime.now(), updatedBy: Option[Long],
                updatedOn: Option[DateTime], isEnabled: Boolean) extends BaseEntity

    def firstName = column[String]("FIRST_NAME", O.Length(65, varying = true), NotNull)
  def lastName = column[String]("LAST_NAME", O.Length(20, varying = true), NotNull)
  def uid = column[String]("UID", O.Length(20, varying = true), NotNull)
  def gender = column[Gender]("GENDER", NotNull)
  def avatar = column[String]("AVATAR", Nullable)

*
*
* */
case class UserRest(id:Option[Long], username:String, role:ClientRole, email:Option[String], emailVerified:Option[Boolean], phone:String,
                    phoneVerified:Option[Boolean], firstName:String, lastName:String, uid:String, gender:Gender,
                    avatar:Option[String], createdBy:UserMeta, createdOn:DateTime, updatedBy: Option[UserMeta],
                    updatedOn: Option[DateTime], isEnabled: Boolean) extends RestEntity {

  // if(tktNo.isEmpty || tktNo.get.length > 6) this.successNel[Ticket] else NBCError("Invalid ticket no", "V001", NbcValidation).failureNel[NBCError]



  private def validateUsername = validateLengthAbove[UserRest](username, 6, "User Name", this)
  private def validateEmail = email.map{validateEmailStr[UserRest](_, this)} getOrElse this.success
   //verification flag must be specified if email is defined
  private def validateEmailVerifyFlag = email.map{
      x => emailVerified.map{ x => this.success }
        .getOrElse(AppError("Email verification flag unset for defined email value", "402", Validation).failureNel)
    }.getOrElse(this.success)
  private def validatePhone = validatePhoneStr[UserRest](phone, this)
   //verification flag must be specified if phone is defined
  private def validatePhoneVerifyFlag =
      phoneVerified.map{ x => this.success }
        .getOrElse(AppError("Phone verification flag unset.", "402", Validation).failureNel)
  private def validateFirstName = (validateLengthAbove[UserRest](firstName, 2, "First Name", this) |@| validateLengthBelow[UserRest](firstName, 40, "First Name", this)).tupled
  private def validateLastName = if(lastName.length > 2) validateLengthBelow[UserRest](firstName, 40, "First Name", this) else this.success
  private def validateUId = validateLengthAbove[UserRest](uid, 5, "UID", this)
  private def validateAvatar = if(avatar.isDefined) (validateLengthAbove[UserRest](avatar.get, 5, "Avatar", this) |@| validateLengthBelow[UserRest](avatar.get, 75, "Avatar", this)).tupled else this.success

  def validate:ValidationNel[AppError, UserRest] = {
    (validateUsername |@| validateEmail |@| validateEmailVerifyFlag |@| validatePhone |@| validatePhoneVerifyFlag |@| validateFirstName |@| validateLastName |@| validateUId |@| validateAvatar |@| validateCreatedBy[UserRest](createdBy, "Created By", this) |@| validateUpdatedBy[UserRest](updatedBy, "Updated By", this)).tupled/
  }
}
object UserRest{
  import models.user.GenderUtils.genderFormats
  import models.user.RoleUtils.clientRoleFormats

  implicit val userRestFormats = Json.format[UserRest]
}

case class UserTableRest(id: Option[Long], username:String, firstName:String, lastName:String, role:String,
                         avatar:String, currentTickets:Option[Int], lastActive:Option[DateTime]) extends TableRestEntity
object UserTableRest{
  implicit val userTableRestFormats = Json.format[UserTableRest]
}

