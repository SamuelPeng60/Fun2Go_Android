package com.funTrip.fun2go.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.SpotAdapter
import com.funTrip.fun2go.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var spotAdapter: SpotAdapter

    // UI Components
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnRefreshUser: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rvSpots: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initRecyclerView()
        initViewModel()

        // 預設行為：抓取 ID = 1 的用戶資料
        viewModel.fetchUser(1)
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnRefreshUser = findViewById(R.id.btnRefreshUser)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvSpots = findViewById(R.id.rvSpots)

        // 搜尋按鈕點擊事件
        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString()
            if (keyword.isNotEmpty()) {
                viewModel.searchSpots(keyword)
                // 收起鍵盤 (可選)
            } else {
                Toast.makeText(this, "請輸入關鍵字", Toast.LENGTH_SHORT).show()
            }
        }

        // 重新整理用戶按鈕
        btnRefreshUser.setOnClickListener {
            viewModel.fetchUser(1) // 模擬重抓 User 1
        }
    }

    private fun initRecyclerView() {
        spotAdapter = SpotAdapter()
        rvSpots.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = spotAdapter
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // 1. 觀察用戶資料
        viewModel.userResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    val user = response.data
                    tvWelcome.text = "Hi, ${user?.name}"
                    tvUserEmail.text = user?.email ?: "No Email"
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "用戶讀取失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. 觀察搜尋結果
        viewModel.spotsResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> {
                    showLoading(true)
                    tvEmptyState.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    showLoading(false)
                    val spots = response.data
                    if (!spots.isNullOrEmpty()) {
                        spotAdapter.submitList(spots)
                        tvEmptyState.visibility = View.GONE
                        rvSpots.visibility = View.VISIBLE
                    } else {
                        spotAdapter.submitList(emptyList())
                        tvEmptyState.visibility = View.VISIBLE
                        rvSpots.visibility = View.GONE
                    }
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "搜尋失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. 建立用戶
        viewModel.createUserResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "建立用戶成功: ${response.data?.name}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "建立用戶失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. 更新用戶
        viewModel.updateUserResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "更新用戶成功: ${response.data?.name}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "更新用戶失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. 用戶行程列表
        viewModel.userItinerariesResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "用戶行程數: ${response.data?.size ?: 0}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "取得用戶行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. 用戶收藏列表
        viewModel.userFavoritesResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "用戶收藏數: ${response.data?.size ?: 0}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "取得收藏失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 7. 公開行程列表
        viewModel.itinerariesResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "行程列表共 ${response.data?.size ?: 0} 筆", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "取得行程列表失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 8. 建立行程
        viewModel.createItineraryResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "建立行程成功: ${response.data?.title}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "建立行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 9. 更新行程
        viewModel.updateItineraryResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "更新行程成功: ${response.data?.title}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "更新行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 10. 刪除行程
        viewModel.deleteItineraryResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "刪除行程成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "刪除行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 11. 複製行程
        viewModel.copyItineraryResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "複製行程成功: ${response.data?.title}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "複製行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 12. 發布行程
        viewModel.publishItineraryResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "發布行程成功: ${response.data?.title}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "發布行程失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 13. 新增行程天
        viewModel.addDayResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "新增天數成功: 第 ${response.data?.day_number} 天", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "新增天數失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 14. 更新行程天
        viewModel.updateDayResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "更新天數成功: 第 ${response.data?.day_number} 天", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "更新天數失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 15. 刪除行程天
        viewModel.deleteDayResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "刪除天數成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "刪除天數失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 16. 新增景點到天
        viewModel.addSpotToDayResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "新增景點成功: spot_id=${response.data?.spot_id}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "新增景點失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 17. 更新景點在天內
        viewModel.updateSpotInDayResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "更新景點成功: spot_id=${response.data?.spot_id}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "更新景點失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 18. 移除景點
        viewModel.removeSpotResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "移除景點成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "移除景點失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 19. 重新排序景點
        viewModel.reorderSpotsResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "排序更新成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "排序更新失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 20. 景點詳情
        viewModel.spotDetailResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "景點詳情: ${response.data?.name}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "取得景點詳情失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 21. 建立景點
        viewModel.createSpotResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "建立景點成功: ${response.data?.name}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "建立景點失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 22. 新增收藏
        viewModel.favoriteResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "加入收藏成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "加入收藏失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 23. 移除收藏
        viewModel.unfavoriteResponse.observe(this) { response ->
            when (response) {
                is NetworkResult.Loading -> showLoading(true)
                is NetworkResult.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "移除收藏成功", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "移除收藏失敗: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
