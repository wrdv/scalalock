# DistLock

Distributed lock library in scala. based on mongodb (using [mongo-scala-driver](https://github.com/mongodb/mongo-scala-driver) )

consist of a 2 modules:
- _distlock-api_ - contains the api and the locking logic
- _distlock-mongo_ - an api implementation using mongodb as the lock persistence store. implementation is dependant on [mongo-scala-driver](https://github.com/mongodb/mongo-scala-driver).


### Project state
beta

### Contributing/Developing
Welcomed :) - Please refer to [`CONTRIBUTING.md`](./CONTRIBUTING.md) file.


### License
Copyright (c) 2018  [WeirdDev](http://weirddev.com).
Licensed for free usage under the terms and conditions of Apache V2 - [Apache V2 License](https://www.apache.org/licenses/LICENSE-2.0).
