package dev.zio.schema.example.exercise7

import zio.Chunk
import zio.schema.{DeriveSchema, DynamicValue, Schema, StandardType}

import scala.collection.immutable.ListMap

/**
 * This exercise is based on John DeGoes Spartan training on ZIO-Schema from 2021-11-04
 */
private[exercise7] object Problem {


  final case class Person(name: String, age: Int)
  object Person {
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  }
  final case class Profile(location: String, address: String)
  object Profile {
    implicit val schema: Schema[Profile] = DeriveSchema.gen[Profile]
  }
  // sample url1: /foo/?name=john&age=42#foo
  // sample url2: /foo/?name=john&age=42&location=london&address=baker%20street

  def decodePersonFromQueryParams(params: Map[String, List[String]]): Either[String, Person] =
    for {
      name <- params.get("name").toRight("name parameter is missing")
      age  <- params.get("age").toRight("age parameter is missing")
    } yield Person(name.head, age.head.toInt)

  def decodeProfileFromQueryParams(params: Map[String, List[String]]): Either[String, Profile] =
    for {
      location <- params.get("location").toRight("location parameter is missing")
      address  <- params.get("address").toRight("address parameter is missing")
    } yield Profile(location.head, address.head)

  object Approach1 extends scala.App {
    // this will be a "quick and dirty" solution, that can be accomplished in a few minutes.
    // not suitable for _extremely high performance_ applications
    // probably suitable for the normal business application with medium performance requirements
    def decode[A](params: Map[String, List[String]])(implicit schema: Schema[A]): Either[String, A] = {
      toDV(params).toTypedValue(schema)
    }

    def toDV(params: Map[String, List[String]]): DynamicValue = {
        import DynamicValue._
        DynamicValue.Record(params.foldLeft[ListMap[String, DynamicValue]](ListMap.empty) {
          case (acc, (key, values)) =>
            acc.updated(key, values match {
              case Nil      => Singleton(())
              case x :: Nil => Primitive[String](x, StandardType.StringType)
              case xs       => DynamicValue.Sequence(Chunk.fromIterable(xs).map(Primitive[String](_, StandardType.StringType)))
            })
        })
      }


    val p = decode[Person](Map("name" -> List("John"), "age" -> List("42")))

    println(p)
  }

  object Approach2 extends scala.App{
    import Schema._
    // this will be a sophisticated solution for a high performance library like ZIO
    def decodeFromQueryParams[A](params: Map[String, List[String]])(implicit schema: Schema[A]): Either[String, A] = {
      schema match {
        case enum: Enum[_] => ???
        case record: Record[_] => ???
        case Sequence(schemaA, fromChunk, toChunk) => ???
        case Transform(codec, f, g) => ???
        case Primitive(standardType) => ???
        case Optional(codec) => ???
        case Fail(message) => ???
        case Tuple(left, right) => ???
        case EitherSchema(left, right) => ???
        case Lazy(schema0) => ???
        case Meta(ast) => ???
      }
    }
  }
}

object Runner extends scala.App {

  Problem.Approach1.main(Array.empty)
  Problem.Approach2.main(Array.empty)
}
