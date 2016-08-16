package config

import com.typesafe.config.{Config, ConfigFactory, ConfigValue}

/**
  * Created by abhilash on 9/7/16.
  */
object ConfigUtils {

  def updateConfig(key:String, value:ConfigValue): Unit ={
    ConfigFactory.load(ConfigFactory.defaultApplication().withValue(key, value))
  }

  def currentConfig(): Config ={
    ConfigFactory.defaultApplication()
  }

  def hasKey(key:String):Boolean = {
    ConfigFactory.defaultApplication().hasPath(key)
  }
}
