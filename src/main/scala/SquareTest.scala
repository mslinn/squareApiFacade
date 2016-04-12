import com.micronautics.square._

object SquareTest extends App {
  if (args.isEmpty) sys.error("Please specify your Square access token")

  val squareApi = SquareApiFacade(args(0))
  println(squareApi.locations.mkString(", "))

//  val parameters = java.net.URLEncoder.encode({'begin_time': '2015-01-01T00:00:00-08:00',
//                                 'end_time'  : '2016-01-01T00:00:00-08:00',
//                                 'sort_order': 'ASC'})
}
