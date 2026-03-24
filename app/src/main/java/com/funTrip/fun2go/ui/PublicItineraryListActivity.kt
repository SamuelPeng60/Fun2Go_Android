package com.funTrip.fun2go.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.PublicItineraryAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.util.Calendar

class PublicItineraryListActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvPublicItineraries: RecyclerView
    private lateinit var adapter: PublicItineraryAdapter

    private var pendingItinerary: Itinerary? = null
    private var pendingStartDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public_itinerary_list)

        toolbar = findViewById(R.id.toolbar)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvPublicItineraries = findViewById(R.id.rvPublicItineraries)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.inflateMenu(R.menu.menu_public_itinerary_list)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menuMyItineraries) {
                if (viewModel.currentUser == null) {
                    Toast.makeText(this, getString(R.string.msg_login_for_my_itin), Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(this, ItineraryListActivity::class.java))
                }
                true
            } else false
        }

        adapter = PublicItineraryAdapter(
            onItemClick = { itinerary ->
                startActivity(
                    Intent(this, ItineraryDetailActivity::class.java).apply {
                        putExtra("itinerary_id", itinerary.id)
                        putExtra("itinerary_title", itinerary.title)
                    }
                )
            },
            onCopyClick = { itinerary ->
                if (viewModel.currentUser == null) {
                    Toast.makeText(this, getString(R.string.msg_login_for_copy), Toast.LENGTH_SHORT).show()
                } else {
                    showCopyDatePicker(itinerary)
                }
            },
            onAuthorClick = { itinerary ->
                startActivity(Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", itinerary.author_id)
                })
            }
        )
        rvPublicItineraries.layoutManager = LinearLayoutManager(this)
        rvPublicItineraries.adapter = adapter

        viewModel = ViewModelProvider(this)[ItineraryViewModel::class.java]
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchPublicItineraries()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupObservers() {
        viewModel.publicItineraries.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                    rvPublicItineraries.visibility = View.GONE
                    tvEmpty.visibility = View.GONE
                }
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val list = result.data ?: emptyList()
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvPublicItineraries.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvPublicItineraries.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    tvEmpty.text = getString(R.string.msg_load_public_failed)
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }

        var copyHandled = false
        viewModel.copyResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    copyHandled = false
                    pbLoading.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    if (copyHandled) return@observe
                    copyHandled = true
                    val itinerary = result.data ?: return@observe
                    pendingItinerary = itinerary
                    val startDate = pendingStartDate
                    if (startDate != null) {
                        viewModel.setDatesAfterCopy(itinerary.id, startDate)
                    } else {
                        pbLoading.visibility = View.GONE
                        navigateToDetail(itinerary)
                    }
                }
                is NetworkResult.Error -> {
                    if (copyHandled) return@observe
                    copyHandled = true
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.msg_copy_failed, result.message ?: ""), Toast.LENGTH_SHORT).show()
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
                    navigateToDetail(pending)
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    val pending = pendingItinerary ?: return@observe
                    pendingItinerary = null
                    navigateToDetail(pending)
                }
            }
        }
    }

    private fun showCopyDatePicker(itinerary: Itinerary) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            pendingStartDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            viewModel.copyItinerary(itinerary.id)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun navigateToDetail(itinerary: Itinerary) {
        startActivity(
            Intent(this, ItineraryDetailActivity::class.java).apply {
                putExtra("itinerary_id", itinerary.id)
                putExtra("itinerary_title", itinerary.title)
            }
        )
    }
}
