package com.example.walkingpark

import android.Manifest
import android.content.*
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.walkingpark.components.foreground.service.ParkMapsService
import com.example.walkingpark.database.singleton.Common
import com.example.walkingpark.databinding.ActivityMainBinding
import com.example.walkingpark.factory.PublicApiViewModelFactory
import com.example.walkingpark.tabs.tab_1.HomeFragment
import com.example.walkingpark.tabs.tab_2.ParkMapsFragment
import com.example.walkingpark.tabs.tab_3.SettingsFragment
import com.example.walkingpark.repository.RestApiRepository

// TODO 데이터 바인딩 대체?? -> 자세히 알아볼 것 !!!!
// TODO DAGGER 공부 -> 의존주입에 관해 이해
// TODO Coroutine 공부 -> 더욱 확실히 학습!!!!!

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private lateinit var viewModel: MainViewModel

    lateinit var parkMapsService: ParkMapsService       // 서비스 객체
    private var isParkMapsServiceRunning = false
    lateinit var parkMapsReceiver: BroadcastReceiver

    // TODO 측정소 정보를 가져오려면, 현재 위경도 좌표를 tm 좌표로 변환해야 하며, jar 과 같은 외부 라이브러리는 부정확함.
    // TODO 다른 API 연동이 필요하여 이를 보류.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding!!.lifecycleOwner = this
        viewModel = ViewModelProvider(
            this,
            PublicApiViewModelFactory(RestApiRepository(this))
        )[MainViewModel::class.java]

        setBottomButtonsAsTab()         // 하단 버튼 설정
        startParkMapsService()          // 위치데이터 서비스 실행

        // 퍼미션 요청 핸들링. (onActivityResult 대체)
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.e("퍼미션 요청", "퍼미션 요청")
            val permissionCheck = viewModel.handleLocationPermissions(permissions)
            if (permissionCheck) {
                val intent = Intent(this, ParkMapsService::class.java)
                intent.putExtra("requestCode", Common.PERMISSION)
                startParkMapsService(intent)
            } else {

            }
        }
        // 퍼미션 요청 수행!!
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 백그라운드 위치정보 서비스 활성 및 콜백 등록.
    private fun startParkMapsService() {

        val serviceConnection: ServiceConnection = object : ServiceConnection {
            // 서비스 연결 관련 콜백
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder
            ) {
                // 서비스와 연결되었을 때 호출되는 메서드
                // 서비스 객체를 전역변수로 저장
                parkMapsService = viewModel.getParkMapsService(service)
                isParkMapsServiceRunning = true

                // 서비스에서 맵 요청 콜백이 완료됨에 따라 이를 액티비티에 알려주기 위한 리시버 초기화 및 등록
                parkMapsReceiver = ParkMapsReceiver(applicationContext)
                registerReceiver(parkMapsReceiver, IntentFilter(Common.REQUEST_ACTION_UPDATE))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                // 서비스와 연결이 끊겼을 때 호출되는 메서드
                isParkMapsServiceRunning = false
                Toast.makeText(
                    applicationContext,
                    "위치 서비스 연결 해제됨",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        // 서비스 실행
        val intent = Intent(this, ParkMapsService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // 서비스를 간단하게 호출하기 위한 메서드
    // 서비스에 다른 요청사항을
    private fun startParkMapsService(intent: Intent) {
        Log.e("sendToService", "sendToService")
        // 버전별 포그라운드 서비스 실행을 위한 별도의 처리 필요. 오레오 이상은 포그라운드 서비스를 명시해주어야 하는듯
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    }

    private fun setBottomButtonsAsTab() {
        binding!!.buttonHome.setOnClickListener {
            val transaction1 = supportFragmentManager.beginTransaction()
            transaction1.replace(R.id.fragmentContainer, HomeFragment()).commit()
        }

        binding!!.buttonMaps.setOnClickListener {
            val transaction2 = supportFragmentManager.beginTransaction()
            transaction2.replace(R.id.fragmentContainer, ParkMapsFragment()).commit()
        }

        binding!!.buttonSettings.setOnClickListener {
            val transaction3 = supportFragmentManager.beginTransaction()
            transaction3.replace(R.id.fragmentContainer, SettingsFragment()).commit()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        requestLocationUpdate()
    }

    private fun requestLocationUpdate() {
        val intent = Intent(this, ParkMapsFragment::class.java)
        intent.putExtra("requestCode", Common.LOCATION_UPDATE)
        startParkMapsService(intent)
    }

    class ParkMapsReceiver(val context: Context) : BroadcastReceiver() {
        override fun onReceive(p0: Context?, result: Intent?) {
            Log.e("ParkMapsReceiver", "ParkMapsReceiver")
            when (result!!.action) {
                Common.REQUEST_ACTION_UPDATE -> {
                    val intent = Intent(context, ParkMapsService::class.java)
                    intent.putExtra("requestCode", Common.LOCATION_UPDATE)
                    context.startService(intent)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {}
}