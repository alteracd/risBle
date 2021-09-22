package com.example.bler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.content.pm.PackageManager;
import android.icu.lang.UCharacter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
//import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


public class MainActivity extends AppCompatActivity {

    private Button searchButton;
    private Button modeButton;
    private ListView searchList;
    private Button sendButton;
    private TextView sendView;

    private int dbm = -1;
    private WifiManager mywifiManager;
    private WifiInfo mywifiinfo;

    private String message = "";
    private String  mobileNetworkSignal= "";
    private String  type= "";
    private boolean flag = false;
    private boolean BLE_flag = false;
    private boolean BLE = false;
    private boolean mode = true;

    private LineChartView lineChart;
//    private String[] timeline= new String[30] ;//X轴的标注
    private int[] signal= new int[30];//图表的数据点
    private String[] timeline = {"30s前","29","28","27s前","26","25","24","23","22s前","21","20","19","18","19","18","15","14s前","13","12","11","10s前","9","8","7","6s前","5s前","4","3","2","1"};//X轴的标注
//    private int[] signal= {74,22,18,79,20,74,20,74,42,90,74,42,90,50,42,90,33,10,74,22,18,79,20,74,22,18,79,20};//图表的数据
    private List<PointValue> mPointValues = new ArrayList<PointValue>();
    private List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();


    public static UUID UUID_SERVER;
    public static UUID UUID_CHAR_READ;
    public static UUID UUID_CHAR_WRITE;
    public static final String TAG = "BluetoothLeService";
    private BluetoothGatt myBluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback;

    private BluetoothGattService myBluetoothService;
    private BluetoothGattCharacteristic myCharateristic;

    private BluetoothLeScanner myBluetoothLeScanner;
    private boolean scanning;
    private Handler myHandler;
    private myAdapter leDeviceListAdapter;
    private myAdapter adapterlist;
    private ScanCallback leScanCallback;

    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothManager myBluetoothManager;
    private BluetoothDevice selectedDevice;
    private static final long SCAN_PERIOD = 10000;  // Stops scanning after 10 seconds.

    public void getBlePermissionFromSys() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 102;
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    Log.e("perm", "申请权限\t "  );
                    return;
                }
                Log.e("perm", "权限\t "  );
            }
        }
    }

//    public void checkPermissions( Context context){
//        int PERMISSION_ALL = 1;
//        String[] PERMISSIONS = {
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_PRIVILEGED,
//        };
//        if(!hasPermissions(context, PERMISSIONS)){
//            this.requestPermissions( PERMISSIONS, PERMISSION_ALL);
//        }
//    }
//    public boolean hasPermissions(Context context, String... permissions) {
//        if (context != null && permissions != null) {
//            for (String permission : permissions) {
//                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }

    private void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    myBluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            scanning = true;
            myBluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            myBluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private void clearline(){
        for(int i=0;i<30;i++) {
            signal[i] = -127;
        }
        mPointValues.clear();

    }

    public void modechange() {
        clearline();
        if (mode) {
            mode = false;
            modeButton.setText("类型：WiFi");
        } else {
            mode = true;
            modeButton.setText("类型：移动网络");
        }
    }

    public void getDbm() {

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
            @Override
            public void onCellInfo(List<CellInfo> cellInfoList) {
                // DO SOMETHING
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            int ss = 0;
            int counter = 0;
            int dbms = 0;
            if (null != cellInfoList)
            {
                for (CellInfo cellInfo : cellInfoList)
                {
                    if (cellInfo instanceof CellInfoGsm)
                    {
                        CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm)cellInfo).getCellSignalStrength();
                        ss = cellSignalStrengthGsm.getDbm();
                        type="Gsm";
                        //Log.e("66666", "cellSignalStrengthGsm" + cellSignalStrengthGsm.toString());
                        Log.e("66666", "gsm dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoCdma)
                    {
                        CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma)cellInfo).getCellSignalStrength();
                        ss = cellSignalStrengthCdma.getDbm();
                        type="Cdma";
                        //Log.e("66666", "cellSignalStrengthCdma" + cellSignalStrengthCdma.toString() );
                        Log.e("66666", "cdma dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoWcdma)
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                        {
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma)cellInfo).getCellSignalStrength();
                            ss = cellSignalStrengthWcdma.getDbm();
                            type="Wcdma";
                            //Log.e("66666", "cellSignalStrengthWcdma" + cellSignalStrengthWcdma.toString() );
                            Log.e("66666", "wcdma dbm\t " + ss );
                        }
                    }
                    else if (cellInfo instanceof CellInfoLte)
                    {
                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)cellInfo).getCellSignalStrength();
                        ss=cellSignalStrengthLte.getRssi();
                        type="Lte";
