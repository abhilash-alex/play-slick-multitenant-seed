package controllers

import dao.client._
import dao.master.{AreaDao, StateDao, ClientDao, WardDao}
import models.common._
import models.rest.{RestEntity, TableRestEntity}
import models.ticket.TicketDelta
import models.user.{Admin, L1, ClientRole, User}
import models.web.NBCAuthenticatedRequest
import play.api.{Configuration, Logger}
import play.api.mvc.{Action, Controller, Result}
import play.api.mvc.Results._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.parsing.json.JSONObject

/**
  * Created by abhilash on 26/7/16.
  *
  *
  * todo inject class with all DAO's in application
  *
  */
abstract class BaseRestController[E <: RestEntity, T <: TableRestEntity](logger:Logger, config:Configuration)
                    (implicit dbContext:ExecutionContext, eFormats:Format[E], eListFormats:Format[List[T]]) extends Controller{

  case class GetRequest(id:Long)
  implicit val getRequestFormats = Json.format[GetRequest]
  case class PutRequest(data:E)
  implicit val putRequestFormats = Json.format[PutRequest]
  case class PostRequest(id:Long, data:E)
  implicit val postRequestFormats = Json.format[PostRequest]
  case class DeleteRequest(id:Long)
  implicit val deleteRequestFormats = Json.format[DeleteRequest]
  case class DeleteManyRequest(ids:List[Long])
  implicit val deleteManyRequestFormats = Json.format[DeleteManyRequest]
  case class GetManyRequest(query:Option[String] = None, page:Int, offset:Int)
  implicit val getManyRequestFormats = Json.format[GetManyRequest]

  abstract class Response {
    val isSuccess: Boolean
    val c:Option[String]
    val m:Option[String]
  }
  object responseWrites extends  Writes[Response] {
    override def writes(o: Response): JsValue = Json.obj(
      "isSuccess" -> o.isSuccess,
      "c" -> o.c,
      "c" -> o.m
    )
  }

  // masters daos
  val teamDao:ClientDao = new dao.master.ClientDao
  val areaDao:AreaDao = new dao.master.AreaDao
  val stateDao:StateDao = new dao.master.StateDao
  val wardDao:WardDao = new dao.master.WardDao

  // tenant daos
  val userDao:UserDao = new dao.client.UserDao
  val userRoleDao:UserRoleDao = new dao.client.UserRoleDao
  val ticketDao:TicketDao = new dao.client.TicketDao
  val ticketCommentDao:TicketCommentDao = new dao.client.TicketCommentDao
  val ticketDeltaDao:TicketDeltaDao = new dao.client.TicketDeltaDao
  val ticketAreaDao:TicketAreaDao = new dao.client.TicketAreaDao



  case class GetResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[E]) extends Response
  implicit val getResponseFormats = Json.format[GetResponse]
  case class PutResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[E]) extends Response
  implicit val putResponseFormats = Json.format[PutResponse]
  case class PostResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[E]) extends Response
  implicit val postResponseFormats = Json.format[PostResponse]
  case class DeleteResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[Long]) extends Response
  implicit val deleteResponseFormats = Json.format[DeleteResponse]
  case class GetManyResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[List[T]], cnt:Option[Int]) extends Response
  implicit val getManyResponseFormats = Json.format[GetManyResponse]
  case class DeleteManyResponse(isSuccess:Boolean, c:Option[String], m:Option[String], d:Option[List[Long]]) extends Response
  implicit val deleteManyResponseFormats = Json.format[DeleteManyResponse]

  val elementName:String
  // validate user permissions before running any operation

  // check if user has perm to fetch given record, only admin and l1 can.
  // l2 can only view own (assigned & created by) records

  def fetchById(id:Long, reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError, E]]

  def create(e:E, reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError,E] ] //perm will be checked at controller level

  def update(e:E, reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError, E]]

  def delete(id:Long, reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError,Long]]

  def fetchPaginated(query:Option[String] = None, page:Int, offset:Int, reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError, (List[T], Int)]]

  def deleteMany(ids:List[Long], reqUser:Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None):Future[Either[AppError,List[Long]]]


  protected def resultForError(body:Response, error:AppError):Result = {
    import AppError._
    error.errType match {
      case Unauthorised => Unauthorized(Json.toJson(body)(responseWrites))
      case NotFound => NotFound(Json.toJson(body)(responseWrites))
      case BadRequest => BadRequest(Json.toJson(body)(responseWrites))
    }
  }

  def get = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[GetRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to fetch element $elementName for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        fetchById(body.id, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(GetResponse(true, None, None, Some(res))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(GetResponse(false, Some(err.code), Some(err.message), None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(GetResponse(false, Some(err.code), Some(err.message), None), err))
    }
  }

  def put = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[PutRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to create element $elementName for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        create(body.data, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(PutResponse(true, None, None, Some(res))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(PutResponse(false, Some(err.code), Some(err.message), None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(PutResponse(false, Some(err.code), Some(err.message), None), err))
    }
  }

  def post = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[PostRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to update element $elementName with id ${body.id} for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        update(body.data, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(PostResponse(true, None, None, Some(res))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(PostResponse(false, Some(err.code), Some(err.message), None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(PostResponse(false, Some(err.code), Some(err.message), None), err))
    }
  }

  def deleteSingle = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[DeleteRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to delete element $elementName with id ${body.id} for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        delete(body.id, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(DeleteResponse(true, None, None, Some(res))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(DeleteResponse(false, Some(err.code), Some(err.message), None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(DeleteResponse(false, Some(err.code), Some(err.message), None), err))
    }
  }

  def deleteMany: Action[JsValue] = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[DeleteManyRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to delete many elements $elementName with ids ${body.ids} for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        deleteMany(body.ids, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(DeleteManyResponse(true, None, None, Some(res))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(DeleteManyResponse(false, Some(err.code), Some(err.message), None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(DeleteManyResponse(false, Some(err.code), Some(err.message), None), err))
    }
  }

  def getMany = AuthenticatedAction.async(parse.json){ request =>
    request.body.asOpt[GetManyRequest] match {
      case Some(body) =>
        logger.debug(s"Got request to fetch paginated elements $elementName with query ${body.query}, page ${body.page} " +
          s"and offset ${body.offset} for user id ${request.clientToken.userId} and for team id ${request.clientToken.clientCode}")
        fetchPaginated(body.query, body.page, body.offset, request.clientToken.userId, request.clientToken.clientCode) map { x =>
          x match {
            case Right(res) => Ok(Json.toJson(GetManyResponse(true, None, None, Some(res._1), Some(res._2))))
            case Left(err) =>
              logger.error(Json.toJson(err).toString())
              resultForError(GetManyResponse(false, Some(err.code), Some(err.message), None, None), err)
          }
        }
      case None =>
        val err = AppError("Malformed request body", "400", BadRequest)
        logger.error(Json.toJson(err).toString())
        Future.successful(resultForError(GetManyResponse(false, Some(err.code), Some(err.message), None, None), err))
    }
  }
}




class TicketRestController(logger:Logger, config:Configuration)
                          (implicit executionContext:ExecutionContext, eFormats:Format[TicketRest], eListFormats:Format[List[TicketTableRest]])
  extends BaseRestController[TicketRest, TicketTableRest](logger:Logger, config:Configuration){

  override val elementName: String = "Ticket"


  private def generateDelta(old:TicketRest, updated:TicketRest):List[TicketDelta] = ???

  /*
  * Future begin
  * get team's db name for teamId, team service
  * get request user , UserRest fetchById
  * check role permission of user.
  * validate model : ticket, ticket comments
  * old : fetch rest entity by id, fetchById
  * generate deltas : if empty list, no changes to update.
  * start transaction
   * update ticket table by id, full row
   * update ticket comment table, create for those with None id. no updating old comments for now.
   * add delta's to delta table
  * end transaction
  * return updated model
  * Future end
  * */
  override def update(e: TicketRest, reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, TicketRest]] = {
    import scalaz.OptionT._
    import scalaz._
    import Scalaz._

    Future {
    // get team's db name for teamId, team service
    // get request user
      val optUserDb = {
        for {
          id <- optionT(Future.successful(e.id))
          dbX <- optionT(
             db match {
               case Some(x) => Future.successful(Option(x))
               case None => teamDao.getDbByCode(teamCode, None).map(x => x)
             }
          )
          user <- optionT(userDao.getById(reqUser, Some(dbX.dbName)))
          userRole <- optionT(userRoleDao.getRoleForUser(user.id.get, Some(dbX.dbName)))
        } yield (id, dbX, user, userRole)
      }

      optUserDb.fold(
        {
          case (ticketId, dbXY, user, userRole) =>
            // check role permission of user.
            if(userRole == Admin || userRole == L1){
              // validate ticket


              // fetch current ticket
              val exTicket = fetchById(ticketId, user.id.get, teamCode, Some(dbXY), Some(user))
              exTicket map {
                optTicket => optTicket match {
                  case Right(exTick) =>
                    val deltas = generateDelta(exTick, e)
                    if(!deltas.isEmpty){

                    } else {
                      Left(AppError(s"No changes to update on ticket ${exTick.no} Invalid user $reqUser or team code $teamCode", "400", BadRequest))
                    }
                  case Left(err) =>
                    Left(err)
                }
              }
            } else {
              Left(AppError(s"Unauthorised user $reqUser for team code $teamCode", "403", Unauthorised))
            }
        },{
          Left(AppError(s"Invalid user $reqUser or team code $teamCode", "400", NotFound))
        }
      )

      Left(AppError("","",Unauthorised))
    }
  }

  override def delete(id: Long, reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, Long]] = ???

  override def deleteMany(ids: List[Long], reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, List[Long]]] = ???

  override def fetchPaginated(query: Option[String], page: Int, offset: Int, reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, (List[TicketTableRest], Int)]] = ???

  override def fetchById(id: Long, reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, TicketRest]] = ???

  override def create(e: TicketRest, reqUser: Long, teamCode:String, db:Option[Database] = None, user:Option[User] = None): Future[Either[AppError, TicketRest]] = ???
}