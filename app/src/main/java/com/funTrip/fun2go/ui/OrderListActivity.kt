package com.funTrip.fun2go.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Order
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.OrderAdapter
import com.funTrip.fun2go.ui.viewmodel.OrderViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.NumberFormat
import java.util.Locale

class OrderListActivity : AppCompatActivity() {

    private lateinit var viewModel: OrderViewModel
    private lateinit var adapter: OrderAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView

    private var currentStatusFilter: String? = null
    private var detailSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pbLoading = findViewById(R.id.pbLoading)
        tvEmpty   = findViewById(R.id.tvEmpty)
        rvOrders  = findViewById(R.id.rvOrders)

        viewModel = ViewModelProvider(this)[OrderViewModel::class.java]

        adapter = OrderAdapter { order -> showOrderDetailSheet(order) }
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter

        setupChips()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchOrders(currentStatusFilter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun setupChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupStatus)
        data class StatusFilter(val label: String, val value: String?)
        val filters = listOf(
            StatusFilter("全部",   null),
            StatusFilter("待付款", "pending"),
            StatusFilter("已確認", "confirmed"),
            StatusFilter("已完成", "completed"),
            StatusFilter("已取消", "cancelled")
        )
        filters.forEach { f ->
            val chip = Chip(this).apply {
                text = f.label
                isCheckable = true
                isChecked = (f.value == null)
            }
            chip.setOnClickListener {
                currentStatusFilter = f.value
                viewModel.fetchOrders(currentStatusFilter)
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupObservers() {
        viewModel.orders.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                    rvOrders.visibility  = View.GONE
                    tvEmpty.visibility   = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        tvEmpty.text      = "暫無訂單記錄"
                        tvEmpty.visibility = View.VISIBLE
                        rvOrders.visibility = View.GONE
                    } else {
                        tvEmpty.visibility  = View.GONE
                        rvOrders.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    tvEmpty.text         = "載入失敗：${result.message}"
                    tvEmpty.visibility   = View.VISIBLE
                    rvOrders.visibility  = View.GONE
                }
            }
        }

