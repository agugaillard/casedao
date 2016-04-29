# CaseDAO

CaseDAO is a Scala library that allows you to *create, read, update and delete* your case classes **naturally**. It is non-blocking and requires [MongoDB](https://www.mongodb.org/). Its only condition is that every relationship is bidirectional, it means there must be a way for an entity to reach every entity that is related to.

## Examples

```scala
case class User(
  document: String,
  name: String,
  cars: Seq[Lite[Car]] = Seq(),
  @Key("_id") id: BSONObjectID = BSONObjectID.generate
) extends Entity

object User {
  implicit val daoConf = new DAOConf[User]("users", (u) => BSONDocument("document" -> u.document))
  implicit val lite = Lite.conf[User]
  implicit val jsonFormat = Json.format[User]
  implicit val bsonFormat = Macros.handler[User]
}

case class Car(
  plate: String,
  model: String,
  drivers: Seq[Lite[User]],
  @Key("_id") id: BSONObjectID = BSONObjectID.generate
) extends Entity

object Car {
  implicit val daoConf = new DAOConf[Car]("cars", (c) => BSONDocument("plate" -> c.plate))
  implicit val lite = Lite.conf[Car]
  implicit val jsonFormat = Json.format[Car]
  implicit val bsonFormat = Macros.handler[Car]
}

val user1 = User("document1", "Name One")
val user2 = User("document2", "Name Two")
```

### Save

```scala
CaseDAO.save(user1)
CaseDAO.save(user2)

//  db.users.find()
//    { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name" : "Name One", "cars" : [] }
//    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name" : "Name Two", "cars" : [] }

val car1 = Car("AAA 000", "Ford", Seq(Lite(user1))
val car2 = Car("BBB 111", "Chevrolet", Seq(Lite(user1), Lite(user2))

CaseDAO.save(car1)
CaseDAO.save(car2)

//  db.cars.find()
//    { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford",
//      "drivers" : [ { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name": "Name One" } ] }
//    { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet",
//      "drivers" : [ { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name": "Name One" },
//                    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name": "Name Two" } ] }
//  db.users.find()
//    { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name" : "Name One",
//      "cars" : [ { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford" },
//                 { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet" } ] }
//    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name" : "Name Two",
//      "cars" : [ { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet" } ] }
```

### Update

```scala
val updatedUser1 = user1.copy(name = "Name One Updated")
CaseDAO.update[User](updatedUser1)

//  db.cars.find()
//    { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford",
//      "drivers" : [ { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name": "Name One Updated" } ] }
//    { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet",
//      "drivers" : [ { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name": "Name One Updated" },
//                    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name": "Name Two" } ] }
//  db.users.find()
//    { "_id" : ObjectId("IDUSER1"), "document" : "document1", "name" : "Name One Updated",
//      "cars" : [ { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford" },
//                 { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet" } ] }
//    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name" : "Name Two",
//      "cars" : [ { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet" } ] }
```

### Read

```scala
val readUser1: User = CaseDAO.get[User]("IDUSER1")
```

### Remove

#### Deplace

```scala
CaseDAO.deplace(user1, user2)

//  db.cars.find()
//    { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford",
//      "drivers" : [ { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name": "Name Two" } ] }
//    { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet",
//      "drivers" : [ { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name": "Name Two" } ] }
//
//  db.users.find()
//    { "_id" : ObjectId("IDUSER2"), "document" : "document2", "name" : "Name Two",
//      "cars" : [ { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford" },
//                 { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet" } ] }
```

#### Delete

```scala
CaseDAO.delete(user2)

//  db.cars.find()
//    { "_id" : ObjectId("IDCAR1", "plate" : "AAA 000", "model" : "Ford", "drivers" : [ ] }
//    { "_id" : ObjectId("IDCAR2", "plate" : "BBB 111", "model" : "Chevrolet", "drivers" : [ ] }
//
//  db.users.find()  ->  empty
```

*There is also an overloaded method `deplace` that allows you to deplace by multiple entities and in determined collections*

## Usage
Add this to your `build.sbt`:
```
resolvers += "SonaType" at "https://oss.sonatype.org/content/groups/public"

libraryDependencies += "com.github.agugaillard" %% "casedao" % "0.1-SNAPSHOT"
```
