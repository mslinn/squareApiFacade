package com.micronautics.square

import collection.JavaConverters._
import com.mashape.unirest.http.{HttpResponse, JsonNode, Unirest}
import java.util.TimeZone
import org.json.{JSONArray, JSONObject}

case class SquareLocation(name: String, id: String, timezone: TimeZone, capabilities: List[String]) {
  override def toString = s"""Location "$name" with id $id in time zone ${ timezone.getDisplayName } has capabilities: """ + capabilities.mkString(", ")
}

case class SquareApiFacade(accessToken: String) {
  def locations: List[SquareLocation] = {
    import java.util.{Iterator => JIterator}
    val headers: Map[String, String] = List(
      "Accept" -> "application/json",
      "Content-Type" -> "application/json",
      "Authorization" -> s"Bearer $accessToken"
    ).toMap[String, String]
    val responseJson: HttpResponse[JsonNode] = Unirest
      .get("https://connect.squareup.com/v2/locations")
      .headers(headers.asJava)
      .asJson

    val locationsJson: JSONArray = responseJson.getBody.getObject.getJSONArray("locations")
    val locations: List[JSONObject] = locationsJson.iterator.asInstanceOf[JIterator[JSONObject]].asScala.toList
    locations.map { location: JSONObject =>
      SquareLocation(
        location.getString("name"),
        location.getString("id"),
        TimeZone.getTimeZone(location.getString("timezone")),
        location.getJSONArray("capabilities").iterator.asInstanceOf[JIterator[String]].asScala.toList
      )
    }
  }

}
