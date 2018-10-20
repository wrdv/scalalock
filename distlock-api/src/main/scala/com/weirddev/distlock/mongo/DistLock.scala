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

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
class DistLock(lockRepository:LockRepository)(implicit ec: ExecutionContext) extends Lock {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  /**
    * @see com.weirddev.distlock.api.DistLock#lock
    */
  override def lock[T](resourceId: String, expire: Duration)(synchronizedTask: => T): Future[Option[T]] = {
    lockRepository.tryToLock(resourceId, expire) map {
      case true =>
        log.info("successfully acquired lock")
        synchronizedTask match {
          case futureReturnValue:Future[_] =>
            futureReturnValue onComplete{ _ =>
              lockRepository.releaseLock(resourceId, expire)
            }
            Some(futureReturnValue).asInstanceOf[Option[T]]
          case nonFutureReturnValue  =>
            lockRepository.releaseLock(resourceId, expire)
            Some(nonFutureReturnValue)
        }
      case false =>
        log.error("could not retrieve lock. aborting execution")
        None
    }
  }

}
