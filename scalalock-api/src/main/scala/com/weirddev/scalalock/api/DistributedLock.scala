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
package com.weirddev.scalalock.api

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
class DistributedLock(lockRepository:LockRepository)(implicit ec: ExecutionContext) extends Lock {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  /**
    * @see com.weirddev.scalalock.api.Lock#lock
    */
  override def acquire[T](resourceId: String, expire: Duration= Duration.Inf,releaseLockWhenDone:Boolean = true, optTaskId:Option[String]=None)(synchronizedTask: => T): Future[Option[T]] = {
    val resourceDetailsLogMsg = s"for resource '$resourceId'${optTaskId.map(" by task '" + _+"'").getOrElse("")}"
    lockRepository.tryToLock(resourceId, expire,optTaskId) map {
      case true =>
        log.info(s"successfully acquired lock "+resourceDetailsLogMsg)
        synchronizedTask match {
          case futureReturnValue:Future[_] =>
            if(releaseLockWhenDone){
              futureReturnValue onComplete{ _ =>
                lockRepository.releaseLock(resourceId, expire,optTaskId)
              }
            }
            Some(futureReturnValue).asInstanceOf[Option[T]]
          case nonFutureReturnValue  =>
            if(releaseLockWhenDone) {
              lockRepository.releaseLock(resourceId, expire,optTaskId)
            }
            Some(nonFutureReturnValue)
        }
      case false =>
        log.info(s"could not acquire lock $resourceDetailsLogMsg. aborting execution")
        None
    }
  }

}
