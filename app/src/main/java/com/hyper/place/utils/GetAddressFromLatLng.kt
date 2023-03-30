package com.hyper.place.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale


class GetAddressFromLatLng(context: Context,private val latitude : Double,
                           private val longitude :Double) {
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var mAddressListener: AddressListener



    private suspend fun doInBackground() : String{
        try {

            val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addressList.isNullOrEmpty()) {
                val address: Address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(",")
                }
                sb.deleteCharAt(sb.length - 1) // Here we remove the last comma that we have added above from the address.
                return sb.toString()
            }
        } catch (e: IOException) {
            Log.e("HappyPlaces", "Unable connect to Geocoder")
        }

        return ""
    }

    private suspend fun onPostExecute(resultString: String){
        if(resultString == null){
            mAddressListener.onError()
        }else {
            mAddressListener.onAddressFound(resultString)
        }
    }


    fun setAddressListener(addressListener: AddressListener) {
        mAddressListener = addressListener
    }

     fun getAddress() {
        GlobalScope.launch(Dispatchers.IO) {
            val result = doInBackground()
            onPostExecute(result)
        }
    }


    interface AddressListener {
        fun onAddressFound(address: String?)
        fun onError()
    }
}