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
package com.weirddev.scalalock.model

import java.util.Date

/**
  * Date: 10/18/2018
  * @author Yaron Yamin
  */
case class LockRegistry(_id:String, registeredAt:Date, state:String, byHost:String, taskId: Option[String])

object LockRegistry {
  val DefaultCollectionName = "lock_registry"
}

object LockStates extends Enumeration {
  type LockState = Value
  val LOCKED,OPEN = Value
}

object LockRegistryFieldName{
  val Id = "_id"
  val RegisteredAt = "registeredAt"
  val State = "state"
  val ByHost = "byHost"
  val TaskId = "taskId"
}