//                        Log.e("66666", "cellSignalStrengthLte.getAsuLevel()\t" + cellSignalStrengthLte.getAsuLevel() );
//                        Log.e("66666", "cellSignalStrengthLte.getCqi()\t" + cellSignalStrengthLte.getCqi() );
//                        Log.e("66666", "cellSignalStrengthLte.getDbm()\t " + cellSignalStrengthLte.getDbm() );
//                        Log.e("66666", "cellSignalStrengthLte.getLevel()\t " + cellSignalStrengthLte.getLevel() );
//                        Log.e("66666", "cellSignalStrengthLte.getRsrp()\t " + cellSignalStrengthLte.getRsrp() );
//                        Log.e("66666", "cellSignalStrengthLte.getRssi()\t " + cellSignalStrengthLte.getRssi() );
//                        Log.e("66666", "cellSignalStrengthLte.getRssnr()\t " + cellSignalStrengthLte.getRssnr() );
//                        Log.e("66666", "cellSignalStrengthLte.getTimingAdvance()\t " + cellSignalStrengthLte.getTimingAdvance() );
//                        Log.e("66666", "LTE dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoNr)
                    {
                        CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr)cellInfo).getCellSignalStrength();
                        ss=cellSignalStrengthNr.getCsiRsrp();
                        type="Nr";

//                        Log.e("66666", "cellSignalStrengthNr.getAsuLevel()\t" + cellSignalStrengthNr.getAsuLevel() );
//                        Log.e("66666", "cellSignalStrengthNr.getDbm()\t " + cellSignalStrengthNr.getDbm() );
//                        Log.e("66666", "cellSignalStrengthNr.getLevel()\t " + cellSignalStrengthNr.getLevel() );
//                        Log.e("66666", "cellSignalStrengthNr.getcsiRsrp()\t " + cellSignalStrengthNr.getCsiRsrp() );
//                        Log.e("66666", "cellSignalStrengthNr.getCsiRssi()\t " + cellSignalStrengthNr.getCsiRsrq());
//                        Log.e("66666", "cellSignalStrengthNr.getSsRsrp()\t " + cellSignalStrengthNr.getSsRsrp() );
//                        Log.e("66666", "cellSignalStrengthNr.getSsRsrq()\t " + cellSignalStrengthNr.getSsRsrq() );
//                        Log.e("66666", "NR dbm\t " + ss );
                    }
                    if(ss>-128 && ss<0) {
                        dbms = dbms + ss;
                        counter++;
                    }
                }
            }
//            Log.e("66666", "size\t " + counter );
            if(counter!=0)
                dbm = dbms/counter;
        }
//             Log.e("66666", "last dbm\t " + dbm );
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        tm.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);

