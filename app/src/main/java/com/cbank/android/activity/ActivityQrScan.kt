package com.cbank.android.activity

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.cbank.android.R
import com.cbank.android.databinding.ActivityQrScanBinding
import com.google.zxing.BarcodeFormat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.util.*


class ActivityQrScan : AppCompatActivity() {

    private lateinit var binding: ActivityQrScanBinding

    var codeScanner : CodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_qr_scan
        )

        setInit()
        setEvent()

    }


    private fun setUI() {

    }

    private fun setInit() {
        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
            .setPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .check()


        binding.ibClose.setColorFilter(Color.parseColor("#ffffff"),PorterDuff.Mode.SRC_IN)
        binding.ivQrCode.setColorFilter(Color.parseColor("#ffffff"),PorterDuff.Mode.SRC_IN)
        binding.ivRecycler.setColorFilter(Color.parseColor("#ffffff"),PorterDuff.Mode.SRC_IN)
        binding.ivLogo.setColorFilter(Color.parseColor("#1b213f"),PorterDuff.Mode.SRC_IN)

    }

    private fun setEvent() {}

    private fun setQrScan(){
        codeScanner = CodeScanner(this, binding.codeScannerView)


        codeScanner!!.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                AlertDialog.Builder(
                    this,
                    R.style.Theme_AppCompat_DayNight_Dialog_Alert
                ).setPositiveButton(
                    "전송"
                ) { dialogInterface, i ->
                    Toast.makeText(this,result.text, Toast.LENGTH_LONG).show()
                    val intent = Intent()
                    intent.putExtra("result", result.text)
                    setResult(RESULT_OK, intent)
                    finish()


                }
                    .setNegativeButton(
                        "취소"
                    ) { dialogInterface, i -> codeScanner!!.startPreview() }
                    .setMessage("전송하시겠습니까?")
                    .show()
            }
        }

        binding.codeScannerView.setOnClickListener {
            codeScanner!!.startPreview()
        }

        //QR코드만 스캔되게 설정
        val list = ArrayList<BarcodeFormat>()
        list.add(BarcodeFormat.QR_CODE)
        codeScanner!!.formats = list

        codeScanner!!.startPreview()
    }

    var permissionlistener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            setQrScan()
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            finish()
        }

    }

}