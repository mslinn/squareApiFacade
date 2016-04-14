package com.micronautics.square

import java.util.{TimeZone, UUID}

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat.fullDateTime
import Model._
import org.json.JSONObject

object Model {
  implicit class CaseClassToString(anyRef: AnyRef) {
    /** @return equivalent compressed JSON object, suppressing properties which have values that are empty strings, Nil, or None
      *         Unwraps Option values */
    def toJsonNameValue: String = {
      import java.lang.reflect.Field

      def retval(field: Field, v: Any): Option[String] = Some(s"${ field.getName }:$v")

      val fields = anyRef.getClass.getDeclaredFields.flatMap { field =>
        field.setAccessible(true)
        val value: AnyRef = field.get(anyRef)
        if (null==value || value=="" || value==None || value==Nil) None else value match {
          case Some(v) => retval(field, v)
          case v =>  retval(field, v)
        }
      }

      fields.mkString("{", ",", "}")
    }
  }
}

object Address {
  def apply(json: JSONObject): Address =
    Address(
      address_line_1=json.getString("address_line_1"),
      locality=json.getString("locality"),
      postal_code=PostalCode(json.getString("postal_code")),
      country=Country(json.getString("country")),
      address_line_2=if (json.has("address_line_2")) json.getString("address_line_2") else "",
      address_line_3=if (json.has("address_line_3")) json.getString("address_line_3") else "",
      sublocality=if (json.has("sublocality")) json.getString("sublocality") else "",
      sublocality_2=if (json.has("sublocality_2")) json.getString("sublocality_2") else "",
      sublocality_3=if (json.has("sublocality_3")) json.getString("sublocality_3") else "",
      administrative_district_level_1=if (json.has("administrative_district_level_1")) json.getString("administrative_district_level_1") else "",
      administrative_district_level_2=if (json.has("administrative_district_level_2")) json.getString("administrative_district_level_2") else "",
      administrative_district_level_3=if (json.has("administrative_district_level_3")) json.getString("administrative_district_level_3") else ""
    )
}

case class Address(
  address_line_1: String,
  locality: String,
  postal_code: PostalCode,
  country: Country,
  address_line_2: String="",
  address_line_3: String="",
  sublocality: String="",
  sublocality_2: String="",
  sublocality_3: String="",
  administrative_district_level_1: String="",
  administrative_district_level_2: String="",
  administrative_district_level_3: String=""
) {
  def toJson = this.toJsonNameValue
}

case class Card(
  last_4: String,
  card_brand: String,
  id: String="",
  exp_month: String="",
  exp_year: String="",
  cardholder_name: String="",
  billing_address: Option[Address]=None
) {
   def toJson = this.toJsonNameValue
   override def toString = s"$card_brand ending with $last_4"
}

case class Country(value: String) extends AnyVal {
  override def toString = value
}

object Money {
  def apply(json: JSONObject): Money = Money(json.getInt("amount"), json.getString("currency"))
}

case class Money(amount: Int, currency: String) {
  def toJson = this.toJsonNameValue
  override def toString = s"$amount $currency"
}

case class Nonce(value: String) extends AnyVal {
  override def toString = value
}

case class Payment(nonce: Nonce, amount: Money, billingAddress: Address, uuid: String=UUID.randomUUID.toString, maybeNote: Option[String], maybeRefId: Option[String]) {
  def toJson = s""""{
                   |  "idempotency_key": "$uuid",
                   |  "billing_address": ${ billingAddress.toJson },
                   |  "amount_money": ${ amount.toJson },
                   |  "card_nonce": "$nonce",
                   |  ${ maybeRefId.map( refId => s""" "reference_id: "$refId" """).mkString },
                   |  ${ maybeNote.map( note => s""" "note": "$note" """).mkString },
                   |  "delay_capture": false
                   |}
                   """.stripMargin
}

case class PostalCode(value: String) extends AnyVal {
  override def toString = value
}

case class Refund(
  id:	String,
  location_id: String,
  transaction_id: String,
  tender_id:String,
  created_at: String,
  reason: String,
  amount_money: Money,
  status: String,
  processing_fee_money: Money
) {
  def toJson = this.toJsonNameValue
}

case class SquareLocation(
  name: String,
  id: String,
  timezone: TimeZone,
  capabilities: List[LocationCapability],
  address: Option[Address]=None
) {
  def toJson = this.toJsonNameValue
  override def toString = s"""Location "$name" with id $id in time zone ${ timezone.getDisplayName } has capabilities: """ + capabilities.mkString(", ")
}

case class SquareTransaction(
  id: String,
  location_id: String,
  created_at: DateTime,
  product: SquareProduct,
  tenders: List[Tender]=Nil,
  reference_id: Option[String]=None,
  refunds: List[Refund]=Nil
) {
  def toJson = this.toJsonNameValue

  override def toString = {
    val refId: String = reference_id.map(ri => s"with reference #$ri").mkString
    s"""Transaction #$id at location #$location_id created_at ${ fullDateTime.print(created_at) } by $product $refId
       |  ${ tenders.mkString("\n    ") }
       |  ${ refunds.mkString("\n    ") }
     """.stripMargin
  }
}

case class Tender(
  transaction_id: String,
  created_at: DateTime,
  amount_money: Money,
  id: String,
  `type`: String,
  processing_fee_money: Money,
  location_id: String,
  customer_id: Option[String]=None,
  note: Option[String]=None,
  card_details: Option[TenderCardDetails]=None,
  cash_details: Option[TenderCashDetails]=None
) {
  def toJson = this.toJsonNameValue

  override def toString =
    s"""Tender #$id paid by ${`type`} ${ card_details.mkString }${ cash_details.mkString } for $amount_money with fee $processing_fee_money""".stripMargin
}

case class TenderCardDetails(
  entryMethod: String,
  card: Card,
  status: String
) {
  def toJson = this.toJsonNameValue
  override def toString = s"$card $entryMethod"
}

case class TenderCashDetails(
  buyer_tendered_money: Money,
  change_back_money: Money
) {
  def toJson = this.toJsonNameValue
}