//        Timer timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//                tm.requestCellInfoUpdate(minThreadExecutor, new TelephonyManager.CellInfoCallback() {
//                    @Override
//                    public void onCellInfo(@NonNull List<CellInfo> list) {
//                        //Extract needed data
//                    }
//                });
//            }
//        }, 1000, 1000);

    }

    public void getMobileDbm() {

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

//        List<CellSignalStrengthLte> strengthNrList = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
//            strengthNrList = tm.getSignalStrength().getCellSignalStrengths(CellSignalStrengthLte.class);
//        }
//        Log.e("123","strengthNrList" + strengthNrList);

        int ss = 0;
        int counter = 0;
        int dbms = 0;
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        //List<Integer> dbms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            if (null != cellInfoList)
            {
                for (CellInfo cellInfo : cellInfoList)
                {
                    if (cellInfo instanceof CellInfoGsm)
                    {
                        CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm)cellInfo).getCellSignalStrength();
                        ss = cellSignalStrengthGsm.getDbm();
                        //Log.e("66666", "cellSignalStrengthGsm" + cellSignalStrengthGsm.toString());
                        Log.e("66666", "gsm dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoCdma)
                    {
                        CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma)cellInfo).getCellSignalStrength();
                        ss = cellSignalStrengthCdma.getDbm();
                        //Log.e("66666", "cellSignalStrengthCdma" + cellSignalStrengthCdma.toString() );
                        Log.e("66666", "cdma dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoWcdma)
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                        {
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma)cellInfo).getCellSignalStrength();
                            ss = cellSignalStrengthWcdma.getDbm();
//                            Log.e("66666", "cellSignalStrengthWcdma" + cellSignalStrengthWcdma.toString() );
                            Log.e("66666", "wcdma dbm\t " + ss );
                        }
                    }
                    else if (cellInfo instanceof CellInfoLte)
                    {
                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)cellInfo).getCellSignalStrength();
                        ss=cellSignalStrengthLte.getRssi();

//                        Log.e("66666", "cellSignalStrengthLte.getAsuLevel()\t" + cellSignalStrengthLte.getAsuLevel() );
//                        Log.e("66666", "cellSignalStrengthLte.getCqi()\t" + cellSignalStrengthLte.getCqi() );
//                        Log.e("66666", "cellSignalStrengthLte.getDbm()\t " + cellSignalStrengthLte.getDbm() );
//                        Log.e("66666", "cellSignalStrengthLte.getLevel()\t " + cellSignalStrengthLte.getLevel() );
//                        Log.e("66666", "cellSignalStrengthLte.getRsrp()\t " + cellSignalStrengthLte.getRsrp() );
//                        Log.e("66666", "cellSignalStrengthLte.getRssi()\t " + cellSignalStrengthLte.getRssi() );
//                        Log.e("66666", "cellSignalStrengthLte.getRssnr()\t " + cellSignalStrengthLte.getRssnr() );
//                        Log.e("66666", "cellSignalStrengthLte.getTimingAdvance()\t " + cellSignalStrengthLte.getTimingAdvance() );
//                        Log.e("66666", "LTE dbm\t " + ss );
                    }
                    else if (cellInfo instanceof CellInfoNr)
                    {
                        CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr)cellInfo).getCellSignalStrength();
                        ss=cellSignalStrengthNr.getCsiRsrp();

//                        Log.e("66666", "cellSignalStrengthNr.getAsuLevel()\t" + cellSignalStrengthNr.getAsuLevel() );
//                        Log.e("66666", "cellSignalStrengthNr.getDbm()\t " + cellSignalStrengthNr.getDbm() );
//                        Log.e("66666", "cellSignalStrengthNr.getLevel()\t " + cellSignalStrengthNr.getLevel() );
//                        Log.e("66666", "cellSignalStrengthNr.getcsiRsrp()\t " + cellSignalStrengthNr.getCsiRsrp() );
//                        Log.e("66666", "cellSignalStrengthNr.getCsiRssi()\t " + cellSignalStrengthNr.getCsiRsrq());
//                        Log.e("66666", "cellSignalStrengthNr.getSsRsrp()\t " + cellSignalStrengthNr.getSsRsrp() );
//                        Log.e("66666", "cellSignalStrengthNr.getSsRsrq()\t " + cellSignalStrengthNr.getSsRsrq() );
//                        Log.e("66666", "NR dbm\t " + ss );
                    }
                    dbms=dbms+ss;
                    counter++;
                }
            }
            //Collections.sort(dbms);
