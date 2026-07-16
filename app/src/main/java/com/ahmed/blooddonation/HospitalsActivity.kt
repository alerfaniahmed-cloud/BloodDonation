package com.ahmed.blooddonation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HospitalsActivity : AppCompatActivity() {

    private val allHospitals = listOf(
        // الرياض
        Hospital("مستشفى الملك فهد الطبي التخصصي", "الرياض", "0114322222"),
        Hospital("مستشفى الملك فيصل التخصصي ومركز الأبحاث", "الرياض", "0114647272"),
        Hospital("مدينة الملك سعود الطبية", "الرياض", "0114355555"),
        Hospital("مستشفى الملك خالد الجامعي", "الرياض", "0114670000"),
        Hospital("مستشفى الحرس الوطني بالرياض", "الرياض", "0118011111"),

        // جدة
        Hospital("مستشفى الملك عبدالعزيز الجامعي", "جدة", "0126408200"),
        Hospital("مستشفى جدة العام", "جدة", "0126653571"),
        Hospital("مستشفى الملك فهد بجدة", "جدة", "0126657000"),

        // مكة المكرمة
        Hospital("مستشفى الملك عبدالعزيز بمكة", "مكة المكرمة", "0125271122"),
        Hospital("مستشفى النور التخصصي", "مكة المكرمة", "0125300888"),

        // المدينة المنورة
        Hospital("مستشفى الملك سلمان بالمدينة المنورة", "المدينة المنورة", "0148459000"),
        Hospital("مستشفى الأنصار العام", "المدينة المنورة", "0148225555"),

        // الدمام والمنطقة الشرقية
        Hospital("مستشفى الملك فهد التخصصي بالدمام", "الدمام", "0138422222"),
        Hospital("مستشفى الدمام المركزي", "الدمام", "0138335000"),
        Hospital("مستشفى الملك فهد الجامعي بالخبر", "الخبر", "0138966666"),

        // حفر الباطن
        Hospital("مستشفى حفر الباطن المركزي", "حفر الباطن", "0137310000"),

        // الطائف
        Hospital("مستشفى الطائف العام", "الطائف", "0127362600"),
        Hospital("مستشفى الملك عبدالعزيز التخصصي بالطائف", "الطائف", "0127374000"),

        // أبها وعسير
        Hospital("مستشفى عسير المركزي", "أبها", "0172247777"),

        // تبوك
        Hospital("مستشفى الملك خالد بتبوك", "تبوك", "0144270000"),

        // القصيم (بريدة)
        Hospital("مستشفى الملك فهد التخصصي ببريدة", "بريدة", "0163258000"),

        // نجران
        Hospital("مستشفى الملك خالد بنجران", "نجران", "0175232000"),

        // جازان
        Hospital("مستشفى الأمير محمد بن ناصر بجازان", "جازان", "0173218888")
    )

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospitals)

        recyclerView = findViewById(R.id.hospitalsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HospitalAdapter(allHospitals)

        val searchInput = findViewById<EditText>(R.id.hospitalSearchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterHospitals(s.toString())
            }
        })
    }

    private fun filterHospitals(query: String) {
        val filtered = if (query.isBlank()) {
            allHospitals
        } else {
            allHospitals.filter {
                it.name.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true)
            }
        }
        recyclerView.adapter = HospitalAdapter(filtered)
    }
}
