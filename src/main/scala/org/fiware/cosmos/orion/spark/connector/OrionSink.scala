package org.fiware.cosmos.orion.spark.connector

import org.apache.spark.streaming.dstream.DStream
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase

import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory


case class OrionSinkObject(content: String, url: String, contentType: ContentType.Value, method: HTTPMethod.Value, headers: Map[String,String]= (Map[String,String]()))



object ContentType extends Enumeration {
  type ContentType = Value
  val JSON = Value("application/json")
  val Plain = Value("text/plain")
}

/**
  * HTTP Method of the message
  */
object HTTPMethod extends Enumeration {
  type HTTPMethod = Value
  val POST = Value("HttpPost")
  val PUT = Value("HttpPut")
  val PATCH = Value("HttpPatch")
}


object OrionSink {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def addSink(rddStream: DStream[OrionSinkObject]): Unit = {

    rddStream.foreachRDD { rdd =>
      rdd.foreach { record =>
        process(record)
      }
    }
  }

  private def process(msg: OrionSinkObject): Unit = {

    val httpEntity : HttpEntityEnclosingRequestBase= createHttpMsg(msg)
    val client = HttpClientBuilder .create.build

    try {
      val response = client.execute(httpEntity)
      logger.info("POST to " + msg.url)
    } catch {
      case e: Exception => {
        logger.error(e.toString)
      }
    }

  }

  /**
    * Http object creator for sending the message
    * @param method HTTP Method
    * @param url Destination URL
    * @return HTTP object
    */
 def getMethod(method: HTTPMethod.Value, url: String): HttpEntityEnclosingRequestBase = {
    method match {
      case HTTPMethod.POST => new HttpPost(url)
      case HTTPMethod.PUT => new HttpPut(url)
      case HTTPMethod.PATCH => new HttpPatch(url)
    }
  }

 def createHttpMsg(msg: OrionSinkObject) : HttpEntityEnclosingRequestBase= {
    val httpEntity = getMethod(msg.method, msg.url)
    httpEntity.setHeader("Content-type", msg.contentType.toString)
   if(msg.headers.nonEmpty){
     msg.headers.foreach({case(k,v) => httpEntity.setHeader(k,v)})
   }
    httpEntity.setEntity(new StringEntity(msg.content))
    httpEntity
  }
}
