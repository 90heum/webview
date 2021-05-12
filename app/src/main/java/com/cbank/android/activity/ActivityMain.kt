package com.cbank.android.activity

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.cbank.android.R
import com.cbank.android.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File
import java.io.UnsupportedEncodingException
import java.lang.NullPointerException
import java.net.URISyntaxException
import java.net.URLDecoder
import java.util.*
class ActivityMain : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object {
  //    var baseUri = "file:///android_asset/" + "index.html"
//        var baseUri = "https://cryptobank.design/"
        var baseUri = "http://3.35.239.131/#/Main00"

        var webViewPop: WebView? = null
        var mWebSettings: WebSettings? = null
        private var childView: Stack<WebView>? = null
        private var currentWebView: WebView? = null

        private var filePathCallbackNormal: ValueCallback<Uri>? = null
        private var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
        private val FILECHOOSER_NORMAL_REQ_CODE = 1
        private val FILECHOOSER_LOLLIPOP_REQ_CODE = 2

        private var cameraImageUri: Uri? = null

        private val cookieManager = CookieManager.getInstance()

        var backPressCloseHandler: BackPressCloseHandler? = null

        lateinit var context: Context

        private var nStatus : Int? = null

        private lateinit var biometricPrompt: BiometricPrompt
        private lateinit var promptInfo: BiometricPrompt.PromptInfo
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )

        setInit()
        setEvent()
        setWebView()

    }
    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
        webViewPop!!.onPause()
        currentWebView!!.onPause()
    }
    override fun onResume() {
        super.onResume()
        nStatus = BiometricManager.from(this).canAuthenticate()
        binding.webView.onResume()
        webViewPop!!.onResume()
        currentWebView!!.onResume()
    }

    fun setInit() {
        binding.activity = this@ActivityMain
        context = applicationContext
        webViewPop = binding.webView
        childView = Stack()
        val pref = this.getPreferences(0)
        val edit = pref.edit()

        makeNotificationChannel()

        getToken()

//        checkLocation()

        cookieManager.setAcceptCookie(true)

        backPressCloseHandler = BackPressCloseHandler(this)

        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()



        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                .setPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                .check()

    }



    fun setEvent() {

    }


    private fun setWebView() {

        binding.webView!!.webViewClient = MyWebViewClient()
        binding.webView!!.webChromeClient = MyChromeClient(this@ActivityMain, binding)

        val settings = binding.webView.settings
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.useWideViewPort = true // 화면 사이즈 맞추기 허용
        settings.javaScriptEnabled = true // javascript 이용할 수 있게함
        settings.domStorageEnabled = true // 내부 저장소 이용할 수 있게 함
        settings.loadWithOverviewMode = true

        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN // 화면 맞춤
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN)

        if (Build.VERSION.SDK_INT >= 21) {
            binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } else {
            binding.webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        settings.setGeolocationEnabled(true)
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.setSupportMultipleWindows(true)
        settings.setGeolocationEnabled(true)

        binding.webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val permissionListener: PermissionListener = object : PermissionListener {
                override fun onPermissionGranted() {
                    if (Build.VERSION.SDK_INT >= 23 &&
                        ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@ActivityMain,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            0
                        )
                    } else {
                        val request =
                            DownloadManager.Request(Uri.parse(url))
                        request.setMimeType(mimeType)
                        //------------------------COOKIE!!------------------------
                        val cookies =
                            CookieManager.getInstance().getCookie(url)
                        request.addRequestHeader("cookie", cookies)
                        //------------------------COOKIE!!------------------------
                        request.addRequestHeader("User-Agent", userAgent)
                        request.setDescription("Downloading file...")
                        request.setTitle(
                            URLUtil.guessFileName(
                                url,
                                contentDisposition,
                                mimeType
                            )
                        )
                        request.allowScanningByMediaScanner()
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(
                                url,
                                contentDisposition,
                                mimeType
                            )
                        )
                        val dm =
                            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        dm.enqueue(request)
                        Toast.makeText(
                            applicationContext,
                            "Downloading File",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {}
            }
            TedPermission.with(binding.activity)
                .setPermissionListener(permissionListener)
                .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check()
        })


        binding.webView.addJavascriptInterface(
            AndroidBridge(binding.activity!!),
            "webview"
        )

        childView!!.push(binding.webView)
        currentWebView = binding.webView
        currentWebView!!.loadUrl(baseUri)

        registerForContextMenu(binding.webView)


    }

    fun getToken() {

        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {

                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token

                    // Log and toast
                    val msg = token
                    Log.d("111111", token.toString())

                    val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                    val edit = pref.edit()
                    edit.putString("token", token.toString()).apply()

                })


    }

    /**
     *
     * 푸시 토큰 가져오기
     * strToken = window.webview.getToken();
     *
     *
     * 위도, 경도 가져오기
     * strLatitude = window.webview.getLatitude();
     * strLongitude = window.webview.getLongitude();
     *
     *
     * QrCode 호출
     * window.webview.callQrCode();
     *
     * QrCode 결과 값 받기
     * function getQrCodeResult(data){
     *      alert(data)
     * }
     *
     *
     * Biometric 호출
     * window.webview.callBiometric();
     *
     *
     *
     * Biometric 상태 값 받기
     *      0 : 인증 성공
     *      1 : 인증 실패
     *      2 : BIOMETRIC_ERROR_HW_UNAVAILABLE
     *      3 : BIOMETRIC_ERROR_NONE_ENROLLED
     *      4 : BIOMETRIC_ERROR_NO_HARDWARE
     *      5 : 기타
     *
     * function getBioMetricState(data){
     *      alert(data)
     * }
     *
     */

    // 2021-02-자동로그인
    class AndroidBridge(activity: ActivityMain) {

        val activity = activity

        @JavascriptInterface
        fun getToken(): String {
            val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
            return pref.getString("token", "")!!
        }

        @JavascriptInterface
        fun getLatitude(): String {
            val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
            return pref.getString("latitude", "")!!
        }
        @JavascriptInterface
        fun getLongitude(): String {
            val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
            return pref.getString("longitude", "")!!
        }

        @JavascriptInterface
        fun callQrCode() {
            activity.actionQrScanner()
        }

        @JavascriptInterface
        fun callBiometric() {
            activity.actionBiometric()
        }




    }

    override fun onBackPressed() {

        if (currentWebView != binding.webView) {
            val animation = AnimationUtils.loadAnimation(baseContext, R.anim.right_out)
            animation.startOffset = 0
            animation.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    if (currentWebView !== binding.webView) {
                        binding.webRoot.removeView(currentWebView)
                        currentWebView!!.destroy()
                        currentWebView = childView!!.pop()
                    }
                }
                override fun onAnimationRepeat(animation: Animation) {}
            })
            currentWebView!!.startAnimation(animation)
        } else if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            backPressCloseHandler!!.onBackPressed()
        }
    }


    var permissionlistener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {

            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val isGPSEnabled: Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled: Boolean = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                //매니페스트에 권한이 추가되어 있다해도 여기서 다시 한번 확인해야함
                if (Build.VERSION.SDK_INT >= 23 &&
                        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(binding.activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
                } else {
                    when { //프로바이더 제공자 활성화 여부 체크
                        isNetworkEnabled -> {
                            val location =
                                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) //인터넷기반으로 위치를 찾음

                            val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                            val edit = pref.edit()
                            edit.putString("latitude", location?.latitude.toString()!!).apply()
                            edit.putString("longitude", location?.longitude.toString()!!).apply()

//                            Toast.makeText(context,"latitude ${location?.latitude} / longitude : ${location?.longitude}", Toast.LENGTH_LONG).show()
//                            Log.d("111111", "latitude ${location?.latitude} / longitude : ${location?.longitude}")

                        }
                        isGPSEnabled -> {
                            val location =
                                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) //GPS 기반으로 위치를 찾음

                            val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                            val edit = pref.edit()
                            edit.putString("latitude", location?.latitude.toString()!!).apply()
                            edit.putString("longitude", location?.longitude.toString()!!).apply()

//                            Toast.makeText(context,"latitude ${location?.latitude} / longitude : ${location?.longitude}", Toast.LENGTH_LONG).show()
//                            Log.d("111111", "latitude ${location?.latitude} / longitude : ${location?.longitude}")

                        }
                        else -> {

                        }
                    }
                }

        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
