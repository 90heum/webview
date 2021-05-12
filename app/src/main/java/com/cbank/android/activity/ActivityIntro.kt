package com.cbank.android.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.cbank.android.R
import com.cbank.android.databinding.ActivityIntroBinding


class ActivityIntro : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    private var hIntro: Handler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_intro
        )

        setInit()
        setEvent()

    }


    private fun setUI() {}

    private fun setInit() {


        Glide.with(this).load(R.drawable.water_intro).into(binding.splashGifImage)

        hIntro = Handler()
        hIntro!!.postDelayed(rIntro, 2000)
    }

    private fun setEvent() {}


    private val rIntro = Runnable { moveToLogin() }

    private fun moveToLogin() {
        val intent = Intent(this@ActivityIntro, ActivityMain::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}