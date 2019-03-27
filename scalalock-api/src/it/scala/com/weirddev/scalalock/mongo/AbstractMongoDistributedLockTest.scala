/*
 * Copyright 2019 WeirdDev.
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
package com.weirddev.scalalock.mongo

import com.weirddev.scalalock.api.DistributedLock
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MILLISECONDS, _}
import scala.concurrent.{Await, Future}

/**
  * Date: 3/27/2019
  * @author Yaron Yamin
  *
  */
abstract class AbstractMongoDistributedLockTest extends Specification with Mockito
{
  val LockExpirationDurationMillis = 1000
  val mongoDistLock:DistributedLock

  "MongoDistributedLock accepting non-future returning task" should {

    val TaskExecMsgPrefix = "running synchronized task... no."

    "return computation result when lock is open" in {
      Thread.sleep( LockExpirationDurationMillis )
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  1")
        "I'm done"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
    }

    "return None when locked" in {
      Thread.sleep(LockExpirationDurationMillis)
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  2")
        Thread.sleep(LockExpirationDurationMillis/2)
        "I'm done"
      }
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  3")
        "I'm can't get into the lock"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === None
    }

    "return result when lock expired" in {
      Thread.sleep(LockExpirationDurationMillis  + 100)
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS )){
        println(TaskExecMsgPrefix + "  4")
        Thread.sleep(LockExpirationDurationMillis * 2)
        "I'm done"
      }
      Thread.sleep(LockExpirationDurationMillis + 50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  5")
        "opened it again"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }

    "return result when task is done" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  6")
        Thread.sleep(LockExpirationDurationMillis/2)
        "I'm done"
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  7")
        "opened it again"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }
    "keep lock is releaseLockWhenDone is set to false" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS),releaseLockWhenDone = false){
        println(TaskExecMsgPrefix + "  6B")
        Thread.sleep(LockExpirationDurationMillis/2)
        "I'm done"
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  7B")
        "should not be able to open"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === None
    }
  }
  "MongoDistributedLock accepting a future returning task" should {

    val TaskExecMsgPrefix = "running a future synchronized task ... no."

    "return computation result when lock is open" in {
      Thread.sleep( LockExpirationDurationMillis )
      val result: Future[Option[Future[String]]] = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        Future {
          println(TaskExecMsgPrefix + "  1")
          Thread.sleep(400)
          "I'm done"
        }
      }
      assertFutureResult(result, "I'm done")
    }

    "return None and dont run future task when locked" in {
      Thread.sleep(LockExpirationDurationMillis)
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        Future {
          println(TaskExecMsgPrefix + "  2")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
        }
      }
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  3")
        "I'm can't get into the lock"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === None
    }

    "return result when lock expired" in {
      Thread.sleep(LockExpirationDurationMillis  + 100)
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS )){
        Future {
          println(TaskExecMsgPrefix + "  4")
          Thread.sleep(LockExpirationDurationMillis * 2)
          "I'm done"
        }
      }
      Thread.sleep(LockExpirationDurationMillis + 50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        println(TaskExecMsgPrefix + "  5")
        "opened it again"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }

    "return result when task is done" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS),releaseLockWhenDone = true,Some("task returning a future")){
        Future{
          println(TaskExecMsgPrefix + "  6")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
        }
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS),releaseLockWhenDone = true,Some("a 2nd task returning a future")){
        println(TaskExecMsgPrefix + "  7")
        "opened it again"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }
    "keep lock is releaseLockWhenDone is set to false" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS),releaseLockWhenDone = false,Some("task returning a future")){
        Future{
          println(TaskExecMsgPrefix + "  8")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
        }
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.acquire("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS),releaseLockWhenDone = true,Some("a 2nd task returning a future")){
        println(TaskExecMsgPrefix + "  7")
        "should not be able to open"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === None
    }
  }

  private def assertFutureResult(result: Future[Option[Future[String]]], expectedResult: String): MatchResult[String] = {
    val maybeEventualString = Await.result(result, 3.seconds)
    maybeEventualString.getClass ===  classOf[Some[Future[String]]]
    Await.result(maybeEventualString.get, 3.seconds) === expectedResult
  }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme