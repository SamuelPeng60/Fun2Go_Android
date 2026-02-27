package com.funTrip.fun2go.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.DistanceInfo
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.model.Spot
import com.funTrip.fun2go.data.model.User
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.ItineraryDetailActivity
import com.funTrip.fun2go.ui.ItineraryListActivity
import com.funTrip.fun2go.ui.adapter.SavedListItem
import com.funTrip.fun2go.ui.adapter.SavedSpotAdapter
import com.funTrip.fun2go.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: MainViewModel
    private lateinit var googleMap: GoogleMap

    // ─── Google Sign-In ────────────────────────────────────────
    private lateinit var googleSignInClient: GoogleSignInClient
    private var loginDialog: BottomSheetDialog? = null
    private var createItineraryDialog: BottomSheetDialog? = null
    private var createItineraryHandled = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // 立刻儲存 Google 帳號資訊 → isLoggedIn() 立即為 true
                val tempUser = User(
                    id = 0,
                    name = account.displayName ?: account.email ?: "Google 用戶",
                    email = account.email,
                    avatarUrl = account.photoUrl?.toString()
                )
                viewModel.saveGoogleAccount(tempUser)
                loginDialog?.dismiss()

                val idToken = account.idToken
                if (idToken != null) {
                    // 再呼叫後端取 JWT（會更新為後端的用戶資訊）
                    viewModel.loginWithGoogle(idToken, account.photoUrl?.toString())
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google 登入失敗：${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── 定位 ──────────────────────────────────────────────────
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "需要位置權限才能顯示目前位置", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── UI ────────────────────────────────────────────────────
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var ivUserAvatar: ImageView
    private lateinit var btnRefreshUser: ImageButton
    private lateinit var chipGroup: LinearLayout

    // 地圖資料
    private var allSpots: List<Spot> = emptyList()
    private val markerSpotMap = HashMap<Marker, Spot>()
    private var selectedCategory = "all"

    // 我的列表
    private val savedSpots = mutableListOf<Spot>()
    private var hasLoadedFromDb = false
    private var savedListAdapter: SavedSpotAdapter? = null

    // Feature 2：匯入景點後的目標行程
    private var pendingNavigationItinerary: Itinerary? = null

    private val categoryMap = linkedMapOf(
        "all"          to "全部",
        "attraction"   to "景點",
        "restaurant"   to "餐廳",
        "night_market" to "夜市",
        "shopping"     to "購物",
        "cafe"         to "咖啡廳"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGoogleSignIn()
        initViews()
        setupCategoryChips()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initViewModel()

        viewModel.fetchAllSpots()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized && viewModel.isLoggedIn) {
            viewModel.currentUser?.id?.takeIf { it > 0 }?.let { viewModel.fetchUserItineraries(it) }
        }
    }

    // ─── Google Sign-In 初始化 ─────────────────────────────────

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    /** 顯示登入提示 BottomSheet */
    private fun showLoginBottomSheet(descText: String = "登入後即可使用此功能") {
        if (loginDialog?.isShowing == true) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_login, null)

        view.findViewById<TextView>(R.id.tvLoginDesc).text = descText

        view.findViewById<MaterialButton>(R.id.btnGoogleSignIn).setOnClickListener {
            dialog.dismiss()
            // 先 signOut 確保每次都顯示帳號選擇畫面
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        loginDialog = dialog
        dialog.setOnDismissListener { loginDialog = null }
        dialog.setContentView(view)
        dialog.show()
    }

    /** 需要登入才能執行的操作 */
    private fun requireLogin(desc: String = "登入後即可使用此功能", action: () -> Unit) {
        if (viewModel.isLoggedIn) {
            action()
        } else {
            showLoginBottomSheet(desc)
        }
    }

    // ─── Views 初始化 ──────────────────────────────────────────

    private fun initViews() {
        progressBar    = findViewById(R.id.progressBar)
        tvWelcome      = findViewById(R.id.tvWelcome)
        ivUserAvatar   = findViewById(R.id.ivUserAvatar)
        btnRefreshUser = findViewById(R.id.btnRefreshUser)
        chipGroup      = findViewById(R.id.chipGroup)

        ivUserAvatar.setOnClickListener {
            if (viewModel.isLoggedIn) {
                showProfileBottomSheet()
            } else {
                showLoginBottomSheet("登入後即可查看個人資料")
            }
        }

        btnRefreshUser.setOnClickListener {
            val uid = viewModel.currentUser?.id
            if (uid != null) {
                viewModel.fetchUser(uid)
            } else {
                showLoginBottomSheet()
            }
        }

        findViewById<MaterialButton>(R.id.btnListView).setOnClickListener {
            showSavedListBottomSheet()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener {
                requireLogin("登入後即可建立旅遊行程") {
                    showCreateItinerarySheet()
                }
            }

        findViewById<MaterialButton>(R.id.btnZoomIn).setOnClickListener {
            if (::googleMap.isInitialized) googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        findViewById<MaterialButton>(R.id.btnZoomOut).setOnClickListener {
            if (::googleMap.isInitialized) googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        findViewById<MaterialButton>(R.id.btnMyLocation).setOnClickListener {
            moveToMyLocation()
        }

        findViewById<ImageButton>(R.id.btnNavExplore).setOnClickListener {
            requireLogin("登入後即可管理行程") {
                startActivity(Intent(this, ItineraryListActivity::class.java))
            }
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
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.0330, 121.5654), 14f))

        googleMap.setOnMarkerClickListener { marker ->
            markerSpotMap[marker]?.let { showSpotBottomSheet(it) }
            true
        }

        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

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
                MarkerOptions().position(LatLng(lat, lng)).title(spot.name)
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

        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddToList)

        fun syncButton() {
            if (savedSpots.any { it.id == spot.id }) {
                btnAdd.text = "✓ 已加入"
                btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
            } else {
                btnAdd.text = "＋ 加入列表"
                btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44062"))
            }
        }
        syncButton()

        btnAdd.setOnClickListener {
            val alreadySaved = savedSpots.any { it.id == spot.id }
            if (alreadySaved) {
                savedSpots.removeAll { it.id == spot.id }
                viewModel.removeSavedSpot(spot.id)
                syncButton()
                savedListAdapter?.submitList(buildMixedList(null))
                return@setOnClickListener
            }
            val itineraries = viewModel.cachedUserItineraries
            if (viewModel.isLoggedIn && itineraries.isNotEmpty()) {
                val options = (itineraries.map { it.title } + listOf("＋ 新增行程", "只加入列表")).toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("加入行程")
                    .setItems(options) { _, which ->
                        savedSpots.add(spot); viewModel.addSavedSpot(spot)
                        syncButton(); savedListAdapter?.submitList(buildMixedList(null))
                        when {
                            which < itineraries.size ->
                                viewModel.addSpotToExistingItinerary(itineraries[which].id, spot)
                            which == itineraries.size ->
                                requireLogin("登入後即可建立旅遊行程") { showCreateItinerarySheet() }
                            // else: 只加入列表，已在上面完成
                        }
                    }.show()
            } else {
                savedSpots.add(spot); viewModel.addSavedSpot(spot)
                syncButton(); savedListAdapter?.submitList(buildMixedList(null))
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun buildMixedList(distances: List<DistanceInfo?>?): List<SavedListItem> {
        val list = mutableListOf<SavedListItem>()
        savedSpots.forEachIndexed { index, spot ->
            list.add(SavedListItem.SpotItem(spot))
            if (index < savedSpots.size - 1) {
                val info = distances?.getOrNull(index)
                if (info != null) {
                    list.add(SavedListItem.DistanceSeparator(info.distance, info.duration))
                }
            }
        }
        return list
    }

    private fun showSavedListBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_saved_list, null)

        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyList)
        val rv = view.findViewById<RecyclerView>(R.id.rvSavedSpots)

        fun refreshState() {
            if (savedSpots.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rv.visibility = View.VISIBLE
            }
        }

        savedListAdapter = SavedSpotAdapter(
            { cat -> categoryMap[cat] ?: cat ?: "景點" }
        ) { spot ->
            val pos = savedSpots.indexOfFirst { it.id == spot.id }
            if (pos != -1) {
                savedSpots.removeAt(pos)
                viewModel.removeSavedSpot(spot.id)
                savedListAdapter?.submitList(buildMixedList(null))
                viewModel.fetchDistancesBetweenSpots(savedSpots)
                refreshState()
            }
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = savedListAdapter
        savedListAdapter?.submitList(buildMixedList(null))
        refreshState()

        val distObserver = Observer<List<DistanceInfo?>?> { distances ->
            if (distances != null) savedListAdapter?.submitList(buildMixedList(distances))
        }
        viewModel.distanceResults.observe(this, distObserver)

        dialog.setOnDismissListener {
            viewModel.distanceResults.removeObserver(distObserver)
            savedListAdapter = null
        }

        viewModel.fetchDistancesBetweenSpots(savedSpots)
        dialog.setContentView(view)
        dialog.show()
    }

    // ─── ViewModel Observers ──────────────────────────────────

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // 從 Room 載入已儲存景點（僅一次）
        viewModel.savedSpotsLiveData.observe(this) { entities ->
            if (!hasLoadedFromDb) {
                hasLoadedFromDb = true
                savedSpots.addAll(entities.map { it.toSpot() })
                savedListAdapter?.notifyDataSetChanged()
            }
        }

        // Header 由 currentUserLiveData 驅動
        viewModel.currentUserLiveData.observe(this) { user ->
            if (user != null) {
                tvWelcome.text = user.name
                if (!user.avatarUrl.isNullOrEmpty()) {
                    ivUserAvatar.load(user.avatarUrl) {
                        transformations(CircleCropTransformation())
                        placeholder(android.R.drawable.sym_def_app_icon)
                        error(android.R.drawable.sym_def_app_icon)
                    }
                } else {
                    ivUserAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            } else {
                tvWelcome.text = "遊客"
                ivUserAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }

        // Google 登入結果
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "歡迎，${result.data?.name}！", Toast.LENGTH_SHORT).show()
                    result.data?.id?.takeIf { it > 0 }?.let { viewModel.fetchUserItineraries(it) }
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "登入失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 刷新用戶（loading 狀態）
        viewModel.userResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> showLoading(false)
                is NetworkResult.Error   -> {
                    showLoading(false)
                    Toast.makeText(this, "用戶讀取失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 景點
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

        // 建立行程結果
        viewModel.createItineraryResponse.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    createItineraryHandled = false
                }
                is NetworkResult.Success -> {
                    if (createItineraryHandled) return@observe
                    createItineraryHandled = true
                    createItineraryDialog?.dismiss()
                    val itinerary = result.data ?: return@observe
                    if (savedSpots.isNotEmpty()) {
                        pendingNavigationItinerary = itinerary
                        AlertDialog.Builder(this)
                            .setTitle("匯入景點")
                            .setMessage("是否將列表中 ${savedSpots.size} 個景點加入「${itinerary.title}」的第一天？")
                            .setPositiveButton("加入") { _, _ ->
                                viewModel.importSpotsToNewItinerary(itinerary.id, savedSpots.toList())
                            }
                            .setNegativeButton("略過") { _, _ ->
                                navigateToItineraryDetail(itinerary)
                                pendingNavigationItinerary = null
                            }
                            .show()
                    } else {
                        navigateToItineraryDetail(itinerary)
                    }
                }
                is NetworkResult.Error -> {
                    // 按鈕重新啟用由 sheet 內的 observer 處理
                }
            }
        }

        // 匯入景點結果（Feature 2）
        viewModel.importSpotsResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "景點已匯入行程！", Toast.LENGTH_SHORT).show()
                    pendingNavigationItinerary?.let { navigateToItineraryDetail(it) }
                    pendingNavigationItinerary = null
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "匯入失敗：${result.message}", Toast.LENGTH_SHORT).show()
                    pendingNavigationItinerary?.let { navigateToItineraryDetail(it) }
                    pendingNavigationItinerary = null
                }
            }
        }

        // 加入景點到現有行程結果（Feature 3）
        viewModel.addSpotToItineraryResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> Unit
                is NetworkResult.Success -> Toast.makeText(this, "景點已加入行程！", Toast.LENGTH_SHORT).show()
                is NetworkResult.Error -> Toast.makeText(this, "加入行程失敗：${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── 個人資料 BottomSheet ──────────────────────────────────

    private fun showProfileBottomSheet() {
        val user = viewModel.currentUser ?: run {
            Toast.makeText(this, "尚未載入使用者資料", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_profile, null)

        val ivAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)
        val etName   = view.findViewById<TextInputEditText>(R.id.etProfileName)
        val etEmail  = view.findViewById<TextInputEditText>(R.id.etProfileEmail)
        val tvDate   = view.findViewById<TextView>(R.id.tvProfileJoinDate)
        val btnSave  = view.findViewById<MaterialButton>(R.id.btnSaveProfile)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        etName.setText(user.name)
        etEmail.setText(user.email ?: "")
        tvDate.text = "加入日期：${user.createdAt?.take(10) ?: "—"}"

        if (!user.avatarUrl.isNullOrEmpty()) {
            ivAvatar.load(user.avatarUrl) {
                transformations(CircleCropTransformation())
                placeholder(android.R.drawable.sym_def_app_icon)
                error(android.R.drawable.sym_def_app_icon)
            }
        }

        val saveObserver = Observer<NetworkResult<com.funTrip.fun2go.data.model.User>> { result ->
            when (result) {
                is NetworkResult.Loading -> btnSave.isEnabled = false
                is NetworkResult.Success -> {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "已儲存", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                is NetworkResult.Error -> {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "儲存失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.updateUserResponse.observe(this, saveObserver)

        btnSave.setOnClickListener {
            val name  = etName.text?.toString()?.trim() ?: ""
            val email = etEmail.text?.toString()?.trim()
            if (name.isEmpty()) { etName.error = "姓名不能為空"; return@setOnClickListener }
            viewModel.updateUser(user.id, name, email)
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
            googleSignInClient.signOut()
            dialog.dismiss()
            Toast.makeText(this, "已登出", Toast.LENGTH_SHORT).show()
        }

        dialog.setOnDismissListener {
            viewModel.updateUserResponse.removeObserver(saveObserver)
        }
        dialog.setContentView(view)
        dialog.show()
    }

    // ─── 定位功能 ──────────────────────────────────────────────

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableMyLocation() {
        if (::googleMap.isInitialized) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), 16f
                        )
                    )
                } else {
                    Toast.makeText(this, "無法取得目前位置，請確認 GPS 已開啟", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "定位失敗：${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ─── 新增行程 BottomSheet ──────────────────────────────────

    private fun showCreateItinerarySheet() {
        if (createItineraryDialog?.isShowing == true) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_itinerary, null)

        val tilTitle  = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilItineraryTitle)
        val etTitle   = view.findViewById<TextInputEditText>(R.id.etItineraryTitle)
        val etStart   = view.findViewById<TextInputEditText>(R.id.etStartDate)
        val etEnd     = view.findViewById<TextInputEditText>(R.id.etEndDate)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreateItinerary)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelCreate)
        val pbCreating = view.findViewById<ProgressBar>(R.id.pbCreating)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        etStart.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇開始日期")
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                etStart.setText(dateFormat.format(Date(ms)))
            }
            picker.show(supportFragmentManager, "start_date_picker")
        }

        etEnd.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇結束日期")
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                etEnd.setText(dateFormat.format(Date(ms)))
            }
            picker.show(supportFragmentManager, "end_date_picker")
        }

        val createObserver = Observer<NetworkResult<com.funTrip.fun2go.data.model.Itinerary>> { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    btnCreate.isEnabled = false
                    pbCreating.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    btnCreate.isEnabled = true
                    pbCreating.visibility = View.GONE
                    // dialog dismissed + activity launched by initViewModel observer
                }
                is NetworkResult.Error -> {
                    btnCreate.isEnabled = true
                    pbCreating.visibility = View.GONE
                    Toast.makeText(this, "建立失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.createItineraryResponse.observe(this, createObserver)

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                tilTitle.error = "行程名稱不能為空"
                return@setOnClickListener
            }
            tilTitle.error = null
            val start = etStart.text?.toString()?.trim() ?: ""
            val end   = etEnd.text?.toString()?.trim() ?: ""
            viewModel.createItinerary(title, start, end)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            viewModel.createItineraryResponse.removeObserver(createObserver)
            createItineraryDialog = null
        }

        createItineraryDialog = dialog
        dialog.setContentView(view)
        dialog.show()
    }

    private fun navigateToItineraryDetail(itinerary: Itinerary) {
        val intent = Intent(this, ItineraryDetailActivity::class.java)
        intent.putExtra("itinerary_id", itinerary.id)
        intent.putExtra("itinerary_title", itinerary.title)
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
