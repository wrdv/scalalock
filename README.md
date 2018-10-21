# DistLock

Distributed lock library in scala. based on mongodb (using [mongo-scala-driver](https://github.com/mongodb/mongo-scala-driver) )

consist of a 2 modules:
- _distlock-api_ - contains the api and the locking logic
- _distlock-mongo_ - an api implementation using mongodb as the lock persistence store. implementation is dependant on [mongo-scala-driver](https://github.com/mongodb/mongo-scala-driver).


### Usage

1. Add dependencies to `build.sbt`:

    ```scala
    libraryDependencies ++= Seq(
      "com.weirddev" %% "distlock-api" % "1.0.3",
      "com.weirddev" %% "distlock-mongo" % "1.0.3"
    )
    ```

2. wrap the block that should be synchronized across all nodes in cluster with ```DistLock#lock()``` method call

    ```scala
    import com.mongodb.ConnectionString
    import com.weirddev.distlock.mongo.MongoDistLock
    import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase}
    import scala.concurrent.Future
    import scala.concurrent.duration._

    protected val db: MongoDatabase = MongoClient(MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
        .build()).getDatabase("test")

    val distLock:DistLock = new MongoDistLock(db)

    distLock.lock("some_task_id", 10 minutes){
      Future{
        println("this block needs to execute in isolation across all application nodes in cluster")
        Thread.sleep(5000)
        "Task Completed"
      }
    }
    ```

3. For other usage scenarios, review the [integrative test code](https://github.com/wrdv/distlock/blob/master/distlock-mongo/src/it/scala/com/weirddev/distlock/MongoDistLockTest.scala)

### Contributing/Developing
Welcomed :) - Please refer to [`CONTRIBUTING.md`](./CONTRIBUTING.md) file.

### License
Copyright (c) 2018  [WeirdDev](http://weirddev.com).
Licensed for free usage under the terms and conditions of Apache V2 - [Apache V2 License](https://www.apache.org/licenses/LICENSE-2.0).
