package com.funTrip.fun2go.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.ItineraryDayAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

class ItineraryDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmptyDays: TextView
    private lateinit var rvDays: RecyclerView
    private lateinit var dayAdapter: ItineraryDayAdapter

    private var itineraryId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail)

        itineraryId = intent.getIntExtra("itinerary_id", -1)
        val itineraryTitle = intent.getStringExtra("itinerary_title") ?: "行程詳情"

        toolbar = findViewById(R.id.toolbar)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmptyDays = findViewById(R.id.tvEmptyDays)
        rvDays = findViewById(R.id.rvDays)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = itineraryTitle
        }

        dayAdapter = ItineraryDayAdapter()
        rvDays.layoutManager = LinearLayoutManager(this)
        rvDays.adapter = dayAdapter

        viewModel = ViewModelProvider(this)[ItineraryViewModel::class.java]
        setupObservers()

        if (itineraryId != -1) {
            viewModel.loadItinerary(itineraryId)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_itinerary_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete_itinerary -> {
                showDeleteConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers() {
        viewModel.itineraryDetail.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                    rvDays.visibility = View.GONE
                    tvEmptyDays.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val itinerary = result.data
                    if (itinerary != null) {
                        supportActionBar?.title = itinerary.title
                        val days = itinerary.days ?: emptyList()
                        if (days.isEmpty()) {
                            tvEmptyDays.visibility = View.VISIBLE
                            rvDays.visibility = View.GONE
                        } else {
                            tvEmptyDays.visibility = View.GONE
                            rvDays.visibility = View.VISIBLE
                            dayAdapter.submitList(days)
                        }
                    } else {
                        tvEmptyDays.visibility = View.VISIBLE
                        rvDays.visibility = View.GONE
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    tvEmptyDays.visibility = View.GONE
                    rvDays.visibility = View.GONE
                    Snackbar.make(
                        toolbar,
                        "載入失敗：${result.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    finish()
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    Snackbar.make(
                        toolbar,
                        "刪除失敗：${result.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("刪除行程")
            .setMessage("確定要刪除此行程嗎？此操作無法復原。")
            .setPositiveButton("刪除") { _, _ ->
                viewModel.deleteItinerary(itineraryId)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
