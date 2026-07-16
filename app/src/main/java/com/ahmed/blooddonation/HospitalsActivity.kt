package com.ahmed.blooddonation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HospitalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospitals)

        val recyclerView = findViewById<RecyclerView>(R.id.hospitalsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val hospitals = listOf(
            Hospital("مستشفى الملك فهد", "الرياض", "0112345678"),
            Hospital("مستشفى الملك فيصل التخصصي", "الرياض", "0112345679"),
            Hospital("مستشفى الملك عبدالعزيز", "جدة", "0122345678"),
            Hospital("مستشفى حفر الباطن المركزي", "حفر الباطن", "0132345678")
        )

        recyclerView.adapter = HospitalAdapter(hospitals)
    }
}
