package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.model.CharterRequest
import com.funTrip.fun2go.data.model.CreateOrderRequest
import com.funTrip.fun2go.data.model.Vehicle
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.VehicleAdapter
import com.funTrip.fun2go.ui.viewmodel.OrderViewModel
import com.funTrip.fun2go.ui.viewmodel.VehicleViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class VehicleListActivity : AppCompatActivity() {

    private lateinit var vehicleViewModel: VehicleViewModel
    private lateinit var orderViewModel: OrderViewModel
    private lateinit var adapter: VehicleAdapter
    private lateinit var rvVehicles: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView

    private var currentType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_vehicle_list)
        }

        // 訂單 icon
        toolbar.inflateMenu(R.menu.menu_vehicle_list)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_my_orders) {
                val user = TokenManager.getInstance(this).getSavedUser()
                if (user == null) {
                    Toast.makeText(this, getString(R.string.msg_login_for_orders), Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(this, OrderListActivity::class.java))
                }
                true
            } else false
        }

        pbLoading  = findViewById(R.id.pbLoading)
        tvEmpty    = findViewById(R.id.tvEmpty)
        rvVehicles = findViewById(R.id.rvVehicles)

        vehicleViewModel = ViewModelProvider(this)[VehicleViewModel::class.java]
        orderViewModel   = ViewModelProvider(this)[OrderViewModel::class.java]

        adapter = VehicleAdapter { vehicle -> onVehicleClick(vehicle) }
        rvVehicles.layoutManager = LinearLayoutManager(this)
        rvVehicles.adapter = adapter

        setupChips()
        setupOrderObservers()

        vehicleViewModel.vehicles.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility  = View.VISIBLE
                    rvVehicles.visibility = View.GONE
                    tvEmpty.visibility    = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        tvEmpty.visibility    = View.VISIBLE
                        rvVehicles.visibility = View.GONE
                    } else {
                        tvEmpty.visibility    = View.GONE
                        rvVehicles.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility  = View.GONE
                    tvEmpty.text          = getString(R.string.msg_itin_load_failed2, result.message ?: "")
                    tvEmpty.visibility    = View.VISIBLE
                    rvVehicles.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vehicleViewModel.fetchVehicles(type = currentType, available = if (currentType != null) true else null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun onVehicleClick(vehicle: Vehicle) {
        val user = TokenManager.getInstance(this).getSavedUser()
        if (user == null) {
            Toast.makeText(this, getString(R.string.msg_login_for_booking), Toast.LENGTH_SHORT).show()
            return
        }
        if (!vehicle.isAvailable) {
            Toast.makeText(this, getString(R.string.msg_vehicle_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        showBookingSheet(vehicle)
    }

    private fun setupOrderObservers() {
        var bookingSheet: BottomSheetDialog? = null

        orderViewModel.createOrderResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> { /* sheet 內的 ProgressBar 已顯示 */ }
                is NetworkResult.Success -> {
                    val order = result.data ?: return@observe
                    bookingSheet?.dismiss()
                    bookingSheet = null

                    val amount = order.totalAmount.toDoubleOrNull()?.toLong()
                    val amountText = if (amount != null) {
                        "NT$ ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
                    } else {
                        "NT$ ${order.totalAmount}"
                    }

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.title_order_created))
                        .setMessage(getString(R.string.msg_order_created_detail, amountText))
                        .setPositiveButton(getString(R.string.label_pay_now)) { _, _ -> orderViewModel.payOrder(order.id) }
                        .setNegativeButton(getString(R.string.label_pay_later)) { _, _ ->
                            startActivity(Intent(this, OrderListActivity::class.java))
                        }
                        .setCancelable(false)
                        .show()
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, getString(R.string.msg_booking_failed, result.message ?: ""), Toast.LENGTH_LONG).show()
                    // 讓 sheet 內的按鈕恢復（sheet 可能已關閉，但若還開著就恢復）
                }
            }
        }

        orderViewModel.payResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> { }
                is NetworkResult.Success -> {
                    Toast.makeText(this, getString(R.string.msg_payment_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, OrderListActivity::class.java))
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, getString(R.string.msg_payment_failed, result.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }

        // 讓 showBookingSheet 能控制 sheet 引用
        _bookingSheetRef = { bookingSheet = it }
    }

    private var _bookingSheetRef: ((BottomSheetDialog?) -> Unit)? = null

    private fun showBookingSheet(vehicle: Vehicle) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_booking, null)
        sheet.setContentView(view)
        _bookingSheetRef?.invoke(sheet)

        // 車輛資訊
        view.findViewById<TextView>(R.id.tvVehicleHeader).text = vehicle.name
        val typeName = when (vehicle.type) {
            "sedan_4" -> getString(R.string.vehicle_type_sedan)
            "van_9"   -> getString(R.string.vehicle_type_van)
            "bus_20"  -> getString(R.string.vehicle_type_bus)
            else      -> vehicle.type
        }
        val price = vehicle.pricePerDay.toDoubleOrNull()?.toLong()
        val priceText = if (price != null) {
            "NT$ ${NumberFormat.getNumberInstance(Locale.US).format(price)}"
        } else {
            "NT$ ${vehicle.pricePerDay}"
        }
        view.findViewById<TextView>(R.id.tvVehicleSub).text =
            "$typeName · ${getString(R.string.format_capacity, vehicle.capacity)} · ${getString(R.string.format_price_per_day, priceText)}"

        val etPickupLocation  = view.findViewById<TextInputEditText>(R.id.etPickupLocation)
        val etDropoffLocation = view.findViewById<TextInputEditText>(R.id.etDropoffLocation)
        val etPickupTime      = view.findViewById<TextInputEditText>(R.id.etPickupTime)
        val etDays            = view.findViewById<TextInputEditText>(R.id.etDays)
        val etPassengerCount  = view.findViewById<TextInputEditText>(R.id.etPassengerCount)
        val etContactName     = view.findViewById<TextInputEditText>(R.id.etContactName)
        val etContactPhone    = view.findViewById<TextInputEditText>(R.id.etContactPhone)
        val etSpecialRequests = view.findViewById<TextInputEditText>(R.id.etSpecialRequests)
        val tvAmountPreview   = view.findViewById<TextView>(R.id.tvAmountPreview)
        val pbBooking         = view.findViewById<ProgressBar>(R.id.pbBooking)
        val btnConfirm        = view.findViewById<MaterialButton>(R.id.btnConfirmBooking)
        val btnCancel         = view.findViewById<MaterialButton>(R.id.btnCancelBooking)

        // 天數變動 → 即時更新預估金額
        etDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val days = s?.toString()?.toIntOrNull() ?: 0
                if (price != null && days > 0) {
                    val total = price * days
                    tvAmountPreview.text =
                        getString(R.string.format_amount_preview, NumberFormat.getNumberInstance(Locale.US).format(total))
                } else {
                    tvAmountPreview.text = ""
                }
            }
        })

        // 上車時間 picker
        etPickupTime.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            val dateStr = String.format(
                                Locale.US, "%04d-%02d-%02dT%02d:%02d:00",
                                year, month + 1, day, hour, minute
                            )
                            etPickupTime.setText(dateStr.replace("T", " ").take(16))
                            etPickupTime.tag = dateStr  // 儲存完整 ISO 格式
                        },
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener { sheet.dismiss() }

        btnConfirm.setOnClickListener {
            val pickup   = etPickupLocation.text?.toString()?.trim() ?: ""
            val pickupTime = etPickupTime.tag as? String ?: ""
            val days     = etDays.text?.toString()?.toIntOrNull() ?: 0
            val pax      = etPassengerCount.text?.toString()?.toIntOrNull() ?: 0
            val name     = etContactName.text?.toString()?.trim() ?: ""
            val phone    = etContactPhone.text?.toString()?.trim() ?: ""

            when {
                pickup.isEmpty()    -> { Toast.makeText(this, getString(R.string.msg_booking_no_pickup), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                pickupTime.isEmpty() -> { Toast.makeText(this, getString(R.string.msg_booking_no_time), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                days <= 0           -> { Toast.makeText(this, getString(R.string.msg_booking_invalid_days), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                pax <= 0            -> { Toast.makeText(this, getString(R.string.msg_booking_no_pax), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                name.isEmpty()      -> { Toast.makeText(this, getString(R.string.msg_booking_no_contact_name), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                phone.isEmpty()     -> { Toast.makeText(this, getString(R.string.msg_booking_no_contact_phone), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            }

            pbBooking.visibility = View.VISIBLE
            btnConfirm.isEnabled = false
            btnCancel.isEnabled  = false

            val req = CreateOrderRequest(
                charter = CharterRequest(
                    vehicle_id       = vehicle.id,
                    pickup_location  = pickup,
                    dropoff_location = etDropoffLocation.text?.toString()?.trim()?.ifEmpty { null },
                    pickup_time      = pickupTime,
                    days             = days,
                    passenger_count  = pax,
                    contact_name     = name,
                    contact_phone    = phone,
                    special_requests = etSpecialRequests.text?.toString()?.trim()?.ifEmpty { null }
                )
            )
            orderViewModel.createOrder(req)
        }

        sheet.setOnDismissListener { _bookingSheetRef?.invoke(null) }
        sheet.show()
    }

    private fun setupChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFilter)

        data class FilterChip(val label: String, val type: String?)
        val filters = listOf(
            FilterChip(getString(R.string.category_all),         null),
            FilterChip(getString(R.string.vehicle_type_sedan),   "sedan_4"),
            FilterChip(getString(R.string.vehicle_type_van),     "van_9"),
            FilterChip(getString(R.string.vehicle_type_bus),     "bus_20")
        )

        filters.forEach { filter ->
            val chip = Chip(this).apply {
                text = filter.label
                isCheckable = true
                isChecked = (filter.type == null)
            }
            chip.setOnClickListener {
                currentType = filter.type
                vehicleViewModel.fetchVehicles(
                    type = currentType,
                    available = if (currentType != null) true else null
                )
            }
            chipGroup.addView(chip)
        }
    }
}
