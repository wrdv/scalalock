/*
 *  Copyright 2019 WeirdDev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.weirddev.scalalock.reactivemongo

import com.weirddev.scalalock.mongo.AbstractMongoDistributedLockTest
import org.specs2.specification.AfterAll
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Date: 3/27/2019
  *
  * @author Yaron Yamin
  */
class ReactiveMongoDistributedLockTest extends AbstractMongoDistributedLockTest with AfterAll {
  sequential
  isolated
  private val mongoUri: String = "mongodb://localhost:27017/test"
  private val driver = AsyncDriver()
  val database: Future[DB] = for {
    uri <- MongoConnection.fromString(mongoUri)
    con <- driver.connect(uri, uri.db)
    db <- con.database(uri.db.get)
  } yield db

  val mongoDistLock = new ReactiveMongoDistributedLock(database, "test_lock_registry2")

  def afterAll(): Unit = {
    database.map { _ =>
      println(s"Closing connection....")
      driver.close()
    }
  }
}
