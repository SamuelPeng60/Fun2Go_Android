package com.funTrip.fun2go.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
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
            StatusFilter(getString(R.string.status_all),       null),
            StatusFilter(getString(R.string.status_pending),   "pending"),
            StatusFilter(getString(R.string.status_confirmed), "confirmed"),
            StatusFilter(getString(R.string.status_completed), "completed"),
            StatusFilter(getString(R.string.status_cancelled), "cancelled")
        )
        filters.forEach { f ->
            val chip = Chip(this).apply {
                text = f.label
                isCheckable = true
                tag = f.value
            }
            chipGroup.addView(chip)
        }
        // 預設勾選「全部」
        (chipGroup.getChildAt(0) as? Chip)?.isChecked = true
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            currentStatusFilter = group.findViewById<Chip>(checkedId).tag as? String
            viewModel.fetchOrders(currentStatusFilter)
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
                        tvEmpty.text      = getString(R.string.empty_orders)
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
                    tvEmpty.text         = getString(R.string.msg_load_orders_failed, result.message ?: "")
                    tvEmpty.visibility   = View.VISIBLE
                    rvOrders.visibility  = View.GONE
                }
            }
        }

        viewModel.cancelResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> { /* handled in sheet */ }
                is NetworkResult.Success -> {
                    Toast.makeText(this, getString(R.string.msg_order_cancelled), Toast.LENGTH_SHORT).show()
                    detailSheet?.dismiss()
                    viewModel.fetchOrders(currentStatusFilter)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, getString(R.string.msg_cancel_failed, result.message ?: ""), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this, getString(R.string.msg_payment_success), Toast.LENGTH_SHORT).show()
                    detailSheet?.dismiss()
                    viewModel.fetchOrders(currentStatusFilter)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, getString(R.string.msg_payment_failed, result.message ?: ""), Toast.LENGTH_LONG).show()
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
            "pending"   -> Pair(getString(R.string.status_pending),   Color.parseColor("#FF9800"))
            "confirmed" -> Pair(getString(R.string.status_confirmed), Color.parseColor("#4CAF50"))
            "completed" -> Pair(getString(R.string.status_completed), Color.parseColor("#9E9E9E"))
            "cancelled" -> Pair(getString(R.string.status_cancelled), Color.parseColor("#F44336"))
            else        -> Pair(order.status, Color.parseColor("#9E9E9E"))
        }
        tvDetailStatus.text = statusText
        tvDetailStatus.setTextColor(statusColor)

        val dateLabel = order.createdAt.take(10)
        view.findViewById<TextView>(R.id.tvDetailOrderId).text = "${getString(R.string.format_order_id, order.id)} · $dateLabel"

        // 車輛資訊
        view.findViewById<TextView>(R.id.tvDetailVehicleName).text =
            booking?.vehicleName ?: getString(R.string.label_charter_order)

        val typeName = when (booking?.vehicleType) {
            "sedan_4" -> getString(R.string.vehicle_type_sedan)
            "van_9"   -> getString(R.string.vehicle_type_van)
            "bus_20"  -> getString(R.string.vehicle_type_bus)
            else      -> booking?.vehicleType ?: ""
        }
        val capacity = booking?.vehicleCapacity?.let { " · ${getString(R.string.format_capacity, it)}" } ?: ""
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
        view.findViewById<TextView>(R.id.tvDetailAmount).text = getString(R.string.format_total_amount, amountText)

        // 連結行程
        val layoutLinkedItinerary = view.findViewById<LinearLayout>(R.id.layoutLinkedItinerary)
        val tvLinkedItinerary     = view.findViewById<TextView>(R.id.tvLinkedItinerary)
        if (order.itineraryId != null) {
            layoutLinkedItinerary.visibility = View.VISIBLE
            tvLinkedItinerary.text = getString(R.string.format_itin_link, order.itineraryId)
            tvLinkedItinerary.setOnClickListener {
                sheet.dismiss()
                startActivity(
                    Intent(this, ItineraryDetailActivity::class.java).apply {
                        putExtra("itinerary_id", order.itineraryId)
                        putExtra("itinerary_title", getString(R.string.format_itin_link, order.itineraryId))
                    }
                )
            }
        } else {
            layoutLinkedItinerary.visibility = View.GONE
        }

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
                .setTitle(getString(R.string.title_cancel_order_dialog))
                .setMessage(getString(R.string.msg_delete_order_confirm))
                .setPositiveButton(getString(R.string.label_cancel_order)) { _, _ ->
                    pbDetailAction.visibility = View.VISIBLE
                    btnPayNow.isEnabled       = false
                    btnCancelOrder.isEnabled  = false
                    viewModel.cancelOrder(order.id)
                }
                .setNegativeButton(getString(R.string.label_go_back), null)
                .show()
        }

        sheet.setOnDismissListener { detailSheet = null }
        sheet.show()
    }
}
