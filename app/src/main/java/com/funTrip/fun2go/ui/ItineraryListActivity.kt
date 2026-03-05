package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import java.util.Calendar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.ItineraryAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ItineraryListActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvItineraries: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: ItineraryAdapter

    private var createDialog: BottomSheetDialog? = null
    private var editDialog: BottomSheetDialog? = null
    private var pendingItinerary: Itinerary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_list)

        toolbar = findViewById(R.id.toolbar)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvItineraries = findViewById(R.id.rvItineraries)
        fabAdd = findViewById(R.id.fabAdd)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_my_itineraries)
        }

        adapter = ItineraryAdapter(
            onItemClick = { itinerary ->
                val intent = Intent(this, ItineraryDetailActivity::class.java)
                intent.putExtra("itinerary_id", itinerary.id)
                intent.putExtra("itinerary_title", itinerary.title)
                startActivity(intent)
            },
            onEditClick = { itinerary ->
                showEditSheet(itinerary)
            },
            onDeleteClick = { itinerary ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_delete_itin))
                    .setMessage(getString(R.string.msg_delete_itin_confirm, itinerary.title))
                    .setPositiveButton(getString(R.string.label_delete)) { _, _ ->
                        viewModel.deleteItinerary(itinerary.id)
                    }
                    .setNegativeButton(getString(R.string.label_cancel), null)
                    .show()
            }
        )
        rvItineraries.layoutManager = LinearLayoutManager(this)
        rvItineraries.adapter = adapter

        fabAdd.setOnClickListener { showCreateSheet() }

        viewModel = ViewModelProvider(this)[ItineraryViewModel::class.java]
        setupObservers()
        setupBottomNav()
    }

    private fun setupBottomNav() {
        findViewById<ImageButton>(R.id.btnNavMap).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
        }
        // btnNavMyItin → 已在此頁，不需跳轉
        findViewById<ImageButton>(R.id.btnNavCharter).setOnClickListener {
            startActivity(Intent(this, VehicleListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
        }
        findViewById<ImageButton>(R.id.btnNavProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("open_profile", true)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = viewModel.currentUser?.id
        Log.d("ILA_DEBUG", "onResume: userId=$userId")
        if (userId == null) {
            finish()
            return
        }
        if (userId > 0) {
            viewModel.fetchUserItineraries(userId)
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
        viewModel.userItineraries.observe(this) { result ->
            Log.d("ILA_DEBUG", "userItineraries: ${result::class.simpleName}" +
                if (result is NetworkResult.Success) " count=${result.data?.size}" else "")
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                    rvItineraries.visibility = View.GONE
                    tvEmpty.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val currentId = viewModel.currentUser?.id ?: 0
                    // 只顯示自己的行程（author_id == currentId）
                    // author_id == 0 表示 API 未回傳 user_id，保留（避免誤過濾）
                    val list = (result.data ?: emptyList()).filter { itin ->
                        currentId == 0 || itin.author_id == 0 || itin.author_id == currentId
                    }
                    if (list.isEmpty()) {
                        tvEmpty.text = getString(R.string.empty_itineraries)
                        tvEmpty.visibility = View.VISIBLE
                        rvItineraries.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvItineraries.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    Log.d("ILA_DEBUG", "userItineraries Error: ${result.message}")
                    pbLoading.visibility = View.GONE
                    tvEmpty.text = getString(R.string.msg_load_itin_failed)
                    tvEmpty.visibility = View.VISIBLE
                    Snackbar.make(toolbar, getString(R.string.msg_itin_load_failed2, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    viewModel.currentUser?.id?.takeIf { it > 0 }?.let {
                        viewModel.fetchUserItineraries(it)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    Snackbar.make(toolbar, getString(R.string.msg_delete_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.initDaysResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val pending = pendingItinerary ?: return@observe
                    pendingItinerary = null
                    startActivity(
                        Intent(this, ItineraryDetailActivity::class.java).apply {
                            putExtra("itinerary_id", pending.id)
                            putExtra("itinerary_title", pending.title)
                        }
                    )
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    val pending = pendingItinerary ?: return@observe
                    pendingItinerary = null
                    startActivity(
                        Intent(this, ItineraryDetailActivity::class.java).apply {
                            putExtra("itinerary_id", pending.id)
                            putExtra("itinerary_title", pending.title)
                        }
                    )
                }
            }
        }
    }

    private fun showCreateSheet() {
        if (createDialog?.isShowing == true) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_itinerary, null)

        val tilTitle    = view.findViewById<TextInputLayout>(R.id.tilItineraryTitle)
        val etTitle     = view.findViewById<TextInputEditText>(R.id.etItineraryTitle)
        val actvDest    = view.findViewById<AutoCompleteTextView>(R.id.actvDestination)
        val etTotalDays = view.findViewById<TextInputEditText>(R.id.etTotalDays)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.etStartDate)
        val btnCreate   = view.findViewById<MaterialButton>(R.id.btnCreateItinerary)
        val btnCancel   = view.findViewById<MaterialButton>(R.id.btnCancelCreate)
        val pbCreating  = view.findViewById<ProgressBar>(R.id.pbCreating)

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

        // hasStarted：防止 LiveData sticky 把舊的 Success/Error 立刻送進來
        // navigated：防止 Success 被處理兩次
        var hasStarted = false
        var navigated = false

        val createObserver = Observer<NetworkResult<Itinerary>> { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    hasStarted = true
                    btnCreate.isEnabled = false
                    pbCreating.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    if (!hasStarted || navigated) return@Observer  // 忽略 stale cached 值
                    navigated = true
                    btnCreate.isEnabled = true
                    pbCreating.visibility = View.GONE
                    val itinerary = result.data ?: return@Observer
                    Log.d("ILA_DEBUG", "create Success itinerary.id=${itinerary.id}")
                    pendingItinerary = itinerary
                    dialog.dismiss()
                    pbCreating.visibility = View.VISIBLE
                    viewModel.initItineraryDays(itinerary.id, itinerary.total_days, selectedStartDate)
                }
                is NetworkResult.Error -> {
                    if (!hasStarted) return@Observer  // 忽略 stale cached 值
                    btnCreate.isEnabled = true
                    pbCreating.visibility = View.GONE
                    Snackbar.make(view, getString(R.string.msg_create_itin_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
        }
        viewModel.createResult.observe(this, createObserver)

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                tilTitle.error = getString(R.string.msg_itin_title_required)
                return@setOnClickListener
            }
            tilTitle.error = null
            val dest      = actvDest.text?.toString()?.trim()
            val totalDays = etTotalDays.text?.toString()?.trim()?.toIntOrNull()
            viewModel.createItinerary(title, totalDays, dest)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            viewModel.createResult.removeObserver(createObserver)
            createDialog = null
        }

        createDialog = dialog
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showEditSheet(itinerary: Itinerary) {
        if (editDialog?.isShowing == true) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_itinerary, null)

        val tilTitle    = view.findViewById<TextInputLayout>(R.id.tilItineraryTitle)
        val etTitle     = view.findViewById<TextInputEditText>(R.id.etItineraryTitle)
        val actvDest    = view.findViewById<AutoCompleteTextView>(R.id.actvDestination)
        val etTotalDays = view.findViewById<TextInputEditText>(R.id.etTotalDays)
        val btnUpdate   = view.findViewById<MaterialButton>(R.id.btnUpdateItinerary)
        val btnCancel   = view.findViewById<MaterialButton>(R.id.btnCancelEdit)
        val pbUpdating  = view.findViewById<ProgressBar>(R.id.pbUpdating)

        val destinations = resources.getStringArray(R.array.destinations)
        actvDest.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, destinations))
        actvDest.setOnClickListener { actvDest.showDropDown() }

        etTitle.setText(itinerary.title)
        actvDest.setText(itinerary.destination ?: "", false)
        etTotalDays.setText(if (itinerary.total_days > 0) itinerary.total_days.toString() else "")

        // hasStarted：防止 LiveData sticky 把舊的 Success 立刻送進來（避免 dialog 一開就關）
        var hasStarted = false

        val updateObserver = Observer<NetworkResult<Itinerary>> { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    hasStarted = true
                    btnUpdate.isEnabled = false
                    pbUpdating.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    if (!hasStarted) return@Observer  // 忽略 stale cached 值
                    btnUpdate.isEnabled = true
                    pbUpdating.visibility = View.GONE
                    dialog.dismiss()
                    viewModel.currentUser?.id?.takeIf { it > 0 }?.let {
                        viewModel.fetchUserItineraries(it)
                    }
                }
                is NetworkResult.Error -> {
                    if (!hasStarted) return@Observer  // 忽略 stale cached 值
                    btnUpdate.isEnabled = true
                    pbUpdating.visibility = View.GONE
                    Snackbar.make(view, getString(R.string.msg_update_itin_failed, result.message ?: ""), Snackbar.LENGTH_LONG).show()
                }
            }
        }
        viewModel.updateResult.observe(this, updateObserver)

        btnUpdate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                tilTitle.error = getString(R.string.msg_itin_title_required)
                return@setOnClickListener
            }
            tilTitle.error = null
            val dest      = actvDest.text?.toString()?.trim()
            val totalDays = etTotalDays.text?.toString()?.trim()?.toIntOrNull()
            viewModel.updateItinerary(itinerary.id, title, totalDays, dest)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            viewModel.updateResult.removeObserver(updateObserver)
            editDialog = null
        }

        editDialog = dialog
        dialog.setContentView(view)
        dialog.show()
    }
}
