package com.funTrip.fun2go.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.PublicItineraryAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar

class PublicItineraryListActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvPublicItineraries: RecyclerView
    private lateinit var adapter: PublicItineraryAdapter

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
                    Toast.makeText(this, "請先登入才能查看我的行程", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "請先登入才能複製行程", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.copyItinerary(itinerary.id)
                }
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
                    tvEmpty.text = "載入失敗，請重試"
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
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, "行程已複製！", Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(this)
                        .setTitle("複製成功")
                        .setMessage("行程已複製到你的帳號，是否前往我的行程？")
                        .setPositiveButton("前往") { _, _ ->
                            startActivity(Intent(this, ItineraryListActivity::class.java))
                        }
                        .setNegativeButton("稍後", null)
                        .show()
                }
                is NetworkResult.Error -> {
                    if (copyHandled) return@observe
                    copyHandled = true
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, "複製失敗：${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
