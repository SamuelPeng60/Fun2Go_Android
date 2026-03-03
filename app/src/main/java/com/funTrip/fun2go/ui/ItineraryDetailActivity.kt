package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import java.util.Calendar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.local.SavedSpotEntity
import com.funTrip.fun2go.data.model.ItineraryDay
import com.funTrip.fun2go.data.model.ItinerarySpot
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.ItineraryDayAdapter
import com.funTrip.fun2go.ui.adapter.SpotPickerAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar

class ItineraryDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmptyDays: TextView
    private lateinit var rvDays: RecyclerView
    private lateinit var dayAdapter: ItineraryDayAdapter

    private var itineraryId: Int = -1
    private var currentSavedSpots: List<SavedSpotEntity> = emptyList()

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

        dayAdapter = ItineraryDayAdapter(
            onAddSpotClick = { day -> showSpotPickerSheet(day) },
            onRemoveSpotClick = { itSpot, dayId -> showRemoveSpotDialog(itSpot, dayId) },
            onDateClick = { day -> showDatePickerForDay(day) }
        )
        rvDays.layoutManager = LinearLayoutManager(this)
        rvDays.adapter = dayAdapter
        setupDragDrop()

        viewModel = ViewModelProvider(this)[ItineraryViewModel::class.java]
        setupObservers()

        if (itineraryId != -1) {
            viewModel.loadItinerary(itineraryId)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
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
                    Snackbar.make(toolbar, "載入失敗：${result.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.spotOperationResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> pbLoading.visibility = View.GONE
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    val msg = if (result.message?.contains("Forbidden", ignoreCase = true) == true)
                        "此行程不屬於您，無法修改景點" else result.message ?: "操作失敗"
                    Snackbar.make(toolbar, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.updateDayResult.observe(this) { result ->
            if (result is NetworkResult.Error) {
                Snackbar.make(toolbar, "更新日期失敗：${result.message}", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.savedSpots.observe(this) { spots ->
            currentSavedSpots = spots
        }

        viewModel.reorderResult.observe(this) { result ->
            if (result is NetworkResult.Error)
                Snackbar.make(toolbar, "排序同步失敗：${result.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupDragDrop() {
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder) =
                if (vh is ItineraryDayAdapter.SpotViewHolder)
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                else makeMovementFlags(0, 0)

            override fun isLongPressDragEnabled() = true

            override fun onMove(
                rv: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = dayAdapter.onSpotMoved(source.adapterPosition, target.adapterPosition)

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG)
                    vh?.itemView?.elevation = 8f
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.elevation = 0f
                dayAdapter.commitDragOrder { dayId, spotIds ->
                    viewModel.reorderSpots(dayId, spotIds)
                }
            }
        }
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(rvDays)
        dayAdapter.itemTouchHelper = helper
    }

    private fun showDatePickerForDay(itDay: ItineraryDay) {
        val cal = Calendar.getInstance()
        itDay.date?.takeIf { it.isNotBlank() }?.let { dateStr ->
            runCatching {
                val parts = dateStr.split("-")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            }
        }
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val dateStr = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            viewModel.updateDayDate(itineraryId, itDay.id, dateStr)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showRemoveSpotDialog(itSpot: ItinerarySpot, dayId: Int) {
        val spotName = itSpot.spot_detail?.name ?: "此景點"
        AlertDialog.Builder(this)
            .setTitle("移除景點")
            .setMessage("確定要從此天移除「$spotName」？")
            .setPositiveButton("移除") { _, _ ->
                viewModel.removeSpotFromDay(itineraryId, dayId, itSpot.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSpotPickerSheet(day: ItineraryDay) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_spot_picker, null)
        val rvPicker = sheetView.findViewById<RecyclerView>(R.id.rvSpotPicker)
        val tvEmpty = sheetView.findViewById<TextView>(R.id.tvEmptySavedSpots)

        val pickerAdapter = SpotPickerAdapter { savedSpot ->
            dialog.dismiss()
            val orderIndex = (day.spots?.size ?: 0) + 1
            viewModel.addSpotToDay(itineraryId, day.id, savedSpot.id, orderIndex)
        }
        rvPicker.layoutManager = LinearLayoutManager(this)
        rvPicker.adapter = pickerAdapter

        val currentSpots = currentSavedSpots
        if (currentSpots.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvPicker.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvPicker.visibility = View.VISIBLE
            pickerAdapter.submitList(currentSpots)
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

}
