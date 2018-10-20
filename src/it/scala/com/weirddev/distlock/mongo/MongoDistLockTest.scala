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

import com.mongodb.ConnectionString
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase}
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Integration test. assumes there's an unauthenticated running mongod on localhost
  *
  * Date: 10/18/2018
  * @author Yaron Yamin
  *
  */
class MongoDistLockTest extends Specification with Mockito
{
  sequential

  private val mongoUri: String = "mongodb://localhost:27017"

  protected val db: MongoDatabase = MongoClient(MongoClientSettings.builder()
    .applyConnectionString(new ConnectionString(mongoUri))
    .build()).getDatabase("test")

  val mongoDistLock = new MongoDistLock(db, "test_lock_registry")

  val LockExpirationDurationMillis = 1000

  "MongoDistLock accepting non-future returning task" should {

    val TaskExecMsgPrefix = "running synchronized task... no."

    "return computation result when lock is open" in {
      Thread.sleep( LockExpirationDurationMillis )
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  1")
          "I'm done"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
    }

    "return None when locked" in {
      Thread.sleep(LockExpirationDurationMillis)
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  2")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
      }
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  3")
          "I'm can't get into the lock"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === None
    }

    "return result when lock expired" in {
      Thread.sleep(LockExpirationDurationMillis  + 100)
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS )){
          println(TaskExecMsgPrefix + "  4")
          Thread.sleep(LockExpirationDurationMillis * 2)
          "I'm done"
      }
      Thread.sleep(LockExpirationDurationMillis + 50)
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  5")
          "opened it again"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }

    "return result when task is done" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  6")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  7")
          "opened it again"
      }
      Await.result(result, 3.seconds) === Some("I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }
  }
  "MongoDistLock accepting a future returning task" should {

    val TaskExecMsgPrefix = "running a future synchronized task ... no."

    "return computation result when lock is open" in {
      Thread.sleep( LockExpirationDurationMillis )
      val result: Future[Option[Future[String]]] = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        Future {
          println(TaskExecMsgPrefix + "  1")
          Thread.sleep(400)
          "I'm done"
        }
      }
      assertFutureResult(result, "I'm done")
    }

    "return None when locked" in {
      Thread.sleep(LockExpirationDurationMillis)
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        Future {
          println(TaskExecMsgPrefix + "  2")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
        }
      }
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  3")
          "I'm can't get into the lock"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === None
    }

    "return result when lock expired" in {
      Thread.sleep(LockExpirationDurationMillis  + 100)
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS )){
        Future {
          println(TaskExecMsgPrefix + "  4")
          Thread.sleep(LockExpirationDurationMillis * 2)
          "I'm done"
        }
      }
      Thread.sleep(LockExpirationDurationMillis + 50)
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  5")
          "opened it again"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }

    "return result when task is done" in {
      Thread.sleep(LockExpirationDurationMillis )
      val result = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
        Future{
          println(TaskExecMsgPrefix + "  6")
          Thread.sleep(LockExpirationDurationMillis/2)
          "I'm done"
        }
      }
      Thread.sleep(LockExpirationDurationMillis/2 +50)
      val result2 = mongoDistLock.lock("test_task", Duration(LockExpirationDurationMillis,MILLISECONDS)){
          println(TaskExecMsgPrefix + "  7")
          "opened it again"
      }
      assertFutureResult(result, "I'm done")
      Await.result(result2, 3.seconds) === Some("opened it again")
    }
  }

  private def assertFutureResult(result: Future[Option[Future[String]]], expectedResult: String): MatchResult[String] = {
    val maybeEventualString = Await.result(result, 3.seconds)
    maybeEventualString.isInstanceOf[Some[Future[String]]] === true
    Await.result(maybeEventualString.get, 3.seconds) === expectedResult
  }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme