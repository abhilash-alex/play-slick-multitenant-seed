package models.user

import com.github.tototoshi.slick.PostgresJodaSupport._
import dao.{BaseDao, BaseEntity, BaseTable}
import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, Tag}
import slick.profile.SqlProfile.ColumnOption.{NotNull, Nullable}

/**
  * Created by abhilash on 11/7/16.
  */


object GenderUtils{
  private val genderMap:Map[Int, Gender] = Map(
    1 -> Male,
    2 -> Female,
    3 -> Other
  )
  def forId(id:Int) : Gender = genderMap.get(id).getOrElse(Other) // todo fix this.

  implicit val mappedTypeMapper = MappedColumnType.base[Gender,Int](
    g => g.id,
    i => forId(i)
  )

  object GenderReads extends Reads[Gender] {
    override def reads(json: JsValue): JsResult[Gender] =
      (json \ "id").asOpt[Int] match {
        case Some(x) => JsSuccess(forId(x))
        case None => JsError(ValidationError("No id value found in gender json object."))
      }
  }

  object GenderWrites extends Writes[Gender] {
    override def writes(o: Gender): JsValue = Json.obj(
      "id" -> o.id,
      "n" -> o.prettyName
    )
  }

  implicit val genderFormats = Format(GenderReads,GenderWrites)
}
sealed trait Gender{
  val id:Int
  val prettyName:String
}
case object Male extends Gender{
  val id:Int = 1
  val prettyName:String = "Male"
}
case object Female extends Gender{
  val id:Int = 2
  val prettyName:String = "Female"
}
case object Other extends Gender{
  val id:Int = 3
  val prettyName:String = "Other"
}

case class User(id:Option[Long], username:String, password:String, email:Option[String], emailVerified:Option[Boolean], phone:String,
                phoneVerified:Option[Boolean], firstName:String, lastName:String, uid:String, gender:Gender, avatar:String,
                createdBy:Option[Long], createdOn:DateTime = DateTime.now(), updatedBy: Option[Long],
                updatedOn: Option[DateTime], isEnabled: Boolean) extends BaseEntity
object User{
  val userTable = TableQuery[UserTable]
}

case class UserRole(id:Option[Long], userId:Long, role: ClientRole, createdBy:Option[Long], createdOn:DateTime  = DateTime.now()) extends BaseEntity
object UserRole{
  val userRoleTable = TableQuery[UserRoleTable]
}

class UserTable(tag:Tag) extends BaseTable[User](tag, None, "USER") {

  import GenderUtils.mappedTypeMapper

  def username = column[String]("USERNAME", O.Length(65, varying = true), NotNull) //unique
  def password = column[String]("PASSWORD", O.Length(20, varying = true), NotNull)
  def email = column[Option[String]]("EMAIL", O.Length(45, varying = true), Nullable) // unique
  def emailVerified = column[Option[Boolean]]("EMAIL_VERIFIED", Nullable)
  def phone = column[String]("PHONE", O.Length(18, varying = true), NotNull) //unique
  def phoneVerified = column[Option[Boolean]]("PHONE_VERIFIED", Nullable)

  def firstName = column[String]("FIRST_NAME", O.Length(65, varying = true), NotNull)
  def lastName = column[String]("LAST_NAME", O.Length(20, varying = true), NotNull)
  def uid = column[String]("UID", O.Length(20, varying = true), NotNull)
  def gender = column[Gender]("GENDER", NotNull)
  def avatar = column[String]("AVATAR", Nullable)

  def createdBy = column[Option[Long]]("CREATED_BY", Nullable)
  def createdOn = column[DateTime]("CREATED_ON", Nullable)
  def updatedBy = column[Option[Long]]("UPDATED_BY", Nullable)
  def updatedOn = column[Option[DateTime]]("UPDATED_ON", Nullable)
  def isEnabled = column[Boolean]("IS_ENABLED", NotNull)


  // constraints
    // foreign keys
  def createdOnFk = foreignKey("USER_CR_FK", createdBy, User.userTable)(_.id.?)
  def updatedOnFk = foreignKey("USER_UP_FK", updatedBy, User.userTable)(_.id.?)
    // unique
  def uniqueUsername = index("user_uniqueUsername", username, unique=true)
  def uniquePhone = index("user_uniquePhone", phone, unique=true)
  def uniqueEmail = index("user_uniqueEmail", email, unique=true)

  override def * =
    (id.?, username, password, email, emailVerified, phone, phoneVerified, firstName, lastName, uid, gender,
      avatar, createdBy, createdOn, updatedBy, updatedOn, isEnabled) <> ((User.apply _).tupled, User.unapply)
}

/*
* id:Option[Long], userId:Long, role: Role, createdBy:Option[Long], createdOn:DateTime  = DateTime.now()) extends BaseEntity
*
* */

class UserRoleTable(tag:Tag) extends BaseTable[UserRole](tag, None, "USER_ROLE") {

  import RoleUtils.mappedTypeMapper

  def userId = column[Long]("USER_ID", NotNull)
  def role = column[ClientRole]("ROLE", NotNull)
  def createdBy = column[Option[Long]]("CREATED_BY", Nullable)
  def createdOn = column[DateTime]("CREATED_ON", Nullable)

  // constraints
    // foreign keys
  def createdOnFk = foreignKey("USER_ROLE_CR_FK", createdBy, User.userTable)(_.id.?)

  override def *  : ProvenShape[UserRole]=
    (id.?, userId, role, createdBy, createdOn) <> ((UserRole.apply _).tupled, UserRole.unapply)
}