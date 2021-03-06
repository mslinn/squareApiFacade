package com.micronautics.square

import java.net.URLEncoder._

import collection.JavaConverters._
import com.mashape.unirest.http.{HttpResponse, JsonNode, Unirest}
import java.util.TimeZone

import org.joda.time.DateTime
import org.json.{JSONArray, JSONObject}

object SquareApiFacade {
  import org.joda.time.format.ISODateTimeFormat

  val isoFormatter = ISODateTimeFormat.dateTime
  def urlEncode(aMap: Map[String, String]): Map[String, String] = {
    import java.net.URLEncoder.encode
    aMap.iterator.map( entry => encode(entry._1, "UTF-8") -> encode(entry._2, "UTF-8")).toMap
  }
}

case class SquareApiFacade(accessToken: String) {
  import java.util.{Iterator => JIterator}
  import SquareApiFacade._

  def headers: Map[String, String] = List(
    "Accept" -> "application/json",
    "Content-Type" -> "application/json",
    "Authorization" -> s"Bearer $accessToken"
  ).toMap[String, String]

  /** @param amount is in cents and assumes USD */
  def charge(locationId: String, nonce: String, amount: Int) = {
    val url = s"https://connect.squareup.com/v2/locations/$locationId/transactions"
    val response = Unirest
      .post(url)
      .headers(headers.asJava)
      .body(s"""{
               |  'card_nonce': '$nonce',
               |  'amount_money': {
               |    'amount_money': $amount,
               |    'currency': 'USD'
               |  },
               |  'idempotency_key': ${ java.util.UUID.randomUUID }
               |}""".stripMargin)
      .asJson
    response
  }

  /** Assumes that the returned value never changes during program lifespan */
  lazy val locations: List[SquareLocation] = {
    val responseJson: HttpResponse[JsonNode] = Unirest
      .get("https://connect.squareup.com/v2/locations")
      .headers(headers.asJava)
      .asJson

    val locationsJson: JSONArray = responseJson.getBody.getObject.getJSONArray("locations")
    val locationList: List[JSONObject] = locationsJson.iterator.asInstanceOf[JIterator[JSONObject]].asScala.toList
    locationList.map { location: JSONObject =>
      val locationStrs: List[String] =
        location.getJSONArray("capabilities").iterator.asInstanceOf[JIterator[String]].asScala.toList
      SquareLocation(
        name=location.getString("name"),
        id=location.getString("id"),
        timezone=TimeZone.getTimeZone(location.getString("timezone")),
        capabilities=locationStrs.map(LocationCapability.valueOf),
        address=if (location.has("address")) Some(Address(location.getJSONObject("address"))) else None
      )
    }
  }

  /** TODO find out why from parameter is ignored; all transactions are returned */
  def transactions(locationId: String, from: DateTime=DateTime.now.minusMonths(1), to: DateTime=DateTime.now, sortAscending: Boolean=true) = {
    val paramMap: Map[String, String] = List(
      "begin_time" -> isoFormatter.print(from),
      "end_time"   -> isoFormatter.print(to),
      "sort_order" -> (if (sortAscending) "ASC" else "DESC")
    ).toMap
    val paramsJson: String = new JSONObject(paramMap.asJava).toString()

    val url = s"https://connect.squareup.com/v2/locations/$locationId/transactions?${encode(paramsJson, "UTF-8")}"
    val responseJson: HttpResponse[JsonNode] = Unirest
      .get(url)
      .headers(headers.asJava)
      .asJson

    val obj = responseJson.getBody.getObject.getJSONArray("transactions")
    val txs = obj.iterator.asInstanceOf[JIterator[JSONObject]].asScala.toList
    val transactions = txs.map { tx =>
      val tendersJson: List[JSONObject] = tx.getJSONArray("tenders").iterator.asInstanceOf[JIterator[JSONObject]].asScala.toList
      val tenders: List[Tender] = tendersJson.map { tj =>
        val cd = tj.getJSONObject("card_details")
        val cardJson = cd.getJSONObject("card")
        val card = Card(cardJson.getString("last_4"), cardJson.getString("card_brand"))
        val cardDetails = TenderCardDetails(cd.getString("entry_method"), card, cd.getString("status"))

        val am = tj.getJSONObject("amount_money")
        val amount = Money(am.getInt("amount"), am.getString("currency"))

        val pf = tj.getJSONObject("processing_fee_money")
        val processingFee = Money(pf.getInt("amount"), pf.getString("currency"))

        Tender(
          transaction_id=tj.getString("transaction_id"),
          created_at=new DateTime(tj.get("created_at")),
          amount_money=amount,
          id=tj.getString("id"),
         `type`=tj.getString("type"),
          processing_fee_money=processingFee,
          location_id=tj.getString("location_id"),
          card_details=Some(cardDetails)
        )
      }

      val refundsJson: List[JSONObject] = if (tx.has("refunds"))
        tx.getJSONArray("refunds").iterator.asInstanceOf[JIterator[JSONObject]].asScala.toList else Nil
      val refunds: List[Refund] = refundsJson.map { json =>
        Refund(
          id=json.getString("id"),
          location_id = json.getString("location_id"),
          transaction_id = json.getString("transaction_id"),
          tender_id = json.getString("tender_id"),
          created_at = json.getString("created_at"),
          reason = json.getString("reason"),
          amount_money = Money(json.getJSONObject("amount_money")),
          status = json.getString("status"),
          processing_fee_money = Money(json.getJSONObject("processing_fee_money"))
        )
      }

      SquareTransaction(
        id=tx.getString("id"),
        location_id=tx.getString("location_id"),
        created_at=new DateTime(tx.getString("created_at")),
        product=SquareProduct.valueOf(tx.getString("product")),
        tenders=tenders,
        reference_id=if (tx.has("reference_id")) Some(tx.getString("reference_id")) else None,
        refunds=refunds
      )
    }
    transactions
  }
}
