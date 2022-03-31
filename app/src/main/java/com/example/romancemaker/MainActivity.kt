package com.example.romancemaker

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {
    private lateinit var activityManager: ActivityManager
    private var playerContext: Context? = null

    private val musicList = mutableListOf<String>()
    private val idList = mutableListOf<Long>()
    private lateinit var listView :ListView

    private val PLAYER_ACT_NAME = "com.example.romancemaker.PlayerActivity"
    private var BT_SCAN_ENABLED: Boolean = false
    var advData_prev: Int = -1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLauncher: ActivityResultLauncher<Intent>

    private val leScanCb =
        object: ScanCallback() {
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                if(results != null){
                    for(result in results){
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                        }
                        if(result.device.name != null && result.scanRecord!!.manufacturerSpecificData[0] != null)
                            Log.w("BLE", "Scanned " + result.device.name)
                    }
                }
            }

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                if(result!!.device.name != null && result.scanRecord!!.manufacturerSpecificData[0] != null){

                    val advData: Int = result.scanRecord!!.manufacturerSpecificData[0].toHex().toInt() - 30
                    if(advData_prev != null && advData != advData_prev){
                        findViewById<TextView>(R.id.swTxt).text = "$advData found"
                        Log.w("BLE", "Scanned " + result.device.name)
                        Log.w("BLE", "Data $advData")
                        val topActivity = activityManager.getRunningTasks(1)[0].topActivity
                        Log.w("FG", "Top Activity name ${topActivity?.className}")
                            if(topActivity?.className.equals(PLAYER_ACT_NAME)){
                                View.c
                            }

                        listView.performItemClick(listView.getChildAt(advData), advData, listView.adapter.getItemId(advData))
                        advData_prev = advData
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.w("BLE SCAN CB", "Failed$errorCode")
            }
        }

    inner class CustomAdapter: BaseAdapter() {
        override fun getCount(): Int {
            return musicList.size
        }

        override fun getItem(p0: Int): Any? {
            return null
        }

        override fun getItemId(p0: Int): Long {
            return 0
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val myView = layoutInflater.inflate(R.layout.list_item, null)

            val songText = myView?.findViewById<TextView>(R.id.txtsongname)
            val orderTxt = myView?.findViewById<TextView>(R.id.orderTxt)

            songText?.text = musicList[p0]
            orderTxt?.text = p0.toString()

            return myView
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listViewSong)
        activityManager = getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(applicationContext, "Device not supports bluetooth", Toast.LENGTH_SHORT).show()
            Log.e("BLE", "Device not supports bluetoothLE")
            finish()
        }

        bluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                Log.w("BT", "Bluetooth on/off launcher registered")
            }
        }

        runtimePermission()
    }

    private fun runtimePermission() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object: MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    displaySong()
                    btJob()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }
            }).check()
    }

    fun displaySong(){
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE
        )

        val selection = "${MediaStore.Audio.Media.TITLE} LIKE '%'"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            proj, selection, null, sortOrder
        )

        cursor?.use {
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            while (cursor.moveToNext()){
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)

                idList.add(id)
                musicList.add(title)
            }
        }

        cursor?.close()

        listView.adapter = CustomAdapter()

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, p2, _ ->
                Log.d("DISPLAY", "displaySong: $p2")

                playerContext = Context()

                val intent = Intent(applicationContext, PlayerActivity::class.java)
                    .putExtra("songs", ArrayList(musicList))
                    .putExtra("ids", ArrayList(idList))
                    .putExtra("pos", p2)

                startActivity(intent)
            }
    }

    fun btJob(){
        val scanBtn: Button = findViewById(R.id.scanBtn)
        val statTxt: TextView = findViewById(R.id.statTxt)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null){
            Toast.makeText(applicationContext, "No bluetoothAdapter", Toast.LENGTH_SHORT).show()
            Log.e("BLE", "No bluetoothAdapter")
            finish()
        }

        scanBtn.setOnClickListener {
            if(!BT_SCAN_ENABLED){
                if(!bluetoothAdapter.isEnabled){
                    Log.w("BT", "Bluetooth on")
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothLauncher.launch(enableIntent)
                }

                statTxt.text = "Scanner On"
                BT_SCAN_ENABLED = true

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                Log.w("BLE", "Scanner on")
                bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCb)

                scanBtn.setBackgroundResource(R.drawable.ic_stopbt)
            }else{
                BT_SCAN_ENABLED = false

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                Log.w("BLE", "Scanner off")
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCb)

                statTxt.text = "Scanner Off"
                scanBtn.setBackgroundResource(R.drawable.ic_scanbt)
            }
        }
//        TODO("using LAST updated, user can notify when")
    }

    fun ByteArray.toHex(): String = joinToString(separator = "") { each -> "%02X".format(each)}

}