//            finish()
        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        var data = data
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_NORMAL_REQ_CODE) {
            if (filePathCallbackNormal == null) return
            val result =
                if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            filePathCallbackNormal!!.onReceiveValue(result)
            filePathCallbackNormal = null
        } else if (requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE) {
            if (filePathCallbackLollipop == null) return
            if (data == null) data = Intent()
            if (data.data == null) data.data = cameraImageUri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePathCallbackLollipop!!.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        data
                    )
                )
            }
            filePathCallbackLollipop = null
        } else if (requestCode == 2000) {
            try {
                val result = data!!.getStringExtra("result")
                Log.d("111111", "result : $result")

                currentWebView!!.loadUrl("javascript:getQrCodeResult($result)")
            }catch (e : NullPointerException){
                e.printStackTrace()
            }
        }
    }

    fun checkLocation() {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                binding.webView.reload()
                currentWebView!!.reload()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
            }
        }
        TedPermission.with(binding.activity)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .check()
    }

    class BackPressCloseHandler(private val activity: Activity) {
        private var backKeyPressedTime: Long = 0
        private var toast: Toast? = null
        fun onBackPressed() {
            if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                backKeyPressedTime = System.currentTimeMillis()
                toast = Toast.makeText(
                    activity, "종료하시려면 뒤로가기 버튼을 한번 더 눌러주세요.",
                    Toast.LENGTH_SHORT
                )
                toast!!.show()
                return
            }
            if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
                activity.finish()
                toast!!.cancel()
            }
        }

    }


    fun makeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pref =
                getSharedPreferences("pref", Context.MODE_PRIVATE)
            val notification_channel = pref.getString("notification_channel", null)
            if (notification_channel == null || notification_channel.length == 0) {
                val channelID = getString(R.string.app_name)
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.deleteNotificationChannel(channelID)
                val notificationChannel = NotificationChannel(
                    channelID,
                    getString(R.string.app_name) + " 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.description = getString(R.string.app_name) + " 알림 설정"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.GREEN
                notificationChannel.enableVibration(true)
                notificationChannel.vibrationPattern = longArrayOf(100, 200, 100, 200)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                notificationManager.createNotificationChannel(notificationChannel)
                val editor = pref.edit()
                editor.putString("notification_channel", channelID)
                editor.apply()
            }
        }

    }

    fun runCamera(_isCapture: Boolean) {
        if (!_isCapture) {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "*/*"
            startActivityForResult(
                Intent.createChooser(i, "File Chooser"),
                FILECHOOSER_LOLLIPOP_REQ_CODE
            )
            return
        }
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val path = filesDir
        val file = File(path, "capture.png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val strpa = applicationContext.packageName
            cameraImageUri = FileProvider.getUriForFile(this, "$strpa.fileprovider", file)
        } else {
            cameraImageUri = Uri.fromFile(file)
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        if (!_isCapture) {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
            pickIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val pickTitle = "사진 가져올 방법을 선택하세요."
            val chooserIntent = Intent.createChooser(pickIntent, pickTitle)
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(intentCamera))
            startActivityForResult(
                chooserIntent,
                FILECHOOSER_LOLLIPOP_REQ_CODE
            )
        } else {
            startActivityForResult(
                intentCamera,
                FILECHOOSER_LOLLIPOP_REQ_CODE
            )
        }
    }


    private class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String
        ): Boolean {
            Log.d("url", url)
            if (url.startsWith("http:") || url.startsWith("https:")) {
                if (url.endsWith(".pdf") || url.startsWith("https://m.youtube.com")) {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.data = Uri.parse(url)
                    view.context.startActivity(intent)
                    return true
                }
                return false
            } else if (url.startsWith("kakaotalk:")) {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(url)
                view.context.startActivity(intent)
                return true
            } else if (url.startsWith("tel:")) {
                val tel = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                view.context.startActivity(tel)
                return true
            } else if (url.startsWith("sms:")) {
                val sms = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                try {
                    sms.putExtra(
                        "sms_body",
                        URLDecoder.decode(
                            url.substring(url.indexOf("?body=") + 6),
                            "utf-8"
                        )
                    )
                    view.context.startActivity(sms)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                return true
            }else if (url.startsWith("market:")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        // forbid launching activities without BROWSABLE category
                        intent.addCategory("android.intent.category.BROWSABLE")
                        // forbid explicit call
                        intent.component = null
                        // forbid Intent with selector Intent
                        intent.selector = null
                        view.context.startActivity(intent)
                    }
                    return true
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            } else if (url.startsWith("camera")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        // forbid launching activities without BROWSABLE category
                        intent.addCategory("android.intent.category.BROWSABLE")
                        // forbid explicit call
                        intent.component = null
                        // forbid Intent with selector Intent
                        intent.selector = null
                        view.context.startActivity(intent)
                    }
                    return true
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            } else if (url.startsWith("intent:")) {
                try {

                    // 카카오톡 관련 소스
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    // 실행 가능한 앱이 있으면 앱 실행
                    if (intent.resolveActivity(view.context.packageManager) != null) {
                        view.context.startActivity(intent)
                        Log.d("should", "ACTIVITY: ${intent.`package`}")
                        return true
                    }

                    // Fallback URL이 있으면 현재 웹뷰에 로딩
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        view.loadUrl(fallbackUrl)
                        return true
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }
    }

    private class MyChromeClient(activity: Activity, binding: ActivityMainBinding) :
        WebChromeClient() {
        var activity = activity
        var binding = binding


        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            return super.onConsoleMessage(consoleMessage)
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, true)
        }

        fun openFileChooser(
            uploadMsg: ValueCallback<Uri>?,
            acceptType: String?
        ) {
            filePathCallbackNormal = uploadMsg
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "*/*"
            activity.startActivityForResult(
                Intent.createChooser(i, "File Chooser"),
                FILECHOOSER_NORMAL_REQ_CODE
            )
        }


        // For Android 5.0+
        override fun onShowFileChooser(
            webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop!!.onReceiveValue(null)
                filePathCallbackLollipop = null
            }
            filePathCallbackLollipop = filePathCallback
            var isCapture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                isCapture = fileChooserParams.isCaptureEnabled
            }

            binding.activity!!.runCamera(isCapture)
            return true
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {

            val nWebView = WebView(view.context)
            val settings = nWebView.settings
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            nWebView.webViewClient = MyWebViewClient()
            nWebView.webChromeClient = MyChromeClient(activity, binding)
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true

            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN // 화면 맞춤
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN)

            if (Build.VERSION.SDK_INT >= 21) {
                nWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            } else {
                nWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            settings.setGeolocationEnabled(true)
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.setSupportMultipleWindows(true)

            view.addJavascriptInterface(
                AndroidBridge(binding.activity!!),
                "webview"
            )

            binding.webRoot.addView(nWebView)
            childView!!.push(currentWebView)
            currentWebView = nWebView
            nWebView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            val animation =
                AnimationUtils.loadAnimation(view.context, R.anim.right_in)
            animation.startOffset = 0
            nWebView.startAnimation(animation)
            val transport = resultMsg.obj as WebViewTransport
            transport.webView = nWebView
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) {
            if (currentWebView != binding.webView) {
                val animation = AnimationUtils.loadAnimation(activity.getBaseContext(), R.anim.right_out)
                animation.startOffset = 0
                animation.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        if (currentWebView !== binding.webView) {
                            binding.webRoot.removeView(currentWebView)
                            currentWebView!!.destroy()
                            currentWebView = childView!!.pop()
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                currentWebView!!.startAnimation(animation)
            }
        }
    }

    fun actionQrScanner(){
        val intent = Intent(binding.activity, ActivityQrScan::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivityForResult(intent, 2000)
        overridePendingTransition(0, 0)

    }

    fun actionBiometric(){


        if (nStatus == BiometricManager.BIOMETRIC_SUCCESS) {

            biometricPrompt.authenticate(promptInfo)

        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_HW_UNAVAILABLE",
                Toast.LENGTH_LONG
            ).show()

            currentWebView!!.loadUrl("javascript:getBioMetricState(" + 2 + ")")


        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_NONE_ENROLLED",
                Toast.LENGTH_LONG
            ).show()

            currentWebView!!.loadUrl("javascript:getBioMetricState(" + 3 + ")")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                startActivity(intent)
            }else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val intent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                    startActivity(intent)
                }else {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                }
            }


        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_NO_HARDWARE",
                Toast.LENGTH_LONG
            ).show()
            currentWebView!!.loadUrl("javascript:getBioMetricState(" + 4 + ")")


        }else {
            Toast.makeText(
                applicationContext,
                "사용할 수 없는 기기",
                Toast.LENGTH_LONG
            ).show()

            currentWebView!!.loadUrl("javascript:getBioMetricState(" + 5 + ")")

        }

    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체인증")
            .setDescription("생체인증을 시작합니다.")
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.prompt_info_use_app_password))
            .build()
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d("TAG", "$errorCode :: $errString")
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("TAG", "인증 실패")

                Toast.makeText(
                    applicationContext,
                    "인증 실패",
                    Toast.LENGTH_LONG
                ).show()

                currentWebView!!.loadUrl("javascript:getBioMetricState(" + 1 + ")")

            }
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(
                    applicationContext,
                    "인증 성공",
                    Toast.LENGTH_LONG
                ).show()
                //인증 완료 시 작업

                currentWebView!!.loadUrl("javascript:getBioMetricState(" + 0 + ")")
            }
        }
            //The API requires the client/Activity context for displaying the prompt view
        return BiometricPrompt(this, executor, callback)
    }


    fun actionLocation(){
        val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
        Toast.makeText(context,"latitude ${pref.getString("latitude","")} / longitude : ${pref.getString("longitude","")}", Toast.LENGTH_LONG).show()
    }


    fun actionBio(){
        val intent = Intent(binding.activity, ActivityBioMetric::class.java)
        startActivity(intent)
    }

}