//            Log.e("66666", "size\t " + counter );
            if(counter!=0)
                dbm = dbms/counter;
            //if (dbms.size() > 1) {
            //    dbm=dbms.get/dbms.size();
            //}
            //else
            //    dbm = ss;
        }
//         Log.e("66666", "last dbm\t " + dbm );
    }

    private void showtext() {
        if(!flag&&!BLE_flag) {
            try {
                if (mode) {
                    message = String.valueOf(dbm);
                    if(type == "Nr")
                        sendView.setText("Nr_CsiRsrp:" + message + "dBm" + "\n");
                    else if(type == "Lte")
                        sendView.setText("Lte_Rssi:" + message + "dBm" + "\n");
                    else if (type == "Wcdma")
                        sendView.setText("Wcdma_signal:" + message + "dBm" + "\n");
                    else if (type == "Cdma")
                        sendView.setText("Cdma_signal:" + message + "dBm" + "\n");
                    else if (type == "Gsm")
                        sendView.setText("Gsm_signal:" + message + "dBm" + "\n");
                } else {
                    message = String.valueOf(mywifiinfo.getRssi());
                    sendView.setText("WiFi_RSSI:" + message + "dBm" + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage() {
        if(!BLE){
            Toast.makeText(MainActivity.this, "蓝牙未连接！" , Toast.LENGTH_SHORT).show();
            return;
        }
        flag=!flag;
        BLE_flag=false;
        if(flag) {
            sendButton.setText("停止发送");
        }
        else{
            sendButton.setText("启动发送");
        }
        if (myBluetoothGatt != null ) {
            TimerTask myTimerTask = new TimerTask() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    if (flag) {
                        try {
                            if (mode) {
                                message = String.valueOf(dbm);
                                if(type == "Nr")
                                    sendView.setText("Nr_CsiRsrp:" + message + "dBm" + "\n");
                                else if(type == "Lte")
                                    sendView.setText("Lte_Rssi:" + message + "dBm" + "\n");
                                else if (type == "Wcdma")
                                    sendView.setText("Wcdma:" + message + "dBm" + "\n");
                                else if (type == "Cdma")
                                    sendView.setText("Cdma:" + message + "dBm" + "\n");
                                else if (type == "Gsm")
                                    sendView.setText("Gsm:" + message + "dBm" + "\n");
                            }
                            else {
                                message = String.valueOf(mywifiinfo.getRssi());
                                sendView.setText("WiFi_RSSI:" + message + "dBm" + "\n");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //Log.e(TAG,"send:"+message);
                        myCharateristic.setValue(message);
                        myBluetoothGatt.writeCharacteristic(myCharateristic);
                        myBluetoothGatt.setCharacteristicNotification(myCharateristic, true);
                        //+"linkSpeed:"+String.valueOf(mywifiinfo.getTxLinkSpeedMbps())+"Mbps");
                    }
                }
            };
            Timer myTimer = new Timer();
            myTimer.schedule(myTimerTask, 0, 100);
        }
    }

    void initView() throws UnsupportedEncodingException {

        //search button initial
        searchButton = findViewById(R.id.search_button);
        searchButton.setText("搜索");
        searchButton.setOnClickListener(v -> showSearchList());

        //BLE list initial
        searchList = findViewById(R.id.search_list);
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = leDeviceListAdapter.getDevice(position);
                Toast.makeText(MainActivity.this, "设备名：" + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                myBluetoothGatt = selectedDevice.connectGatt(MainActivity.this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        });

        //send button initial
        sendButton = findViewById(R.id.send_button);
        sendButton.setText("启动发送");
        sendButton.setOnClickListener(v -> sendMessage());

        //text box init
        sendView = findViewById(R.id.send_view);

        // test mode change  init
        modeButton = findViewById(R.id.mode_button);
        modeButton.setText("类型：移动网络");
        modeButton.setOnClickListener(v -> modechange());
    }

    private void initBluetooth() {
        myBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if(!myBluetoothAdapter.isEnabled())
            myBluetoothAdapter.enable();
        myBluetoothLeScanner = myBluetoothAdapter.getBluetoothLeScanner();
        scanning = true;
        myHandler = new Handler();
        leDeviceListAdapter = new myAdapter(MainActivity.this);
        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                leDeviceListAdapter.addDevice(result.getDevice());
                leDeviceListAdapter.notifyDataSetChanged();
            }
        };

        bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e(TAG, "onConnectionStateChange 连接成功");
                    //mytoast.makeText(MainActivity.this, "连接成功连接成功" ,Toast.LENGTH_SHORT).show();
                    //Looper.prepare();
                    //Toast.makeText(MainActivity.this, "连接成功连接成功" ,Toast.LENGTH_SHORT).show();
                    //Looper.loop();
                    gatt.discoverServices();// search service
                    BLE_flag=true;
                    BLE = true;
                    sendView.setText("已连接:"+selectedDevice.getName());
                    Log.e(TAG, "onConnectionStateChange 查找成功");

                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    Log.e(TAG, "onConnectionStateChange 连接中......");
                    BLE_flag=true;
                    BLE = false;
                    sendView.setText(selectedDevice.getName()+"连接中......");

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "onConnectionStateChange 连接断开");
                    BLE_flag=true;
                    BLE = false;
                    sendView.setText(selectedDevice.getName()+"连接断开");
                    myBluetoothGatt.close();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.e(TAG, "onConnectionStateChange 连接断开中......");
                    BLE_flag=true;
                    BLE = false;
                    sendView.setText(selectedDevice.getName()+"连接断开");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if((status == BluetoothGatt.GATT_SUCCESS) && (myBluetoothGatt.getService(UUID_SERVER) != null)) {
                    Log.e(TAG, "成功发现设备");
                    //轮询BluetoothServices
                    List<BluetoothGattService> list = myBluetoothGatt.getServices();
                    for (BluetoothGattService bluetoothGattService : list) {
                        String str = bluetoothGattService.getUuid().toString();
                        Log.e("Service: ", str);
                        List<BluetoothGattCharacteristic> gattCharacteristics = bluetoothGattService.getCharacteristics();
                        //轮询当前BluetoothService有没有uuid
                        for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                            Log.e("onServicesDisc", ": " + characteristic.getUuid());
                            if (UUID_CHAR_WRITE.toString().equals(characteristic.getUuid().toString())) {
                                myBluetoothService = bluetoothGattService;
                                myCharateristic = characteristic;
                                Log.e("Target: ---------------------->", myCharateristic.getUuid().toString());
                            }
                        }
                    }
                }
                else
                    Log.e(TAG, "没有发现设备");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG,"onCharacteristicWrite发送数据成功");
                } else {
                    Log.e(TAG,"onCharacteristicWrite发送数据失败");
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG,"onDescriptorWrite发送数据成功");
                }else{
                    Log.e(TAG,"onDescriptorWrite发送数据失败");
                }
            }
        };
    }

    private void initWiFi_Mobile() {
        TimerTask myTimerTask = new TimerTask() {
            @Override
            public void run() {
                mywifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);//获取wifi服务
                mywifiinfo = mywifiManager.getConnectionInfo();
                getDbm();
                showtext();

                //getMobileDbm();
                //getMobileNetworkSignal();
            }
        };
            Timer myTimer = new Timer();
            myTimer.schedule(myTimerTask, 0, 100);
    }

    private void initUUID() {

        UUID_SERVER = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
        UUID_CHAR_READ = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
        UUID_CHAR_WRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

        /*
        UUID_SERVER = UUID.fromString("6e400001-b5a3-f393-e0A9-e50e24dcca9e");
        UUID_CHAR_WRITE = UUID.fromString("6e400002-b5a3-f393-e0A9-e50e24dcca9e");
        UUID_CHAR_READ = UUID.fromString("6e400003-b5a3-f393-e0A9-e50e24dcca9e");
         */
    }

    private void showSearchList() {
//        scanning = false;
//        myBluetoothLeScanner.stopScan(leScanCallback);
//        leDeviceListAdapter.clear();
//        scanning = true;
//        myBluetoothLeScanner.startScan(leScanCallback);
        scanLeDevice();
        searchList.setAdapter(leDeviceListAdapter);
    }


    //设置X 轴的显示
    private void getAxisXLables() {
        for (int i = 0; i < timeline.length; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(timeline[i]));
        }
    }
    //图表的每个点的显示
    private void getAxisPoints() {
        for (int i = 0; i < signal.length; i++) {
            mPointValues.add(new PointValue(i, signal[i]));
        }
    }
    private void initLineChart() {
        Line line = new Line(mPointValues).setColor(Color.parseColor("#FFCD41"));  //折线的颜色（橙色）
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(false);//曲线是否平滑，即是曲线还是折线
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true);//是否用线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(false);//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setLines(lines);
        //坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(true);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.GRAY);  //设置字体颜色
        //axisX.setName("date");  //表格名称
        axisX.setTextSize(7);//设置字体大小
        axisX.setMaxLabelChars(30); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
        axisX.setValues(mAxisXValues);  //填充X轴的坐标名称
        data.setAxisXBottom(axisX); //x 轴在底部
        //data.setAxisXTop(axisX);  //x 轴在顶部
        axisX.setHasLines(true); //x 轴分割线
        // Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
        Axis axisY = new Axis();  //Y轴
        axisY.setName("");//y轴标注
        axisY.setTextSize(7);//设置字体大小
        data.setAxisYLeft(axisY);  //Y轴设置在左边
        //data.setAxisYRight(axisY);  //y轴设置在右边

        //设置行为属性，支持缩放、滑动以及平移
