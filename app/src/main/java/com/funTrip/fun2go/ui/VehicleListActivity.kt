package com.funTrip.fun2go.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.VehicleAdapter
import com.funTrip.fun2go.ui.viewmodel.VehicleViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class VehicleListActivity : AppCompatActivity() {

    private lateinit var viewModel: VehicleViewModel
    private lateinit var adapter: VehicleAdapter
    private lateinit var rvVehicles: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView

    private var currentType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "租車"
        }

        pbLoading  = findViewById(R.id.pbLoading)
        tvEmpty    = findViewById(R.id.tvEmpty)
        rvVehicles = findViewById(R.id.rvVehicles)

        adapter = VehicleAdapter { /* item click – reserved for detail page */ }
        rvVehicles.layoutManager = LinearLayoutManager(this)
        rvVehicles.adapter = adapter

        setupChips()

        viewModel = ViewModelProvider(this)[VehicleViewModel::class.java]
        viewModel.vehicles.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility  = View.VISIBLE
                    rvVehicles.visibility = View.GONE
                    tvEmpty.visibility    = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        tvEmpty.visibility    = View.VISIBLE
                        rvVehicles.visibility = View.GONE
                    } else {
                        tvEmpty.visibility    = View.GONE
                        rvVehicles.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility  = View.GONE
                    tvEmpty.text          = "載入失敗：${result.message}"
                    tvEmpty.visibility    = View.VISIBLE
                    rvVehicles.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchVehicles(type = currentType, available = if (currentType != null) true else null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun setupChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFilter)

        data class FilterChip(val label: String, val type: String?)
        val filters = listOf(
            FilterChip("全部",    null),
            FilterChip("轎車",    "sedan_4"),
            FilterChip("九人座",  "van_9"),
            FilterChip("巴士",   "bus_20")
        )

        filters.forEach { filter ->
            val chip = Chip(this).apply {
                text = filter.label
                isCheckable = true
                isChecked = (filter.type == null)
            }
            chip.setOnClickListener {
                currentType = filter.type
                viewModel.fetchVehicles(
                    type = currentType,
                    available = if (currentType != null) true else null
                )
            }
            chipGroup.addView(chip)
        }
    }
}
