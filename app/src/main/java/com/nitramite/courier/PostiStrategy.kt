package com.nitramite.courier

import android.annotation.SuppressLint
import android.util.Log
import com.nitramite.paketinseuranta.EventObject
import com.nitramite.utils.Utils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLException

class PostiStrategy : CourierStrategy {

    companion object {
        // Logging
        private val TAG = PostiStrategy::class.java.simpleName
    }

    override fun execute(parcelCode: String): ParcelObject {
        val parcelObject = ParcelObject(parcelCode)
        val eventObjects = ArrayList<EventObject>()
        try {
            val url = "https://www.posti.fi/henkiloasiakkaat/seuranta/api/shipments/$parcelCode"
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build()
            val response = client.newCall(request).execute()
            val jsonResult = response.body!!.string()


            // Parsing got json content
            // Log.i(TAG, "Parsing posti: " + parcelCode);
            val jsonResponse = JSONObject(jsonResult) // Json content
            val jsonMainNode = jsonResponse.optJSONArray("shipments") // Get "shipments" array
            val jsonChildNode = jsonMainNode.getJSONObject(0) // Get first object from "shipments" array

            // Log.i(TAG, jsonChildNode.toString());

            if (jsonChildNode.length() > 0) {
                parcelObject.isFound = true // Parcel is found

                parcelObject.parcelCode2 = jsonChildNode.optString("trackingCode")
                parcelObject.errandCode = jsonChildNode.optString("errandCode")
                parcelObject.phase = jsonChildNode.optString("phase")


                // Parse estimate delivery time
                try {
                    @SuppressLint("SimpleDateFormat") val apiDateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") val showingDateFormat: DateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    val timeStamp = jsonChildNode.optString("estimatedDeliveryTime")
                    val parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp))
                    val parsedDate = showingDateFormat.format(parseTimeDate)
                    parcelObject.estimatedDeliveryTime = parsedDate
                } catch (e: Exception) {
                    Log.i(TAG, e.toString())
                }


                if (!jsonChildNode.isNull("pickupAddress")) {
                    val pickupAddress = jsonChildNode.getJSONObject("pickupAddress")
                    if (pickupAddress.length() > 4) {
                        parcelObject.setPickupAddress(
                                pickupAddress.optString("name"),
                                pickupAddress.optString("street"),
                                pickupAddress.optString("postcode"),
                                pickupAddress.optString("city"),
                                pickupAddress.optString("latitude"),
                                pickupAddress.optString("longitude"),
                                pickupAddress.optString("availability")
                        )
                    }
                }


                // Parse last pickup date
                try {
                    @SuppressLint("SimpleDateFormat") val apiDateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd") // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") val showingDateFormat: DateFormat = SimpleDateFormat("dd.MM.yyyy")
                    val timeStamp = jsonChildNode.optString("lastPickupDate")
                    val parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp))
                    val parsedDate = showingDateFormat.format(parseTimeDate)
                    parcelObject.lastPickupDate = parsedDate
                } catch (e: Exception) {
                    Log.i(TAG, e.toString())
                }

                if (jsonChildNode.getJSONObject("product").has("name")) {
                    if (!jsonChildNode.getJSONObject("product").getString("name").contains("null")) {
                        parcelObject.product = jsonChildNode.getJSONObject("product").getJSONObject("name").optString("fi")
                    }
                }
                parcelObject.sender = jsonChildNode.optString("sender")
                parcelObject.lockerCode = jsonChildNode.optString("lockerCode")
                val extraServicesArray = jsonChildNode.getJSONArray("extraServices")
                if (extraServicesArray.length() > 0) {
                    val extraServiceObj = extraServicesArray.getJSONObject(0) // Get only first
                    if (extraServiceObj != null) {
                        val extraServiceNameObj = extraServiceObj.optJSONObject("name")
                        if (extraServiceNameObj != null) {
                            parcelObject.extraServices = extraServiceNameObj.optString("fi")
                        }
                    }
                }
                parcelObject.weight = jsonChildNode.optString("weight")
                parcelObject.height = jsonChildNode.optString("height")
                parcelObject.width = jsonChildNode.optString("width")
                parcelObject.depth = jsonChildNode.optString("depth")
                parcelObject.volume = jsonChildNode.optString("volume")
                parcelObject.destinationPostcode = jsonChildNode.optString("destinationPostcode")
                parcelObject.destinationCity = jsonChildNode.optString("destinationCity")
                parcelObject.destinationCountry = jsonChildNode.optString("destinationCountry")
                parcelObject.recipientSignature = jsonChildNode.optString("recipientSignature")
                parcelObject.codAmount = jsonChildNode.optString("codAmount")
                parcelObject.codCurrency = jsonChildNode.optString("codCurrency")


                // Parse events
                val eventsArray = jsonChildNode.optJSONArray("events")
                @SuppressLint("SimpleDateFormat") val apiDateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                @SuppressLint("SimpleDateFormat") val showingDateFormat: DateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                @SuppressLint("SimpleDateFormat") val SQLiteDateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                for (i in 0 until eventsArray.length()) {
                    val event = eventsArray.getJSONObject(i)
                    val timeStamp = event.optString("timestamp")
                    val parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp))
                    val parsedDate = showingDateFormat.format(parseTimeDate)
                    val parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate)

                    // Log.i(TAG, "After parsing date format is: " + parsedDate);
                    // Log.i(TAG, "After parsing SQLite date format is: " + parsedDateSQLiteFormat);
                    val eventJsonObj = eventsArray.optJSONObject(i)
                    val eventObj = eventJsonObj.optJSONObject("description")
                    var eventDescription: String? = "-"
                    if (eventObj != null) {
                        eventDescription = eventObj.optString("fi")
                    }

                    // Pass to object
                    val eventObject = EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, event.optString("locationCode"), event.optString("locationName")
                    )

                    // Add object
                    eventObjects.add(eventObject)
                }
                parcelObject.eventObjects = eventObjects // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "Posti shipment not found")
                parcelObject.isFound = false // Parcel not found
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.i(TAG, e.toString())
        } catch (e: SSLException) {
            Log.i(TAG, "RESET BY PEER FOR $parcelCode")
            parcelObject.updateFailed = true
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return parcelObject
    }


} // End of class