//        lineChart.setInteractive(true);
//        lineChart.setZoomType(ZoomType.HORIZONTAL);
//        lineChart.setMaxZoom((float) 2);//最大方法比例
//        lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);

        lineChart.setLineChartData(data);
        lineChart.setVisibility(View.VISIBLE);

        Viewport v = new Viewport(lineChart.getMaximumViewport());
        v.bottom = -120;
        v.top = -10;
        lineChart.setMaximumViewport(v);
        v.left = 0;
        v.right = 30;
        lineChart.setCurrentViewport(v);

    }

    private void refresh(){

        TimerTask myTimerTask = new TimerTask() {
            @Override
            public void run() {
                mPointValues.clear();
                for(int label=0;label<29;label++) {
                    signal[label]=signal[label+1];
                }
                if (mode) {
                    if(dbm>-125 && dbm<-10)
                    signal[29]=dbm;
                }
                else{
                    signal[29]=mywifiinfo.getRssi();
                }
                getAxisPoints();//获取坐标点
//                getAxisXLables();//获取x轴的标注
                initLineChart();

            }
        };
        Timer myTimer = new Timer();
        myTimer.schedule(myTimerTask, 10, 1000);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lineChart = (LineChartView)findViewById(R.id.line_chart);
        clearline();
        getAxisXLables();//获取x轴的标注
        getAxisPoints();//获取坐标点
        initLineChart();//初始化
        refresh();

        getBlePermissionFromSys();
        //checkPermissions();

        try {
            initView();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        initBluetooth();
        initUUID();
        initWiFi_Mobile();

        scanLeDevice();

    }
}