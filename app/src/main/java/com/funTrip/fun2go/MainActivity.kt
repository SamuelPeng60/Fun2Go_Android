package com.funTrip.fun2go.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import java.util.Calendar
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.model.ItineraryDay
import com.funTrip.fun2go.data.model.Spot
import com.funTrip.fun2go.data.model.SpotRequest
import com.funTrip.fun2go.data.model.UploadResponse
import com.funTrip.fun2go.data.model.User
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.ItineraryDetailActivity
import com.funTrip.fun2go.ui.ItineraryListActivity
import com.funTrip.fun2go.ui.PublicItineraryListActivity
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

    // ─── 圖片選擇器 ────────────────────────────────────────────
    private var onImagePickedCallback: ((Uri) -> Unit)? = null
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onImagePickedCallback?.invoke(it) } }

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
    private lateinit var btnNavProfile: ImageButton
    private lateinit var btnRefreshUser: ImageButton
    private lateinit var chipGroup: LinearLayout

    // 地圖資料
    private var allSpots: List<Spot> = emptyList()
    private val markerSpotMap = HashMap<Marker, Spot>()
    private var selectedCategory = "all"

    // 我的列表
    private val savedSpots = mutableListOf<Spot>()
    private var hasLoadedFromDb = false

    private var pendingNavigationItinerary: Itinerary? = null
    private var pendingStartDate: String? = null

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
        when {
            !viewModel.isLoggedIn -> showLoginBottomSheet(desc)
            !viewModel.hasValidToken -> Toast.makeText(this, "正在登入中，請稍候...", Toast.LENGTH_SHORT).show()
            else -> action()
        }
    }

    // ─── Views 初始化 ──────────────────────────────────────────

    private fun initViews() {
        progressBar    = findViewById(R.id.progressBar)
        tvWelcome      = findViewById(R.id.tvWelcome)
        btnNavProfile  = findViewById(R.id.btnNavProfile)
        btnRefreshUser = findViewById(R.id.btnRefreshUser)
        chipGroup      = findViewById(R.id.chipGroup)

        btnNavProfile.setOnClickListener {
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
            startActivity(Intent(this, PublicItineraryListActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnNavCharter).setOnClickListener {
            startActivity(Intent(this, VehicleListActivity::class.java))
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

        googleMap.setOnMapLongClickListener { latLng ->
            requireLogin("登入後即可新增自訂景點") {
                showCreateSpotSheet(latLng)
            }
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

        // 擁有者操作列：僅建立者可見
        val currentUserId = viewModel.currentUser?.id
        if (spot.creatorId != null && currentUserId != null && spot.creatorId == currentUserId) {
            view.findViewById<LinearLayout>(R.id.llOwnerActions).visibility = View.VISIBLE
            view.findViewById<ImageButton>(R.id.btnEditSpot).setOnClickListener {
                dialog.dismiss()
                showEditSpotSheet(spot)
            }
            view.findViewById<ImageButton>(R.id.btnDeleteSpot).setOnClickListener {
                confirmDeleteSpot(spot, dialog)
            }
        }

        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddToList)

        fun syncButton() {
            if (savedSpots.any { it.id == spot.id }) {
                btnAdd.text = "✓ 已加入行程"
                btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
            } else {
                btnAdd.text = "＋ 加入行程"
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
                // 同步刪除後端 favorites
                val uid = viewModel.currentUser?.id ?: 0
                if (uid > 0) viewModel.removeFavorite(spot.id, uid)
                return@setOnClickListener
            }
            // 步驟一：必須登入
            if (!viewModel.isLoggedIn) {
                showLoginBottomSheet("請先登入 Google，才能將景點加入行程")
                return@setOnClickListener
            }
            // 步驟二：必須有行程
            val itineraries = viewModel.cachedUserItineraries
            if (itineraries.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("尚無行程")
                    .setMessage("請先建立一個行程，才能加入景點")
                    .setPositiveButton("前往建立") { _, _ -> showCreateItinerarySheet() }
                    .setNegativeButton("取消", null)
                    .show()
                return@setOnClickListener
            }
            // 步驟三：選擇要加入的行程
            AlertDialog.Builder(this)
                .setTitle("選擇行程")
                .setItems(itineraries.map { it.title }.toTypedArray()) { _, which ->
                    val selected = itineraries[which]
                    savedSpots.add(spot)
                    viewModel.addSavedSpot(spot)
                    syncButton()
                    val uid = viewModel.currentUser?.id ?: 0
                    if (uid > 0) viewModel.addFavorite(uid, spot.id)

                    viewModel.fetchItineraryDetail(selected.id)
                    var seenLoading = false
                    var dayPickerObs: Observer<NetworkResult<Itinerary>>? = null
                    dayPickerObs = Observer { result ->
                        when (result) {
                            is NetworkResult.Loading -> seenLoading = true
                            is NetworkResult.Success -> if (seenLoading) {
                                seenLoading = false
                                viewModel.itineraryDetail.removeObserver(dayPickerObs!!)
                                val days = result.data?.days.orEmpty()
                                when {
                                    days.isEmpty() -> viewModel.addSpotToExistingItinerary(selected.id, spot)
                                    days.size == 1 -> viewModel.addSpotToDayById(days[0].id, spot.id, (days[0].spots?.size ?: 0) + 1)
                                    else           -> showDayPickerDialog(days, spot)
                                }
                            }
                            is NetworkResult.Error -> if (seenLoading) {
                                seenLoading = false
                                viewModel.itineraryDetail.removeObserver(dayPickerObs!!)
                                viewModel.addSpotToExistingItinerary(selected.id, spot)
                            }
                            else -> {}
                        }
                    }
                    viewModel.itineraryDetail.observe(this, dayPickerObs!!)
                    Toast.makeText(this, "「${spot.name}」已加入「${selected.title}」", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

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
            }
        }

        // Header 由 currentUserLiveData 驅動
        viewModel.currentUserLiveData.observe(this) { user ->
            if (user != null) {
                tvWelcome.text = user.name
                if (!user.avatarUrl.isNullOrEmpty()) {
                    btnNavProfile.load(user.avatarUrl) {
                        transformations(CircleCropTransformation())
                        error(R.drawable.ic_google_logo)
                    }
                } else {
                    btnNavProfile.setImageResource(R.drawable.ic_google_logo)
                }
            } else {
                tvWelcome.text = "遊客"
                btnNavProfile.setImageResource(R.drawable.ic_google_logo)
            }
        }

        // Google 登入結果
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "歡迎，${result.data?.name}！", Toast.LENGTH_SHORT).show()
                    result.data?.id?.takeIf { it > 0 }?.let { userId ->
                        viewModel.fetchUserItineraries(userId)
                        viewModel.fetchUserFavorites(userId)
                    }
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
                    pendingNavigationItinerary = itinerary
                    viewModel.initItineraryDays(itinerary.id, itinerary.total_days, pendingStartDate)
                }
                is NetworkResult.Error -> {
                    // 按鈕重新啟用由 sheet 內的 observer 處理
                }
            }
        }

        // 建立行程後初始化天數結果
        viewModel.initDaysResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    pendingStartDate = null
                    pendingNavigationItinerary?.let { navigateToItineraryDetail(it) }
                    pendingNavigationItinerary = null
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    pendingStartDate = null
                    Toast.makeText(this, "初始化天數失敗：${result.message}", Toast.LENGTH_SHORT).show()
                    pendingNavigationItinerary?.let { navigateToItineraryDetail(it) }
                    pendingNavigationItinerary = null
                }
            }
        }

        // 登入後從後端同步收藏清單
        viewModel.userFavoritesResponse.observe(this) { result ->
            if (result is NetworkResult.Success) {
                result.data?.forEach { spot ->
                    if (!savedSpots.any { it.id == spot.id }) {
                        viewModel.addSavedSpot(spot)
                        savedSpots.add(spot)
                    }
                }
            }
        }

        // 加入景點到現有行程結果（Feature 3）
        viewModel.addSpotToItineraryResult.observe(this) { result ->
            if (result is NetworkResult.Error) {
                Toast.makeText(this, "加入行程失敗：${result.message}", Toast.LENGTH_LONG).show()
            }
        }

        // 刪除景點結果（全域觀察，用於 confirmDeleteSpot）
        viewModel.deleteSpotResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> showLoading(false)
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "刪除失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
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

        val tilTitle    = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilItineraryTitle)
        val etTitle     = view.findViewById<TextInputEditText>(R.id.etItineraryTitle)
        val actvDest    = view.findViewById<AutoCompleteTextView>(R.id.actvDestination)
        val etTotalDays = view.findViewById<TextInputEditText>(R.id.etTotalDays)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.etStartDate)

        val destinations = resources.getStringArray(R.array.destinations)
        actvDest.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, destinations))
        actvDest.setOnClickListener { actvDest.showDropDown() }

        var selectedStartDate: String? = null
        etStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedStartDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                etStartDate.setText(selectedStartDate)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val btnCreate   = view.findViewById<MaterialButton>(R.id.btnCreateItinerary)
        val btnCancel   = view.findViewById<MaterialButton>(R.id.btnCancelCreate)
        val pbCreating  = view.findViewById<ProgressBar>(R.id.pbCreating)

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
            val dest      = actvDest.text?.toString()?.trim()
            val totalDays = etTotalDays.text?.toString()?.trim()?.toIntOrNull()
            pendingStartDate = selectedStartDate
            viewModel.createItinerary(title, totalDays, dest)
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

    // ─── 建立/編輯景點 ─────────────────────────────────────────

    private fun showCreateSpotSheet(latLng: LatLng) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_edit_spot, null)

        view.findViewById<TextView>(R.id.tvSpotSheetTitle).text = "新增景點"

        val etLat = view.findViewById<TextInputEditText>(R.id.etSpotLat)
        val etLng = view.findViewById<TextInputEditText>(R.id.etSpotLng)
        etLat.setText("%.6f".format(latLng.latitude))
        etLng.setText("%.6f".format(latLng.longitude))

        setupSpotCategoryDropdown(view)

        val btnSave     = view.findViewById<MaterialButton>(R.id.btnSaveSpot)
        val btnCancel   = view.findViewById<MaterialButton>(R.id.btnCancelSpot)
        val btnPickImg  = view.findViewById<MaterialButton>(R.id.btnPickImage)
        val ivPreview   = view.findViewById<ImageView>(R.id.ivSpotImagePreview)
        val cardPreview = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardImagePreview)
        val pb          = view.findViewById<android.widget.ProgressBar>(R.id.pbSavingSpot)

        var pendingUri: Uri? = null

        btnPickImg.setOnClickListener {
            onImagePickedCallback = { uri ->
                pendingUri = uri
                cardPreview.visibility = View.VISIBLE
                ivPreview.load(uri)
            }
            imagePickerLauncher.launch("image/*")
        }

        var hasStarted = false
        val observer = Observer<NetworkResult<Spot>> { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    hasStarted = true
                    btnSave.isEnabled = false
                    pb.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> if (hasStarted) {
                    hasStarted = false
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    dialog.dismiss()
                    result.data?.let { newSpot ->
                        allSpots = allSpots + newSpot
                        filterAndPlaceMarkers()
                    }
                    Toast.makeText(this, "景點已建立", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> if (hasStarted) {
                    hasStarted = false
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    Toast.makeText(this, "建立失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.createSpotResponse.observe(this, observer)

        val uploadObserver = Observer<NetworkResult<UploadResponse>> { result ->
            when (result) {
                is NetworkResult.Loading -> { /* pb already visible */ }
                is NetworkResult.Success -> {
                    val url = result.data?.url
                    pendingUri = null
                    buildSpotRequest(view, url)?.let { viewModel.createSpot(it) }
                }
                is NetworkResult.Error -> {
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    Toast.makeText(this, "圖片上傳失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.uploadImageResult.observe(this, uploadObserver)

        btnSave.setOnClickListener {
            val uri = pendingUri
            if (uri != null) {
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    Toast.makeText(this, "無法讀取圖片", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnSave.isEnabled = false
                pb.visibility = View.VISIBLE
                viewModel.uploadImage("spots", bytes, mime)
            } else {
                val req = buildSpotRequest(view) ?: return@setOnClickListener
                viewModel.createSpot(req)
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            viewModel.createSpotResponse.removeObserver(observer)
            viewModel.uploadImageResult.removeObserver(uploadObserver)
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showEditSpotSheet(spot: Spot) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_edit_spot, null)

        view.findViewById<TextView>(R.id.tvSpotSheetTitle).text = "編輯景點"

        view.findViewById<TextInputEditText>(R.id.etSpotName).setText(spot.name)
        view.findViewById<TextInputEditText>(R.id.etSpotAddress).setText(spot.address ?: "")
        view.findViewById<TextInputEditText>(R.id.etSpotLat).setText(spot.latitude ?: "")
        view.findViewById<TextInputEditText>(R.id.etSpotLng).setText(spot.longitude ?: "")
        view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSpotPublic).isChecked = spot.isPublic

        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actvSpotCategory)
        setupSpotCategoryDropdown(view)
        actvCategory.setText(categoryMap[spot.category] ?: spot.category ?: "", false)

        // 若景點已有圖片，預覽現有圖片
        val ivPreview   = view.findViewById<ImageView>(R.id.ivSpotImagePreview)
        val cardPreview = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardImagePreview)
        val currentImageUrl: String? = spot.image_url
        if (!currentImageUrl.isNullOrEmpty()) {
            cardPreview.visibility = View.VISIBLE
            ivPreview.load(currentImageUrl)
        }

        val btnSave    = view.findViewById<MaterialButton>(R.id.btnSaveSpot)
        val btnCancel  = view.findViewById<MaterialButton>(R.id.btnCancelSpot)
        val btnPickImg = view.findViewById<MaterialButton>(R.id.btnPickImage)
        val pb         = view.findViewById<android.widget.ProgressBar>(R.id.pbSavingSpot)

        var pendingUri: Uri? = null

        btnPickImg.setOnClickListener {
            onImagePickedCallback = { uri ->
                pendingUri = uri
                cardPreview.visibility = View.VISIBLE
                ivPreview.load(uri)
            }
            imagePickerLauncher.launch("image/*")
        }

        var hasStarted = false
        val observer = Observer<NetworkResult<Spot>> { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    hasStarted = true
                    btnSave.isEnabled = false
                    pb.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> if (hasStarted) {
                    hasStarted = false
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    dialog.dismiss()
                    result.data?.let { updatedSpot ->
                        allSpots = allSpots.map { if (it.id == updatedSpot.id) updatedSpot else it }
                        filterAndPlaceMarkers()
                    }
                    Toast.makeText(this, "景點已更新", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> if (hasStarted) {
                    hasStarted = false
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    Toast.makeText(this, "更新失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.updateSpotResult.observe(this, observer)

        val uploadObserver = Observer<NetworkResult<UploadResponse>> { result ->
            when (result) {
                is NetworkResult.Loading -> { /* pb already visible */ }
                is NetworkResult.Success -> {
                    val url = result.data?.url
                    pendingUri = null
                    buildSpotRequest(view, url)?.let { viewModel.updateSpot(spot.id, it) }
                }
                is NetworkResult.Error -> {
                    btnSave.isEnabled = true
                    pb.visibility = View.GONE
                    Toast.makeText(this, "圖片上傳失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.uploadImageResult.observe(this, uploadObserver)

        btnSave.setOnClickListener {
            val uri = pendingUri
            if (uri != null) {
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    Toast.makeText(this, "無法讀取圖片", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnSave.isEnabled = false
                pb.visibility = View.VISIBLE
                viewModel.uploadImage("spots", bytes, mime)
            } else {
                val req = buildSpotRequest(view, currentImageUrl) ?: return@setOnClickListener
                viewModel.updateSpot(spot.id, req)
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            viewModel.updateSpotResult.removeObserver(observer)
            viewModel.uploadImageResult.removeObserver(uploadObserver)
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun confirmDeleteSpot(spot: Spot, parentDialog: BottomSheetDialog) {
        AlertDialog.Builder(this)
            .setTitle("刪除景點")
            .setMessage("確定要刪除「${spot.name}」嗎？")
            .setPositiveButton("刪除") { _, _ ->
                viewModel.deleteSpot(spot.id)
                // 觀察結果：成功後關閉父 Sheet 並刷新地圖
                val observer = object : Observer<NetworkResult<Unit>> {
                    override fun onChanged(result: NetworkResult<Unit>) {
                        if (result is NetworkResult.Success) {
                            viewModel.deleteSpotResult.removeObserver(this)
                            parentDialog.dismiss()
                            // 直接從 allSpots 移除，更新地圖
                            allSpots = allSpots.filter { it.id != spot.id }
                            filterAndPlaceMarkers()
                            // 若在本地列表也存在，同步移除
                            if (savedSpots.any { it.id == spot.id }) {
                                savedSpots.removeAll { it.id == spot.id }
                                viewModel.removeSavedSpot(spot.id)
                                val uid = viewModel.currentUser?.id ?: 0
                                if (uid > 0) viewModel.removeFavorite(spot.id, uid)
                            }
                            Toast.makeText(this@MainActivity, "景點已刪除", Toast.LENGTH_SHORT).show()
                        } else if (result is NetworkResult.Error) {
                            viewModel.deleteSpotResult.removeObserver(this)
                        }
                    }
                }
                viewModel.deleteSpotResult.observe(this, observer)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDayPickerDialog(days: List<ItineraryDay>, spot: Spot) {
        val labels = days.map { day ->
            val dateStr = if (day.date.isNullOrBlank()) "" else " · ${day.date}"
            "第${day.day_number}天$dateStr（${day.spots?.size ?: 0} 個景點）"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("選擇加入哪一天")
            .setItems(labels) { _, i ->
                val day = days[i]
                viewModel.addSpotToDayById(day.id, spot.id, (day.spots?.size ?: 0) + 1)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 從 bottom_sheet_create_edit_spot View 中讀取並驗證欄位，回傳 SpotRequest 或 null */
    private fun buildSpotRequest(view: android.view.View, imageUrl: String? = null): SpotRequest? {
        val tilName = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSpotName)
        val name = view.findViewById<TextInputEditText>(R.id.etSpotName).text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            tilName.error = "景點名稱不能為空"
            return null
        }
        tilName.error = null

        val categoryDisplay = view.findViewById<AutoCompleteTextView>(R.id.actvSpotCategory).text?.toString() ?: ""
        val category = categoryMap.entries.firstOrNull { it.value == categoryDisplay }?.key

        val address = view.findViewById<TextInputEditText>(R.id.etSpotAddress).text?.toString()?.trim()?.ifEmpty { null }
        val latStr = view.findViewById<TextInputEditText>(R.id.etSpotLat).text?.toString()?.trim()
        val lngStr = view.findViewById<TextInputEditText>(R.id.etSpotLng).text?.toString()?.trim()
        val lat = latStr?.toDoubleOrNull()
        val lng = lngStr?.toDoubleOrNull()
        val isPublic = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSpotPublic).isChecked

        return SpotRequest(
            name = name,
            category = category,
            latitude = lat,
            longitude = lng,
            address = address,
            image_url = imageUrl,
            is_public = isPublic
        )
    }

    /** 設定景點分類下拉選單 */
    private fun setupSpotCategoryDropdown(view: android.view.View) {
        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actvSpotCategory)
        val displayNames = categoryMap.values.drop(1) // 排除「全部」
        actvCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, displayNames.toList()))
        actvCategory.setOnClickListener { actvCategory.showDropDown() }
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
