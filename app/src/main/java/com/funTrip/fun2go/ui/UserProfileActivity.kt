package com.funTrip.fun2go.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.ui.adapter.PublicItineraryAdapter
import com.funTrip.fun2go.ui.viewmodel.ItineraryViewModel
import com.google.android.material.appbar.MaterialToolbar

class UserProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ItineraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userId = intent.getIntExtra("user_id", -1)
        if (userId == -1) { finish(); return }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val ivAvatar  = findViewById<ImageView>(R.id.ivAvatar)
        val tvName    = findViewById<TextView>(R.id.tvName)
        val tvEmail   = findViewById<TextView>(R.id.tvEmail)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvEmpty   = findViewById<TextView>(R.id.tvEmpty)
        val rvItins   = findViewById<RecyclerView>(R.id.rvItineraries)

        viewModel = ViewModelProvider(this)[ItineraryViewModel::class.java]

        val adapter = PublicItineraryAdapter(
            onItemClick = { itinerary ->
                startActivity(Intent(this, ItineraryDetailActivity::class.java).apply {
                    putExtra("itinerary_id", itinerary.id)
                    putExtra("itinerary_title", itinerary.title)
                    putExtra("cover_image_url", itinerary.coverImageUrl)
                })
            },
            onCopyClick = { /* 不在個人頁提供複製 */ }
        )
        rvItins.layoutManager = LinearLayoutManager(this)
        rvItins.adapter = adapter

        // 觀察用戶資訊
        viewModel.viewedUser.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val user = result.data ?: return@observe
                    toolbar.title = user.name
                    tvName.text   = user.name
                    tvEmail.text  = user.email ?: ""
                    tvEmail.visibility = if (!user.email.isNullOrEmpty()) View.VISIBLE else View.GONE
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        ivAvatar.load(user.avatarUrl) {
                            crossfade(true)
                            transformations(CircleCropTransformation())
                        }
                    }
                }
                else -> {}
            }
        }

        // 觀察行程列表
        viewModel.viewedUserItineraries.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> pbLoading.visibility = View.VISIBLE
                is NetworkResult.Success -> {
                    pbLoading.visibility = View.GONE
                    val list = (result.data ?: emptyList()).filter { it.is_public }
                    if (list.isEmpty()) {
                        tvEmpty.visibility   = View.VISIBLE
                        rvItins.visibility   = View.GONE
                    } else {
                        tvEmpty.visibility   = View.GONE
                        rvItins.visibility   = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is NetworkResult.Error -> {
                    pbLoading.visibility = View.GONE
                    tvEmpty.visibility   = View.VISIBLE
                    rvItins.visibility   = View.GONE
                }
            }
        }

        viewModel.fetchUserProfile(userId)
        viewModel.fetchViewedUserItineraries(userId)
    }
}
