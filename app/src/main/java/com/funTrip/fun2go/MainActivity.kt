package com.funTrip.fun2go.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Spot
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: MainViewModel
    private lateinit var googleMap: GoogleMap

    // UI
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var ivUserAvatar: ImageView
    private lateinit var btnRefreshUser: ImageButton
    private lateinit var chipGroup: LinearLayout

    // 地圖資料
    private var allSpots: List<Spot> = emptyList()
    private val markerSpotMap = HashMap<Marker, Spot>()
    private var selectedCategory = "all"

    // 分類對應表
    private val categoryMap = linkedMapOf(
        "all"         to "全部",
        "attraction"  to "景點",
        "restaurant"  to "餐廳",
        "night_market" to "夜市",
        "shopping"    to "購物",
        "cafe"        to "咖啡廳"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupCategoryChips()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initViewModel()

        viewModel.fetchUser(1)
        viewModel.fetchAllSpots()
    }

    private fun initViews() {
        progressBar   = findViewById(R.id.progressBar)
        tvWelcome     = findViewById(R.id.tvWelcome)
        ivUserAvatar  = findViewById(R.id.ivUserAvatar)
        btnRefreshUser = findViewById(R.id.btnRefreshUser)
        chipGroup     = findViewById(R.id.chipGroup)

        btnRefreshUser.setOnClickListener {
            viewModel.fetchUser(1)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnListView)
            .setOnClickListener {
                Toast.makeText(this, "列表功能開發中", Toast.LENGTH_SHORT).show()
            }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener {
                Toast.makeText(this, "新增行程功能開發中", Toast.LENGTH_SHORT).show()
            }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnZoomIn)
            .setOnClickListener {
                if (::googleMap.isInitialized)
                    googleMap.animateCamera(CameraUpdateFactory.zoomIn())
            }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnZoomOut)
            .setOnClickListener {
                if (::googleMap.isInitialized)
                    googleMap.animateCamera(CameraUpdateFactory.zoomOut())
            }
    }

    private fun setupCategoryChips() {
        categoryMap.forEach { (key, label) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = (key == "all")
                chipCornerRadius = 48f
                setTextColor(if (key == "all") getColor(android.R.color.white) else getColor(android.R.color.black))
                setChipBackgroundColorResource(
                    if (key == "all") android.R.color.holo_red_light else android.R.color.white
                )
                chipStrokeWidth = 1f
                setChipStrokeColorResource(android.R.color.darker_gray)
            }
            chip.setOnClickListener {
                // 更新所有 chip 樣式
                for (i in 0 until chipGroup.childCount) {
                    val c = chipGroup.getChildAt(i) as Chip
                    val isSelected = c == chip
                    c.setTextColor(getColor(if (isSelected) android.R.color.white else android.R.color.black))
                    c.setChipBackgroundColorResource(
                        if (isSelected) android.R.color.holo_red_light else android.R.color.white
                    )
                }
                selectedCategory = key
                filterAndPlaceMarkers()
            }
            chipGroup.addView(chip)
        }
    }

    // ─── Google Maps ───────────────────────────────────────────

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isZoomGesturesEnabled = true

        // 預設鏡頭：台北市中心，zoom 14 可看到街道名稱
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.0330, 121.5654), 14f))

        googleMap.setOnMarkerClickListener { marker ->
            markerSpotMap[marker]?.let { showSpotBottomSheet(it) }
            true
        }

        // 若景點已先載入則立即打點
        if (allSpots.isNotEmpty()) filterAndPlaceMarkers()
    }

    private fun filterAndPlaceMarkers() {
        val filtered = if (selectedCategory == "all") allSpots
                       else allSpots.filter { it.category == selectedCategory }
        placeMarkers(filtered)
    }

    private fun placeMarkers(spots: List<Spot>) {
        if (!::googleMap.isInitialized) return
        googleMap.clear()
        markerSpotMap.clear()
        spots.forEach { spot ->
            val lat = spot.latitude?.toDoubleOrNull() ?: return@forEach
            val lng = spot.longitude?.toDoubleOrNull() ?: return@forEach
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, lng))
                    .title(spot.name)
            )
            marker?.let { markerSpotMap[it] = spot }
        }
    }

    // ─── 景點詳情 BottomSheet ──────────────────────────────────

    private fun showSpotBottomSheet(spot: Spot) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_spot, null)

        view.findViewById<TextView>(R.id.tvSpotName).text = spot.name
        view.findViewById<Chip>(R.id.chipCategory).text =
            categoryMap[spot.category] ?: spot.category ?: "景點"
        view.findViewById<TextView>(R.id.tvSpotAddress).text =
            spot.address ?: "無地址資訊"

        val tvRating = view.findViewById<TextView>(R.id.tvSpotRating)
        if (spot.rating != null) {
            tvRating.text = "★ ${spot.rating}"
            tvRating.visibility = View.VISIBLE
        }

        dialog.setContentView(view)
        dialog.show()
    }

    // ─── ViewModel Observers ──────────────────────────────────

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.userResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    tvWelcome.text = response.data?.name ?: "遊客"
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "用戶讀取失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.spotsResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    allSpots = response.data ?: emptyList()
                    filterAndPlaceMarkers()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "載入景點失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
