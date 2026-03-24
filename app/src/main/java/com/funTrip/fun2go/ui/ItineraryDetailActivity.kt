package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
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
import com.funTrip.fun2go.data.model.UpdateSpotInDayRequest
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.ItineraryDayAdapter
import com.funTrip.fun2go.ui.adapter.SpotPickerAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText

class ItineraryDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var listContainer: FrameLayout
    private lateinit var mapContainer: FrameLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmptyDays: TextView
    private lateinit var rvDays: RecyclerView
    private lateinit var dayAdapter: ItineraryDayAdapter

    private var itineraryId: Int = -1
    private var currentSavedSpots: List<SavedSpotEntity> = emptyList()
    private var datePromptShown = false
    private var currentItinerary: Itinerary? = null

    private var googleMap: GoogleMap? = null
    private var mapFragmentAdded = false

    // 每天對應的 Marker 顏色（Hue）與路徑連線顏色
    private val dayMarkerHues = floatArrayOf(
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_ROSE,
        BitmapDescriptorFactory.HUE_MAGENTA,
        BitmapDescriptorFactory.HUE_AZURE,
        BitmapDescriptorFactory.HUE_YELLOW
    )
    private val dayPolylineColors = intArrayOf(
        0xFFE53935.toInt(),
        0xFF1E88E5.toInt(),
        0xFF43A047.toInt(),
        0xFFFB8C00.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00ACC1.toInt(),
        0xFFE91E63.toInt(),
        0xFF8D6E63.toInt(),
        0xFF039BE5.toInt(),
        0xFFC0CA33.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail)

        itineraryId = intent.getIntExtra("itinerary_id", -1)
        val itineraryTitle = intent.getStringExtra("itinerary_title") ?: getString(R.string.default_itin_title)

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        listContainer = findViewById(R.id.listContainer)
        mapContainer = findViewById(R.id.mapContainer)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmptyDays = findViewById(R.id.tvEmptyDays)
        rvDays = findViewById(R.id.rvDays)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = itineraryTitle
        }

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_list_view)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_map_view)))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showListMode()
                    1 -> showMapMode()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        dayAdapter = ItineraryDayAdapter(
            onAddSpotClick = { day -> showSpotPickerSheet(day) },
            onRemoveSpotClick = { itSpot, dayId -> showRemoveSpotDialog(itSpot, dayId) },
            onDateClick = { day -> showDatePickerForDay(day) },
            onSpotClick = { itSpot, dayId -> showEditSpotSheet(itSpot, dayId) }
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
                        // 若目前在地圖模式，同步更新 markers
                        if (mapContainer.visibility == View.VISIBLE) {
                            googleMap?.let { updateMapMarkers(it) }
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

        viewModel.updateSpotAttrResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> pbLoading.visibility = View.GONE
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    Snackbar.make(toolbar, getString(R.string.msg_spot_update_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
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

    // ── 地圖 / 列表 切換 ─────────────────────────────────────────────────────

    private fun showListMode() {
        listContainer.visibility = View.VISIBLE
        mapContainer.visibility = View.GONE
    }

    private fun showMapMode() {
        listContainer.visibility = View.GONE
        mapContainer.visibility = View.VISIBLE
        initMapIfNeeded()
    }

    private fun initMapIfNeeded() {
        if (mapFragmentAdded) return
        mapFragmentAdded = true
        val mapFrag = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFrag)
            .commit()
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        updateMapMarkers(map)
    }

    private fun updateMapMarkers(map: GoogleMap) {
        val days = currentItinerary?.days ?: return
        map.clear()
        val boundsBuilder = LatLngBounds.Builder()
        var hasAnyPoint = false

        days.forEachIndexed { dayIndex, day ->
            val lineColor = dayPolylineColors[dayIndex % dayPolylineColors.size]
            val spots = day.spots ?: return@forEachIndexed
            val validPoints = mutableListOf<LatLng>()

            spots.forEachIndexed { spotIndex, itSpot ->
                val lat = itSpot.spot_detail?.latitude?.toDoubleOrNull() ?: return@forEachIndexed
                val lng = itSpot.spot_detail?.longitude?.toDoubleOrNull() ?: return@forEachIndexed
                val latlng = LatLng(lat, lng)
                validPoints.add(latlng)
                boundsBuilder.include(latlng)
                hasAnyPoint = true
                map.addMarker(
                    MarkerOptions()
                        .position(latlng)
                        .title(itSpot.spot_detail?.name ?: "")
                        .snippet(getString(R.string.day_label, day.day_number))
                        .icon(BitmapDescriptorFactory.fromBitmap(
                            createNumberedMarker(spotIndex + 1, lineColor)
                        ))
                )
            }

            if (validPoints.size >= 2) {
                map.addPolyline(
                    PolylineOptions()
                        .addAll(validPoints)
                        .color(lineColor)
                        .width(6f)
                        .geodesic(true)
                )
                // 每段路線中點顯示行車時間
                for (i in 0 until validPoints.size - 1) {
                    val midLat = (validPoints[i].latitude + validPoints[i + 1].latitude) / 2
                    val midLng = (validPoints[i].longitude + validPoints[i + 1].longitude) / 2
                    val timeText = calcTravelTime(validPoints[i], validPoints[i + 1])
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(midLat, midLng))
                            .icon(BitmapDescriptorFactory.fromBitmap(createTimeLabel(timeText)))
                            .anchor(0.5f, 0.5f)
                            .zIndex(0f)
                    )
                }
            }
        }

        if (hasAnyPoint) {
            mapContainer.post {
                runCatching {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))
                }
            }
        }
    }

    /** 建立帶數字的圓形 Marker bitmap */
    private fun createNumberedMarker(number: Int, color: Int): Bitmap {
        val size = (48 * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 白色外框
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // 彩色填充
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f * resources.displayMetrics.density, paint)

        // 數字
        paint.color = Color.WHITE
        paint.textSize = size * 0.42f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val textY = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(number.toString(), size / 2f, textY, paint)

        return bmp
    }

    /** 建立行車時間文字標籤 bitmap */
    private fun createTimeLabel(text: String): Bitmap {
        val density = resources.displayMetrics.density
        val padding = (6 * density).toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11 * density
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()
        val w = (textWidth + padding * 2).toInt()
        val h = (textHeight + padding * 1.5f).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 深色半透明背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC333333.toInt() }
        val r = h / 2f
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, bgPaint)

        // 文字
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, w / 2f, h / 2f - (paint.descent() + paint.ascent()) / 2, paint)

        return bmp
    }

    /** Haversine 計算行車時間（與 ItineraryDayAdapter 邏輯一致） */
    private fun calcTravelTime(a: LatLng, b: LatLng): String {
        val R = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val av = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2).pow(2)
        val roadKm = R * 2 * atan2(sqrt(av), sqrt(1 - av)) * 1.3
        val minutes = (roadKm / 25.0 * 60).toInt().coerceAtLeast(1)
        return if (minutes < 60)
            getString(R.string.format_travel_min, minutes)
        else
            getString(R.string.format_travel_hm, minutes / 60, minutes % 60)
    }

    // ────────────────────────────────────────────────────────────────────────

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

    private fun showEditSpotSheet(itSpot: ItinerarySpot, dayId: Int) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_itinerary_spot, null)

        val tvTitle       = sheetView.findViewById<TextView>(R.id.tvEditSpotTitle)
        val etArrival     = sheetView.findViewById<TextInputEditText>(R.id.etArrivalTime)
        val etDeparture   = sheetView.findViewById<TextInputEditText>(R.id.etDepartureTime)
        val etDuration    = sheetView.findViewById<TextInputEditText>(R.id.etDurationMinutes)
        val etNote        = sheetView.findViewById<TextInputEditText>(R.id.etSpotNote)
        val btnSave       = sheetView.findViewById<MaterialButton>(R.id.btnSaveSpotAttr)
        val btnCancel     = sheetView.findViewById<MaterialButton>(R.id.btnCancelSpotAttr)

        tvTitle.text = itSpot.spot_detail?.name ?: getString(R.string.title_edit_spot)

        etArrival.setText(itSpot.arrival_time ?: "")
        etDeparture.setText(itSpot.departure_time ?: "")
        etDuration.setText(itSpot.duration_minutes?.toString() ?: "")
        etNote.setText(itSpot.note ?: "")

        fun showTimePicker(field: TextInputEditText) {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                field.setText("%02d:%02d".format(hour, minute))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }
        etArrival.setOnClickListener { showTimePicker(etArrival) }
        etDeparture.setOnClickListener { showTimePicker(etDeparture) }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val req = UpdateSpotInDayRequest(
                arrival_time   = etArrival.text?.toString()?.trim()?.ifEmpty { null },
                departure_time = etDeparture.text?.toString()?.trim()?.ifEmpty { null },
                duration_minutes = etDuration.text?.toString()?.trim()?.toIntOrNull(),
                note           = etNote.text?.toString()?.trim()?.ifEmpty { null }
            )
            dialog.dismiss()
            viewModel.updateItinerarySpot(itineraryId, dayId, itSpot.id, req)
        }

        dialog.setContentView(sheetView)
        dialog.show()
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
