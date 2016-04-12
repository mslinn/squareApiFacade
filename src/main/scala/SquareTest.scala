import com.micronautics.square._

object SquareTest extends App {
  if (args.isEmpty) sys.error("Please specify your Square access token")

  val squareApi = SquareApiFacade(args(0))
  squareApi.locations.foreach { location =>
    println(s"*** $location ***")
    println(squareApi.transactions(location.id).mkString("", "\n", "\n"))
  }
}
