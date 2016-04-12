import com.micronautics.square._

object SquareTest extends App {
  if (args.isEmpty) {
    System.err.println("Please specify your Square access token")
    System.exit(-1)
  }

  val squareApi = SquareApiFacade(args(0))
  squareApi.locations.foreach { location =>
    println(s"*** $location ***")
    println(squareApi.transactions(location.id).mkString("", "\n", "\n"))
  }
}
