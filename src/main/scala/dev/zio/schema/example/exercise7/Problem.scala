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
      toDV(params).map(_.toTypedValue(schema)).collectFirst {
        case Right(v) => v
      }.toRight("some error")
    }


    // parse each element into String and if possible Int representations. We basically create all
    // possible solutions here. The Set[DynamicValue] removes duplicates.
    def toDV(params: Map[String, List[String]]): Set[DynamicValue] = {
        import DynamicValue._
        params.foldLeft[Set[ListMap[String, DynamicValue]]](Set(ListMap())) {
          case (set, (key, values)) =>
            set.flatMap(acc => {
              values match {
                case Nil      => Set(acc.updated(key, Singleton(())))
                case x :: Nil => {
                  val strInterpretation = Set(acc.updated(key, Primitive[String](x, StandardType.StringType)))
                  val intInterpretation = x.toIntOption match {
                    case Some(value) => Set(acc.updated(key, Primitive[Int](value, StandardType.IntType)))
                    case None => Set()
                  }
                  strInterpretation ++ intInterpretation
                }
                case xs       => Set(acc.updated(key, DynamicValue.Sequence(Chunk.fromIterable(xs).map(Primitive[String](_, StandardType.StringType)))))
              }
            })
        }.map(DynamicValue.Record)
      }


    val p = decode[Person](Map("name" -> List("John"), "age" -> List("42")))

    println(p)
  }

  object Approach2 extends scala.App{
    import Schema._
    type QueryParams = Map[String, List[String]]

    // this will be a sophisticated solution for a high performance library like ZIO
    def decodeFromQueryParams[A](params: QueryParams)(implicit schema: Schema[A]): Either[String, A] = {

      def compile[B](key: Option[String], schema: Schema[B]): QueryParams => Either[String, B] = schema match {
        case Transform(codec, f, g) => ???
        case Primitive(standardType) =>
          key match {
            case None =>
              val error = Left(s"Cannot extract a primitive out of a query string")
              Function.const(error)
            case Some(key) =>
              standardType match {
                case StandardType.StringType =>
                  val f: QueryParams => Either[String, B] = (qp: QueryParams) => qp.get(key) match {
                      case Some(value :: _) => Right[String, B](value.asInstanceOf[B])
                      case _ => Left(s"Cannot extract a primitive string out of nothing")
                    }
                  f
                case StandardType.IntType =>
                  val f: QueryParams => Either[String, B] = (qp: QueryParams) => qp.get(key) match {
                    case Some(value :: _) => value.toIntOption.toRight(s"cannot create an integer out of ${value}").asInstanceOf[Either[String, B]]
                    case _ => Left(s"Cannot extract a primitive string out of nothing")
                  }
                  f
                case _ =>
                  val error = Left(s"Expected String or Int but found ${standardType}")
                  Function.const(error)
            }
          }
        case record: Record[_] => ???

        case enum: Enum[_] => ???
//        case Optional(codec) => ???
        case Fail(message) => ???
//        case Tuple(left, right) => ???
//        case EitherSchema(left, right) => ???
        case Lazy(schema0) => ???
        case Meta(ast) => ???
        case _ =>
          val err = Left(s"Decoding from query parameters is not supported for ${schema}")
          Function.const(err)
      }

      val fn = compile(None, schema)

      fn(params)
    }

    println("approach 2")
    println(decodeFromQueryParams[Person](Map("name" -> List("John"), "age" -> List("42"))))
  }
}

object Runner extends scala.App {

  Problem.Approach1.main(Array.empty)
  Problem.Approach2.main(Array.empty)
}