        viewModel.cancelResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> { /* handled in sheet */ }
                is NetworkResult.Success -> {
                    Toast.makeText(this, "訂單已取消", Toast.LENGTH_SHORT).show()
                    detailSheet?.dismiss()
                    viewModel.fetchOrders(currentStatusFilter)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, "取消失敗：${result.message}", Toast.LENGTH_LONG).show()
                    detailSheet?.let { sheet ->
                        sheet.findViewById<ProgressBar>(R.id.pbDetailAction)?.visibility = View.GONE
                        sheet.findViewById<MaterialButton>(R.id.btnCancelOrder)?.isEnabled = true
                        sheet.findViewById<MaterialButton>(R.id.btnPayNow)?.isEnabled = true
                    }
                }
            }
        }

        viewModel.payResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> { /* handled in sheet */ }
                is NetworkResult.Success -> {
                    Toast.makeText(this, "付款成功，訂單已確認！", Toast.LENGTH_SHORT).show()
                    detailSheet?.dismiss()
                    viewModel.fetchOrders(currentStatusFilter)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, "付款失敗：${result.message}", Toast.LENGTH_LONG).show()
                    detailSheet?.let { sheet ->
                        sheet.findViewById<ProgressBar>(R.id.pbDetailAction)?.visibility = View.GONE
                        sheet.findViewById<MaterialButton>(R.id.btnPayNow)?.isEnabled = true
                        sheet.findViewById<MaterialButton>(R.id.btnCancelOrder)?.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showOrderDetailSheet(order: Order) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_order_detail, null)
        sheet.setContentView(view)
        detailSheet = sheet

        val booking = order.charterBooking

        // 標題列
        val tvDetailStatus = view.findViewById<TextView>(R.id.tvDetailStatus)
        val (statusText, statusColor) = when (order.status) {
            "pending"   -> Pair("待付款", Color.parseColor("#FF9800"))
            "confirmed" -> Pair("已確認", Color.parseColor("#4CAF50"))
            "completed" -> Pair("已完成", Color.parseColor("#9E9E9E"))
            "cancelled" -> Pair("已取消", Color.parseColor("#F44336"))
            else        -> Pair(order.status, Color.parseColor("#9E9E9E"))
        }
        tvDetailStatus.text = statusText
        tvDetailStatus.setTextColor(statusColor)

        val dateLabel = order.createdAt.take(10)
        view.findViewById<TextView>(R.id.tvDetailOrderId).text = "訂單 #${order.id} · $dateLabel"

        // 車輛資訊
        view.findViewById<TextView>(R.id.tvDetailVehicleName).text =
            booking?.vehicleName ?: "包車訂單"

        val typeName = when (booking?.vehicleType) {
            "sedan_4" -> "轎車"
            "van_9"   -> "九人座"
            "bus_20"  -> "巴士"
            else      -> booking?.vehicleType ?: ""
        }
        val capacity = booking?.vehicleCapacity?.let { " · 最多${it}人" } ?: ""
        view.findViewById<TextView>(R.id.tvDetailVehicleInfo).text = "$typeName$capacity"

        // 預訂明細
        view.findViewById<TextView>(R.id.tvDetailPickup).text =
            "上車：${booking?.pickupLocation ?: ""}"

        val tvDropoff = view.findViewById<TextView>(R.id.tvDetailDropoff)
        if (!booking?.dropoffLocation.isNullOrBlank()) {
            tvDropoff.visibility = View.VISIBLE
            tvDropoff.text = "下車：${booking?.dropoffLocation}"
        } else {
            tvDropoff.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tvDetailPickupTime).text =
            "時間：${booking?.pickupTime?.replace("T", " ")?.take(16) ?: ""}"

        view.findViewById<TextView>(R.id.tvDetailDaysAndPax).text =
            "${booking?.days ?: 0}天 · ${booking?.passengerCount ?: 0}人"

        // 聯絡資訊
        view.findViewById<TextView>(R.id.tvDetailContact).text =
            "${booking?.contactName ?: ""}  ${booking?.contactPhone ?: ""}"

        // 總金額
        val amount = order.totalAmount.toDoubleOrNull()?.toLong()
        val amountText = if (amount != null) {
            "NT$ ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
        } else {
            "NT$ ${order.totalAmount}"
        }
        view.findViewById<TextView>(R.id.tvDetailAmount).text = "總金額：$amountText"

        // 按鈕
        val pbDetailAction = view.findViewById<ProgressBar>(R.id.pbDetailAction)
        val btnPayNow      = view.findViewById<MaterialButton>(R.id.btnPayNow)
        val btnCancelOrder = view.findViewById<MaterialButton>(R.id.btnCancelOrder)

        if (order.status == "pending") {
            btnPayNow.visibility      = View.VISIBLE
            btnCancelOrder.visibility = View.VISIBLE
        } else {
            btnPayNow.visibility      = View.GONE
            btnCancelOrder.visibility = View.GONE
        }

        btnPayNow.setOnClickListener {
            pbDetailAction.visibility = View.VISIBLE
            btnPayNow.isEnabled       = false
            btnCancelOrder.isEnabled  = false
            viewModel.payOrder(order.id)
        }

        btnCancelOrder.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("取消訂單")
                .setMessage("確定要取消此訂單嗎？")
                .setPositiveButton("取消訂單") { _, _ ->
                    pbDetailAction.visibility = View.VISIBLE
                    btnPayNow.isEnabled       = false
                    btnCancelOrder.isEnabled  = false
                    viewModel.cancelOrder(order.id)
                }
                .setNegativeButton("返回", null)
                .show()
        }

        sheet.setOnDismissListener { detailSheet = null }
        sheet.show()
    }
}
