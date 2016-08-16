package services.cache

import models.team.{Client, Client$}

import scala.collection.mutable

/**
  * Created by abhilash on 20/7/16.
  *
  *
  * need a more robust impl, maybe use a cache
  *
  * todo init this on start
  *
  */
object ClientService {
  private val tenantMap : scala.collection.mutable.HashMap[Long, Client] = new mutable.HashMap[Long, Client]
  def get(k:Long) = tenantMap.get(k)
  def update(k:Long,v:Client) = tenantMap.put(k,v)
  def update(teams:List[Client]) = tenantMap ++ teams.map(x => (x.id.getOrElse(0), x))
}
