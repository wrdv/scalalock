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
package com.weirddev.scalalock.mongo

import com.mongodb.ConnectionString
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Integration test. assumes there's an unauthenticated running mongod on localhost
  *
  * Date: 10/18/2018
  * @author Yaron Yamin
  *
  */
class MongoDistributedLockITest extends AbstractMongoDistributedLockTest
{
  sequential

  private val mongoUri: String = "mongodb://localhost:27017"
  private val db: MongoDatabase = MongoClient(MongoClientSettings.builder()
    .applyConnectionString(new ConnectionString(mongoUri))
        .build()).getDatabase("test")
  val mongoDistLock = new MongoDistributedLock(db, "test_lock_registry")
}