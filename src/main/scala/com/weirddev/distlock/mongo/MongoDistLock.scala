/*
 * Copyright 2018 WeirdDev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weirddev.distlock.mongo

import java.net.InetAddress
import java.util.Date

import com.weirddev.distlock.api.DistLock
import com.mongodb.{MongoCommandException, WriteConcern}
import com.mongodb.client.model.ReturnDocument
import com.weirddev.distlock.mongo.LockStates.LockState
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Date: 10/18/2018
  * @author Yaron Yamin
  */


/**
  * @param mongoDb the persistence store for the lock collection
  * @param locksCollectionName the collection name where locks will be stored
  */
class MongoDistLock(mongoDb: MongoDatabase,locksCollectionName:String = "lock_registry")(implicit ec: ExecutionContext) extends DistLock
{
  private val log: Logger = LoggerFactory.getLogger(getClass)
  val DuplicateKeyErrorCode = 11000
  object LockRegistryFieldName{
    val Id = "_id"
    val RegisteredAt = "registeredAt"
    val State = "state"
    val ByHost = "byHost"
  }
  /**
    * @see com.weirddev.com.weirddev.distlock.api.DistLock#lock
    */
  override def lock[T : ClassTag](resourceId: String, expire: Duration)(synchronizedTask: () => T): Future[Option[T]] = {
    //todo overload with a version of lockAsync instead ? or instead document this limitation . can provide the lock future as a param so other async computation types could let it wait for them? re-check the syntax if a param is not required - compare to play cacheApi
    findAndModify(resourceId,LockStates.OPEN,LockStates.LOCKED,expire.toSeconds) map {
      case Some(lockRegistry) =>
        log.info("successfully acquired lock "+lockRegistry)
        val returnValue:T = synchronizedTask()
        returnValue match {
          case future:Future[_] =>
            future onComplete{ _ =>
              findAndModify(resourceId,LockStates.LOCKED,LockStates.OPEN,expire.toSeconds)
            }
            Some(returnValue)
          case _ =>
            findAndModify(resourceId,LockStates.LOCKED,LockStates.OPEN,expire.toSeconds)
            Some(returnValue)
        }
      case None =>
        log.error("could not retrieve lock. aborting execution")
        None
    } recover {
      case ex: MongoCommandException if ex.getErrorCode == DuplicateKeyErrorCode /*DuplicateKey is the expected behaviour when document state is locked ( and not expired yet) since upsert = true( to simplify handling the edge case where lock_registry doc not inserted yet) */=>
        log.info("could not acquire lock. aborting execution")
        None
    }
  }
  private val LockRegistryColl: MongoCollection[LockRegistry] = mongoDb.getCollection[LockRegistry](locksCollectionName).withCodecRegistry(
    fromRegistries(fromProviders(classOf[LockRegistry]), DEFAULT_CODEC_REGISTRY)).withWriteConcern(WriteConcern.ACKNOWLEDGED
  )

  protected def findAndModify(resourceId:String, fromState: LockState, toState: LockState, secondsAgo:Long): Future[Option[LockRegistry]]  = {
    log.info(s"trying to find a lock registry document by _id=$resourceId in state=$fromState or with registeredAt < (sysdate - $secondsAgo seconds ago )")
    LockRegistryColl.findOneAndUpdate(
      Filters.and(
        Filters.eq(LockRegistryFieldName.Id,resourceId),
        Filters.or(
          Filters.eq(LockRegistryFieldName.State, fromState.toString),
          Filters.lt(LockRegistryFieldName.RegisteredAt, new Date(System.currentTimeMillis - secondsAgo * 1000))
        )
      ),
      Updates.combine(
        Updates.currentDate(LockRegistryFieldName.RegisteredAt),
        Updates.set(LockRegistryFieldName.State,toState.toString),
        Updates.set(LockRegistryFieldName.ByHost,InetAddress.getLocalHost.getHostName)
      ),
      FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    )
      .andThen(handleResponse(s"find and modify lock registry")).headOption()
  }

  protected def handleResponse[T](tracedOperation:String,logException:Boolean = true, traceResponse:Boolean=true): PartialFunction[Try[T],Boolean] /*PartialFunction[Try[T],Future[T]]*/ = {
    //todo refactor to move locking logic out and use this as a persistence repository while masking mongo-scala api
    case Failure(ex) if ex.isInstanceOf[MongoCommandException] && ex.asInstanceOf[MongoCommandException].getErrorCode == DuplicateKeyErrorCode =>
      log.info(" Lock upsert attempt resulted with duplicate key failure - task could not be acquired")
      false
    case Failure(ex) =>
      val errMsg = tracedOperation + " failed"
      if(logException) {
        log.error(errMsg, ex)
      }
      false
//      Future.failed(new Exception(errMsg, ex))
    case Success(ret) =>
      if(traceResponse) {
        log.info(tracedOperation + " returned: " + ret)
      }
//            Future.successful(ret)
      true
  }

}
