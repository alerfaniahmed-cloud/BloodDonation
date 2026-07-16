package com.ahmed.blooddonation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HospitalsActivity : AppCompatActivity() {

    private val allHospitals = listOf(
        // الرياض
        Hospital("مستشفى الملك فهد الطبي التخصصي", "الرياض", "0114322222", 24.6877, 46.7219),
        Hospital("مستشفى الملك فيصل التخصصي ومركز الأبحاث", "الرياض", "0114647272", 24.6690, 46.7076),
        Hospital("مدينة الملك سعود الطبية", "الرياض", "0114355555", 24.6595, 46.7626),
        Hospital("مستشفى الملك خالد الجامعي", "الرياض", "0114670000", 24.7136, 46.6386),
        Hospital("مستشفى الحرس الوطني بالرياض", "الرياض", "0118011111", 24.7580, 46.6350),

        // جدة
        Hospital("مستشفى الملك عبدالعزيز الجامعي", "جدة", "0126408200", 21.5433, 39.1728),
        Hospital("مستشفى جدة العام", "جدة", "0126653571", 21.4858, 39.1925),
        Hospital("مستشفى الملك فهد بجدة", "جدة", "0126657000", 21.5760, 39.1925),

        // مكة المكرمة
        Hospital("مستشفى الملك عبدالعزيز بمكة", "مكة المكرمة", "0125271122", 21.3891, 39.8579),
        Hospital("مستشفى النور التخصصي", "مكة المكرمة", "0125300888", 21.4241, 39.8173),

        // المدينة المنورة
        Hospital("مستشفى الملك سلمان بالمدينة المنورة", "المدينة المنورة", "0148459000", 24.5247, 39.5692),
        Hospital("مستشفى الأنصار العام", "المدينة المنورة", "0148225555", 24.4672, 39.6024),

        // الدمام والمنطقة الشرقية
        Hospital("مستشفى الملك فهد التخصصي بالدمام", "الدمام", "0138422222", 26.4207, 50.0888),
        Hospital("مستشفى الدمام المركزي", "الدمام", "0138335000", 26.4344, 50.1033),
        Hospital("مستشفى الملك فهد الجامعي بالخبر", "الخبر", "0138966666", 26.2172, 50.1971),

        // حفر الباطن
        Hospital("مستشفى حفر الباطن المركزي", "حفر الباطن", "0137310000", 28.4342, 45.9601),

        // الطائف
        Hospital("مستشفى الطائف العام", "الطائف", "0127362600", 21.2854, 40.4183),
        Hospital("مستشفى الملك عبدالعزيز التخصصي بالطائف", "الطائف", "0127374000", 21.2703, 40.3894),

        // أبها وعسير
        Hospital("مستشفى عسير المركزي", "أبها", "0172247777", 18.2465, 42.5117),

        // تبوك
        Hospital("مستشفى الملك خالد بتبوك", "تبوك", "0144270000", 28.3998, 36.5715),

        // القصيم (بريدة)
        Hospital("مستشفى الملك فهد التخصصي ببريدة", "بريدة", "0163258000", 26.3260, 43.9750),

        // نجران
        Hospital("مستشفى الملك خالد بنجران", "نجران", "0175232000", 17.5656, 44.2289),

        // جازان
        Hospital("مستشفى الأمير محمد بن ناصر بجازان", "جازان", "0173218888", 16.8892, 42.5611)
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private var userLocation: Location? = null
    private var currentList: List<Hospital> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospitals)

        recyclerView = findViewById(R.id.hospitalsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        currentList = allHospitals
        recyclerView.adapter = HospitalAdapter(currentList)

        searchInput = findViewById(R.id.hospitalSearchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterHospitals(s.toString())
            }
        })

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation()
            }
        }
    }

    private fun getUserLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)

            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                    bestLocation = location
                }
            }

            userLocation = bestLocation
            sortHospitalsByDistance()
        } catch (e: SecurityException) {
            // الصلاحية مرفوضة، القائمة تبقى بدون ترتيب حسب المسافة
        }
    }

    private fun sortHospitalsByDistance() {
        val loc = userLocation ?: return
        currentList = allHospitals.sortedBy { hospital ->
            distanceInKm(loc.latitude, loc.longitude, hospital.lat, hospital.lng)
        }
        recyclerView.adapter = HospitalAdapter(currentList)
    }

    private fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun filterHospitals(query: String) {
        val baseList = if (userLocation != null) {
            allHospitals.sortedBy {
                distanceInKm(userLocation!!.latitude, userLocation!!.longitude, it.lat, it.lng)
            }
        } else {
            allHospitals
        }

        val filtered = if (query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true)
            }
        }
        currentList = filtered
        recyclerView.adapter = HospitalAdapter(filtered)
    }
}
