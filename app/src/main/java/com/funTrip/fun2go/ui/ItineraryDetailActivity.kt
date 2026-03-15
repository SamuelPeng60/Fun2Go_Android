package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.local.SavedSpotEntity
import com.funTrip.fun2go.data.model.Itinerary
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
    private var datePromptShown = false
    private var currentItinerary: Itinerary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail)

        itineraryId = intent.getIntExtra("itinerary_id", -1)
        val itineraryTitle = intent.getStringExtra("itinerary_title") ?: getString(R.string.default_itin_title)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val itinerary = currentItinerary
        val currentUserId = viewModel.currentUser?.id ?: 0
        // 只有行程擁有者才顯示發佈按鈕
        if (itinerary != null && currentUserId > 0 && itinerary.author_id == currentUserId) {
            menuInflater.inflate(R.menu.menu_itinerary_detail, menu)
            val publishItem = menu.findItem(R.id.action_publish_itinerary)
            if (itinerary.is_public) {
                publishItem.title = getString(R.string.menu_unpublish_itin)
            } else {
                publishItem.title = getString(R.string.menu_publish_itin)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_publish_itinerary -> {
                val itinerary = currentItinerary ?: return true
                if (itinerary.is_public) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.title_unpublish_itin_confirm))
                        .setMessage(getString(R.string.msg_unpublish_itin_confirm))
                        .setPositiveButton(getString(R.string.label_unpublish)) { _, _ ->
                            viewModel.unpublishItinerary(itinerary)
                        }
                        .setNegativeButton(getString(R.string.label_cancel), null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.title_publish_itin_confirm))
                        .setMessage(getString(R.string.msg_publish_itin_confirm))
                        .setPositiveButton(getString(R.string.label_publish)) { _, _ ->
                            viewModel.publishItinerary(itinerary.id)
                        }
                        .setNegativeButton(getString(R.string.label_cancel), null)
                        .show()
                }
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
                        currentItinerary = itinerary
                        invalidateOptionsMenu()
                        supportActionBar?.title = itinerary.title
                        val days = itinerary.days ?: emptyList()
                        // 偵測複製過來但尚未設定日期的行程（全部天數 date 為 null）
                        if (!datePromptShown && days.isNotEmpty() && days.all { it.date.isNullOrBlank() }) {
                            datePromptShown = true
                            showStartDatePrompt()
                        }
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
                    Snackbar.make(toolbar, getString(R.string.msg_load_detail_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
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
                        getString(R.string.msg_spot_not_yours) else result.message ?: getString(R.string.msg_operation_failed)
                    Snackbar.make(toolbar, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.updateDayResult.observe(this) { result ->
            if (result is NetworkResult.Error) {
                Snackbar.make(toolbar, getString(R.string.msg_update_date_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.savedSpots.observe(this) { spots ->
            currentSavedSpots = spots
        }

        viewModel.reorderResult.observe(this) { result ->
            if (result is NetworkResult.Error)
                Snackbar.make(toolbar, getString(R.string.msg_reorder_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
        }

        viewModel.initDaysResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    viewModel.loadItinerary(itineraryId)
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    viewModel.loadItinerary(itineraryId)
                }
            }
        }

        viewModel.publishResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val updated = result.data ?: return@observe
                    currentItinerary = updated
                    invalidateOptionsMenu()
                    val msg = if (updated.is_public)
                        getString(R.string.msg_itin_published)
                    else
                        getString(R.string.msg_itin_unpublished)
                    Snackbar.make(toolbar, msg, Snackbar.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    Snackbar.make(toolbar, getString(R.string.msg_publish_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
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

    private fun showStartDatePrompt() {
        Toast.makeText(this, getString(R.string.msg_set_start_date_hint), Toast.LENGTH_SHORT).show()
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val dateStr = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            viewModel.setDatesAfterCopy(itineraryId, dateStr)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setOnCancelListener { datePromptShown = false }
        }.show()
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
        val spotName = itSpot.spot_detail?.name ?: ""
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_remove_spot))
            .setMessage(getString(R.string.msg_remove_spot_confirm, spotName))
            .setPositiveButton(getString(R.string.label_remove)) { _, _ ->
                viewModel.removeSpotFromDay(itineraryId, dayId, itSpot.id)
            }
            .setNegativeButton(getString(R.string.label_cancel), null)
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
