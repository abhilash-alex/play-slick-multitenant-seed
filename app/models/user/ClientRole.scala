package models.user

import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, Json, Writes, _}
import slick.driver.PostgresDriver.api._
/**
  * Created by abhilash on 11/7/16.
  */
/*
*
* All master/team agnostic roles go here
*
* */
object RoleUtils{
  private val roleMap:Map[Int, ClientRole] = Map(
    0 -> Admin,
    1 -> L1,
    2 -> L2
  )
  def forId(id:Int) : ClientRole = roleMap.get(id).getOrElse(L1) // todo fix this.

  implicit val mappedTypeMapper = MappedColumnType.base[ClientRole,Int](
    r => r.id,
    i => forId(i)
  )

  object ClientRoleReads extends Reads[ClientRole] {
    override def reads(json: JsValue): JsResult[ClientRole] =
      (json \ "id").asOpt[Int] match {
        case Some(x) => JsSuccess(forId(x))
        case None => JsError(ValidationError("No id value found in client role json object."))
      }
  }

  object ClientRoleWrites extends Writes[ClientRole] {
    override def writes(o: ClientRole): JsValue = Json.obj(
      "id" -> o.id,
      "n" -> o.prettyName
    )
  }

  implicit val clientRoleFormats = Format(ClientRoleReads,ClientRoleWrites)
}

sealed trait ClientRole{
  val id: Int
  val prettyName:String
}

case object Admin extends ClientRole{
  val id = 0
  val prettyName:String = "Admin"

}
case object L1 extends ClientRole{
  val id = 1
  val prettyName:String = "L1"

}
case object L2 extends ClientRole{
  val id = 2
  val prettyName:String = "L2"
}