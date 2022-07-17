package com.example.bler;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

import android.content.DialogInterface;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


public class MainActivity extends AppCompatActivity {
    /** UI控件  **/
    private Button searchButton;
    private Button modeButton;
    private Button infoButton1;
    private Button infoButton2;
    private Button infoButton4;
    private Button infoButton3;
    private Button freq_set;
    private ListView searchList;
    private Button sendButton;
    private TextView sendView;
    private EditText freq_edit;
    private AlertDialog BLEdialog; //单选框

    /** 信号强度 **/
    private double dbm1 = -1;
    private double dbm2 = -1;
    private double dbm3 = -1;
    private double dbm4 = -1;
    private int PCI = 0;
    private String frqcn ="";
    private WifiManager mywifiManager;
    private WifiInfo mywifiinfo;
    private String type = ""; //流量类型

    /** 蓝牙发送相关 **/
    private String message = "";//发送信息
    private boolean flag = false; //发送
    private boolean BLE = false; //BLE连接
    private String BLEstateinfo = "";
    private boolean mode = true; //网络类型 true 流量
    private int charttype = 1; //图线type
    private int freq = 100; //发送间隔
    private TimerTask BLEsendTimerTask; //ble发送定时器任务
    private Timer myBLETimer = new Timer();//ble发送定时器

    /**曲线相关**/
    private LineChartView lineChart;
    private double[] signal = new double[30];//图表的数据点
    private double[] signal1 = new double[30];//图表的数据点
    private double[] signal2 = new double[30];//图表的数据点
    private double[] signal3 = new double[30];//图表的数据点
    private double[] signal4 = new double[30];//图表的数据点
    private double[] signalwifi = new double[30];//图表的数据点
    private String[] timeline = {"30s前", "29s前", "28s前", "27s前", "26s前", "25", "24s前", "23s前",
            "22s前", "21s前", "20s前", "19s前", "18", "19", "18", "15s前", "14s前", "13s前", "12s前",
            "11s前", "10s前", "9s前", "8", "7", "6s前", "5s前", "4s前", "3s前", "2", "1"};//X轴的标注
    private List<PointValue> mPointValues = new ArrayList<PointValue>();
    private List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();

    /** 蓝牙相关 **/
    public static UUID UUID_SERVER;
    public static UUID UUID_CHAR_READ;
    public static UUID UUID_CHAR_WRITE;
    public static final String TAG = "BluetoothLeService";//调试tag
    private BluetoothGatt myBluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback;
    private BluetoothGattService myBluetoothService;
    private BluetoothGattCharacteristic myCharateristic;
    private BluetoothLeScanner myBluetoothLeScanner;
    private boolean scanning;
    private Handler myHandler;
    private myBLEAdapter ListBLEAdapter;
    private ScanCallback leScanCallback;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothManager myBluetoothManager;
    private BluetoothDevice selectedDevice;
    private static final long SCAN_PERIOD = 10000;  // Stops scanning after 10 seconds.
    private ArrayList<String> BLEDeviceList;
    private int dialogflag=0;

    private int selectedCellPCI=0;
    private ArrayList<String> cellList;
    private myAdapter ListAdapter;

    /** 蓝牙权限检查与获取 **/
    public void getBlePermissionFromSys() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 102;
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};//验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {//申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    Log.e("perm", "申请权限\t ");
                    return;
                }
                Log.e("perm", "权限\t ");
            }
        }
    }

    /** 蓝牙扫描 **/
    private void scanLeDevice() {
        if (!scanning) {// Stops scanning after a predefined scan period.
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

    /** 网络类型切换 **/
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

    /** 获取信号强度 **/
    public void getDbm() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
            @Override
            public void onCellInfo(List<CellInfo> cellInfoList) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    int ss1 = 0;
                    int ss2 = 0;
                    int ss3 = 0;
                    int ss4 = 0;

                    int counter = 0;
                    double dbm1s = 0;
                    double dbm2s = 0;
                    double dbm3s = 0;
                    double dbm4s = 0;

                    double Nrcounter = 0;
                    double Nrss1 = 0;
                    double Nrss2 = 0;
                    double Nrss3 = 0;
                    double Nrss4 = 0;
                    String cid = "";
                    String mNrArfcn = "";

                    int Ltecounter = 0;
                    double Ltess1 = 0;
                    double Ltess2 = 0;
                    double Ltess3 = 0;
                    double Ltess4 = 0;
                    String fcn = "";
                    int nowPCI=0;


                    if (null != cellInfoList) {
                        cellList.clear();
                        for (CellInfo cellInfo : cellInfoList) {
                            //Log.e("66666", "cellinfo\t " + cellInfo );
                            if (cellInfo instanceof CellInfoGsm) {
                                CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
                                ss1 = cellSignalStrengthGsm.getDbm();
                                type = "Gsm";
                                if (ss1 > -128 && ss1 < 0) {
                                    dbm1s = dbm1s + ss1;
                                    counter++;
                                }
                                //Log.e("66666", "cellSignalStrengthGsm" + cellSignalStrengthGsm.toString());
                                Log.e("66666", "gsm dbm\t " + ss1);
                            } else if (cellInfo instanceof CellInfoCdma) {
                                CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
                                ss1 = cellSignalStrengthCdma.getDbm();
                                type = "Cdma";
                                if (ss1 > -128 && ss1 < 0) {
                                    dbm1s = dbm1s + ss1;
                                    counter++;
                                }
                                //Log.e("66666", "cellSignalStrengthCdma" + cellSignalStrengthCdma.toString() );
                                Log.e("66666", "cdma dbm\t " + ss1);
                            } else if (cellInfo instanceof CellInfoWcdma) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                                    ss1 = cellSignalStrengthWcdma.getDbm();
                                    //Log.e("66666", "cellSignalStrengthWcdma" + cellSignalStrengthWcdma.toString() );
                                    //Log.e("66666", "wcdma dbm\t " + ss1 );
                                    type = "Wcdma";
                                    if (ss1 > -128 && ss1 < 0) {
                                        dbm1s = dbm1s + ss1;
                                        counter++;
                                    }
                                    Log.e("66666", "WCDMA dbm\t " + ss1);
                                }
                            } else if (cellInfo instanceof CellInfoLte) {/** 4G **/
                                type = "Lte";
                                CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
                                ss1 = cellSignalStrengthLte.getRssi();
                                ss3 = cellSignalStrengthLte.getRsrp();
                                ss2 = cellSignalStrengthLte.getRsrq();
                                ss4 = cellSignalStrengthLte.getRssnr();
                                nowPCI = ((CellInfoLte) cellInfo).getCellIdentity().getPci();
                                //Log.e("66666", "4Gcellinfo\t " + cellInfo );
                                fcn = String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getEarfcn());

                                /** 去除重复 **/
                                for(int i=0; i<cellList.size();i++) {
                                    String[] mycell_string = cellList.get(i).split("\\+");
                                    //Log.e("66666", "cell :\t " + mycell_string[0]+" "+cellList.size());
                                    if ( nowPCI == Integer.parseInt(mycell_string[0])){
                                        cellList.remove(i);
                                    }
                                }
                                cellList.add(nowPCI+"+"+ss1+"+"+fcn);

                                /** 筛选选定的PCI  **/
                                if (ss1 > -150 && ss1 < 0) {
                                    if (selectedCellPCI == nowPCI) {
                                        Ltess1 = ss1;
                                        if (ss2 > -150 && ss2 < 0) Ltess2 = ss2;
                                        else Ltess2 = 0;
                                        if (ss3 > -150 && ss3 < 0) Ltess3 = ss3;
                                        else Ltess3 = 0;
                                        if (ss4 > 0 && ss4 < 150) Ltess4 = ss4;
                                        else Ltess4 = 0;
                                        Log.e("66666", "Lteinfo :\t " + cellSignalStrengthLte);
                                        //Log.e("66666", "Lteinfo :\t " + ((CellInfoLte) cellInfo).getCellIdentity());
                                        //Log.e("66666", "4GRsrp\t " + nowPCI +" "+ ss3);
                                    }
                                }
                                Ltecounter++;
                            } else if (cellInfo instanceof CellInfoNr) { /** 5G **/
                                type = "Nr";

                                CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr) cellInfo).getCellSignalStrength();
                                ss1 = -1 * cellSignalStrengthNr.getSsRsrp();
                                ss2 = -1 * cellSignalStrengthNr.getSsRsrq();
                                ss3 = cellSignalStrengthNr.getCsiRsrp();
                                ss4 = cellSignalStrengthNr.getSsSinr();
                                Log.e("66666", "5Gcellinfo\t " + cellInfo );
                                //PCI = (CellIdentityNr)((CellInfoNr)cellInfo).getCellIdentity().getNci();
                                cid = ((CellInfoNr) cellInfo).getCellIdentity().toString().substring(24, 27);
                                cid = cid.replaceAll(" ", "");
                                cid = cid.replaceAll("[a-zA-Z]","");
                                nowPCI = Integer.parseInt(cid);
                                //Log.e("66666", "cid\t " + cid);

                                mNrArfcn = ((CellInfoNr) cellInfo).getCellIdentity().toString().substring(45, 58);
                                mNrArfcn = mNrArfcn.replaceAll(" ","");
                                mNrArfcn = mNrArfcn.replaceAll("=","");
                                mNrArfcn = mNrArfcn.replaceAll("[a-zA-Z]","");
                                //Log.e("66666", "mNrArfcn\t " + mNrArfcn );

                                //Log.e("66666", "Nrinfo :\t " + cellSignalStrengthNr);
                                //Log.e("66666", "Nrinfo :\t " + ((CellInfoNr) cellInfo).getCellIdentity().toString());
                                //                        Log.e("66666", "CsiRsrp:\t " + cellSignalStrengthNr.getCsiRsrp());
                                //                        Log.e("66666", "CsiRsrq:\t " + cellSignalStrengthNr.getCsiRsrq());
                                //Log.e("66666", "Ss2 SsRsrq:\t " + ss2);
                                //Log.e("66666", "ss1 SsRsrp:\t " + ss1);
                                //                        Log.e("66666", "SsSinr:\t " + cellSignalStrengthNr.getSsSinr());
                                //                        Toast.makeText(MainActivity.this, "Nr5g"+ ss1, Toast.LENGTH_SHORT).show();
                                /** 去除重复 **/
                                for(int i=0; i<cellList.size();i++) {
                                    String[] mycell_string = cellList.get(i).split("\\+");
                                    //Log.e("66666", "cell :\t " + mycell_string[0]+" "+cellList.size());
                                    if ( nowPCI == Integer.parseInt(mycell_string[0])){
                                        cellList.remove(i);
                                    }
                                }
                                cellList.add(nowPCI+"+"+ss1+"+"+mNrArfcn);

                                /** 筛选选定的PCI  **/
                                if (ss1 > -150 && ss1 < 0) {
                                    if (selectedCellPCI == nowPCI) {
                                        Nrss1 = ss1;
                                        if (ss2 > -150 && ss2 < 0) Nrss2 = ss2;
                                        else Nrss2 = 0;
                                        if (ss3 > -150 && ss3 < 0) Nrss3 = ss3;
                                        else Nrss3 = 0;
                                        if (ss4 > 0 && ss4 < 150) Nrss4 = ss4;
                                        else Nrss4 = 0;
                                        //Log.e("66666", "5Ginfo :\t " + cellSignalStrengthNr);
                                        //Log.e("66666", "Lteinfo :\t " + ((CellInfoLte) cellInfo).getCellIdentity());
                                        //Log.e("66666", "5GSsRsrp\t " + nowPCI +" "+ ss1);
                                    }
                                }
                                Nrcounter++;
                            }
                        }
                    }
                    //Log.e("66666", "Nrcounter \t " + Nrcounter );
                    //Log.e("66666", "Ltecounter \t " + Ltecounter );
                    if (Nrcounter != 0 && type == "Nr") {
                        dbm1 = Nrss1;
                        dbm2 = Nrss2;
                        dbm3 = Nrss3;
                        dbm4 = Nrss4;
                        PCI = selectedCellPCI;
                        frqcn = mNrArfcn;
                    } else if (Ltecounter != 0 && type == "Lte") {
                        dbm1 = Ltess1;
                        dbm2 = Ltess2;
                        dbm3 = Ltess3;
                        dbm4 = Ltess4;
                        PCI = selectedCellPCI;
                        frqcn = fcn;
                    } else if (counter != 0) {
                        dbm1 = dbm1s / counter;
                        dbm2 = dbm2s / counter;
                        dbm3 = dbm3s / counter;
                        dbm4 = dbm4s / counter;
                    }
                    //Log.e("66666", "\t " + PCI +" "+ dbm3);
                }

                ListAdapter.clear();
                for(String cell : cellList){
                    ListAdapter.addCell(cell);
                    //Log.e("66666", "cellList :\t " + cell);
                }
                //Log.e("66666", "size :\t " + ListAdapter.size());
                searchList.setAdapter(ListAdapter);

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
    }
    public void getDbm2() {
        //获取TelephonyManager
        TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        int mPhoneRatType = mTelephonyManager.getNetworkType();
        //创建监听
        PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
            private String[] mPara = null;

            @SuppressWarnings("unchecked")
            @Override
            public void onSignalStrengthsChanged(SignalStrength sStrength) {

                if (mPhoneRatType == TelephonyManager.NETWORK_TYPE_NR) {/* 5G状态下获取 */

                    try {
                        Class<?> obj = Class
                                .forName("android.telephony.SignalStrength");
                        Method method = obj
                                .getDeclaredMethod("getCellSignalStrengths");
                        method.setAccessible(true);
                        List<CellSignalStrength> cellListStrengths = (List<CellSignalStrength>) method
                                .invoke(sStrength);
                        if (cellListStrengths != null && cellListStrengths.size() > 0) {
                            for (CellSignalStrength o : cellListStrengths) {
                                if (o instanceof CellSignalStrengthNr) {
                                    CellSignalStrengthNr nr = (CellSignalStrengthNr) o;
                                    Log.e("66666", "5G SsRsrp\t " + nr.getSsRsrp());
                                    Log.e("66666", "5G SsRsqp\t " +nr.getSsRsrq());
                                    Log.e("66666", "5G SsSinr\t " +nr.getSsSinr());
                                }

                            }
                        }

                        // System.out.println(field.getName()+":"+field.get(obj.newInstance()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                /* 4G状态下获取 */
                /*
                if (mPhoneRatType == TelephonyManager.NETWORK_TYPE_LTE) {
                    try {
                        (Integer) sStrength.getClass()
                                .getMethod("getLteRsrp").invoke(sStrength);//LteRsrp
                        (Integer) sStrength.getClass()
                                .getMethod("getLteRsrq").invoke(sStrength);//LteRsrq
                        (Integer) sStrength.getClass()
                                .getMethod("getLteRssnr").invoke(sStrength);//LteRssnr
                        // serverCellInfo.cqi = (Integer) sStrength.getClass()
                        // .getMethod("getLteCqi").invoke(sStrength);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                */
                /* 其他状态下获取 */
                /*
                int mGSMSignal = sStrength.getGsmSignalStrength();
                String[] mPara = sStrength.toString().split(" ");
                Integer.parseInt(mPara[9]);//RSRP
                try {
                    Field f = SignalStrength.class.getDeclaredField("mLteRsrq");
                    Field f2 = SignalStrength.class
                            .getDeclaredField("mLteRssnr");// mLteSignalStrenth
                    f.setAccessible(true);
                    f2.setAccessible(true);

                    Integer.parseInt(f.get(sStrength).toString());//RSRQ

                    int rsinr = Integer.parseInt(f2.get(sStrength).toString());//SINR
                    //适配华为个别手机
                    if (!Build.MODEL.contains("HUAWEI MT7")
                            && !Build.MODEL.contains("HUAWEI P7")
                            && !Build.MODEL.contains("HUAWEI D2")
                            && !Build.MODEL.contains("HUAWEI MT2")) {
                        mLTESINR = rsinr % 10 != 0 ? rsinr / 10
                                : (rsinr / 10) + 1;
                    }
                    int tempSinr = (Integer) sStrength.getClass()
                            .getMethod("getLteRssnr").invoke(sStrength);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                */

            }

        };
        // 开始监听
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

    }

    /** 上方textview显示 **/
    private void showtext() {
        String  textinfo="";
            try {
                if (mode) {
                    if (BLE)
                        BLEstateinfo="蓝牙已连接 ";
                    else
                        BLEstateinfo="蓝牙未连接 ";
                    if(type == "Nr") {
                        if (charttype == 1)
                            textinfo = "  Nr_SsRsrp:" + String.format("%.1f", dbm1) + "dBm" + "\n";
                        else if (charttype == 2)
                            textinfo = "  Nr_SsRsrq:" + String.format("%.1f", dbm2) + "dBm" + "\n";
                        else if (charttype == 3)
                            textinfo = "  Nr_CsiRsrp:" + String.format("%.1f", dbm3) + "dBm" + "\n";
                        else if (charttype == 4)
                            textinfo = "  Nr_SsSinr:" + String.format("%.1f", dbm4) + "dBm" + "\n";
                        sendView.setText(BLEstateinfo + " PCI:" + PCI + textinfo);

                    }
                    else if(type == "Lte") {
                        if (charttype == 1)
                            textinfo = "  Lte_Rssi:" + String.format("%.1f", dbm1) + "dBm" + "\n";
                        else if (charttype == 2)
                            textinfo = "  Lte_Rsrq:" + String.format("%.1f", dbm2) + "dBm" + "\n";
                        else if (charttype == 3)
                            textinfo = "  Lte_Rsrp:" + String.format("%.1f", dbm3) + "dBm" + "\n";
                        else if (charttype == 4)
                            textinfo = "  Lte_Rssnr:" + String.format("%.1f", dbm4) + "dBm" + "\n";
                        sendView.setText(BLEstateinfo + " PCI:" + PCI + textinfo);
                    }
                    else if (type == "Wcdma")
                        sendView.setText(BLEstateinfo +"Wcdma_signal:" + String.format("%.1f", dbm1) + "dBm" + "\n");
                    else if (type == "Cdma")
                        sendView.setText(BLEstateinfo +"Cdma_signal:" + String.format("%.1f", dbm1) + "dBm" + "\n");
                    else if (type == "Gsm")
                        sendView.setText(BLEstateinfo +" Gsm_signal:" + String.format("%.1f", dbm1) + "dBm" + "\n");
                    else{
                        sendView.setText("蓝牙已连接 无信号");
                    }
                } else {
                    message = String.valueOf(mywifiinfo.getRssi());
                    if (BLE) sendView.setText("蓝牙已连接 WiFi_RSSI:" + message + "dBm" + "\n");
                    else sendView.setText("蓝牙未连接 WiFi_RSSI:" + message + "dBm" + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /** 图表上方按钮信息显示 **/
    @SuppressLint("SetTextI18n")
    private void showinfo() {
        MainActivity.this.runOnUiThread(new Runnable() {//只有创建这个view的线程才能触碰到它的views 否则会出错闪退
            public void run() {
                if (mode) {
                    if(type == "Nr") {
                        infoButton1.setText("SsRsrp:\n" + String.format("%.2f",dbm1));
                        infoButton2.setText("SsRsrq:\n" + String.format("%.2f",dbm2));
                        if (dbm3!=0)  infoButton3.setText("CsiRsrp:\n" + String.format("%.2f",dbm3));
                        else infoButton3.setText("");
                        if (dbm4!=0)  infoButton4.setText("SsSinr:\n" + String.format("%.2f",dbm4));
                        else infoButton4.setText("");
                    }
                    else if(type == "Lte") {
                        infoButton1.setText("Rssi:\n" + String.format("%.2f",dbm1));
                        infoButton2.setText("Rsrq:\n" + String.format("%.2f",dbm2));
                        infoButton3.setText("Rsrp:\n" + String.format("%.2f",dbm3));
                        if (dbm4!=0 && dbm4<100 && dbm4>-100)   infoButton4.setText("Rssnr:\n" + String.format("%.2f",dbm4));
                        else infoButton4.setText("");
                    }
                    else if (type == "Wcdma") {
                        infoButton1.setText("");
                        infoButton2.setText("");
                        infoButton3.setText("");
                        infoButton4.setText("");
                    }
                    else if (type == "Cdma") {
                        infoButton1.setText("");
                        infoButton2.setText("");
                        infoButton3.setText("");
                        infoButton4.setText("");
                    }
                    else if (type == "Gsm") {
                        infoButton1.setText("");
                        infoButton2.setText("");
                        infoButton3.setText("");
                        infoButton4.setText("");
                    }
                    else {
                        infoButton1.setText("");
                        infoButton2.setText("");
                        infoButton3.setText("");
                        infoButton4.setText("");
                    }
                } else {
                    infoButton1.setText("Rssi:" + String.valueOf(mywifiinfo.getRssi()));
                    infoButton2.setText("" );
                    infoButton3.setText("");
                    infoButton4.setText("");
                }
            }
        });
    }

    /** 蓝牙信息发送 **/
    private void sendMessage() {
        if(!BLE){
            Toast.makeText(MainActivity.this, "蓝牙未连接！" , Toast.LENGTH_SHORT).show();
            return;
        }
        flag=!flag;
        if(flag) {
            if (myBluetoothGatt != null ) {
                try {
                    blesend();
                    myBLETimer.schedule(BLEsendTimerTask, 0, freq);
                    sendButton.setText("停止发送");
                }catch(Exception ex){
                    ex.printStackTrace();
                    Toast.makeText(MainActivity.this, "发送失败！" , Toast.LENGTH_SHORT).show();
                }
            }

        }
        else{
            try {
                BLEsendTimerTask.cancel();
                myBLETimer.purge();
                sendButton.setText("启动发送");
            }catch(Exception ex){
                ex.printStackTrace();
                Toast.makeText(MainActivity.this, "错误！" , Toast.LENGTH_SHORT).show();
            }
        }

    }
    /** 蓝牙发送任务设定  **/
    private void blesend() {
        BLEsendTimerTask = new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try {
                if (flag) {

                        if (mode) {
//                            number = ("000000"+number).slice(-pos)
                            if (charttype==1)
                                message = "@"+ String.format("%4s",(int)((128+dbm1)*10)).replaceAll(" ", "0")+"#";
                            else if(charttype==2)
                                message = "@"+ String.format("%4s",(int)((20+dbm2)*10)).replaceAll(" ", "0")+"#";
                            else if(charttype==3)
                                message = "@"+ String.format("%4s",(int)((128+dbm3)*10)).replaceAll(" ", "0")+"#";
                            else if(charttype==4)
                                message = "@"+ String.format("%4s",(int)((120+dbm4)*10)).replaceAll(" ", "0")+"#";
//                                Log.e("66666", "message \t " + message );
                        }
                        else {
                            message = "@"+ String.format("%4s",(128+mywifiinfo.getRssi())*(10)).replaceAll(" ", "0")+"#";
                        }


                        myCharateristic.setValue(message);
                        myBluetoothGatt.writeCharacteristic(myCharateristic);
                        myBluetoothGatt.setCharacteristicNotification(myCharateristic, true);
                        //+"linkSpeed:"+String.valueOf(mywifiinfo.getTxLinkSpeedMbps())+"Mbps");
                }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /** 蓝牙发送频率设定 **/
    private void setfreq(){
        try {
            freq= Integer.parseInt(freq_edit.getText().toString());
            Toast.makeText(MainActivity.this, "设定成功" , Toast.LENGTH_SHORT).show();
        }catch(Exception ex){
            ex.printStackTrace();
            Toast.makeText(MainActivity.this, "请输入正确数值" , Toast.LENGTH_SHORT).show();
        }
        if(BLE&&flag) {
            BLEsendTimerTask.cancel();
            myBLETimer.purge();
            flag = false;
            sendButton.setText("启动发送");
        }

    }

    /**  基站PCI列表初始化  **/
    void initView() throws UnsupportedEncodingException {
        //BLE list initial
        ListAdapter = new myAdapter(MainActivity.this);
        cellList = new ArrayList<String>();
        searchList = findViewById(R.id.search_list);
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] cellstring= ListAdapter.getCell(position).split("\\+");
                selectedCellPCI =Integer.parseInt(cellstring[0]);
                Toast.makeText(MainActivity.this, "CellPCI：" + selectedCellPCI, Toast.LENGTH_SHORT).show();
                initialsignals();
            }
        });
    }

    /** UI控件初始化 **/
    void initUI() throws UnsupportedEncodingException{
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int width = dm.widthPixels;
        //Log.e("66666", "wd \t " + width );

        /** search button initial  **/
        searchButton = findViewById(R.id.search_button);
        searchButton.setText("BLE搜索");
        searchButton.setOnClickListener(v -> BLEconnect());
        ViewGroup.LayoutParams BtnPara = searchButton.getLayoutParams();
        BtnPara.width = width/2 - 15;
        searchButton.setLayoutParams(BtnPara);
//        searchButton.setWidth(width/2 - 4);

        /**  send button initial  **/
        sendButton = findViewById(R.id.send_button);
        sendButton.setText("启动发送");
        sendButton.setOnClickListener(v -> sendMessage());
        BtnPara = sendButton.getLayoutParams();
        BtnPara.width = width/2 - 15;
        sendButton.setLayoutParams(BtnPara);
//        sendButton.setWidth(width/2 - 4);

        /** text box init **/
        sendView = findViewById(R.id.send_view);

        /** test mode change  init **/
        modeButton = findViewById(R.id.mode_button);
        modeButton.setText("类型：移动网络");
        modeButton.setOnClickListener(v -> modechange());
        BtnPara = modeButton.getLayoutParams();
        BtnPara.width = width/2 - 15;
        modeButton.setLayoutParams(BtnPara);
//        modeButton.setWidth(width/2 - 4);

        /**  info  init   **/
        infoButton1 = findViewById(R.id.info_button1);
        infoButton1.setText("");
        infoButton1.setOnClickListener(v -> chartchange(1));
        infoButton1.setWidth(width/4 - 5);
        infoButton2 = findViewById(R.id.info_button2);
        infoButton2.setText("");
        infoButton2.setOnClickListener(v -> chartchange(2));
        infoButton2.setWidth(width/4 - 5);
        infoButton3 = findViewById(R.id.info_button3);
        infoButton3.setText("");
        infoButton3.setOnClickListener(v -> chartchange(3));
        infoButton3.setWidth(width/4 - 4);
        infoButton4= findViewById(R.id.info_button4);
        infoButton4.setText("");
        infoButton4.setOnClickListener(v -> chartchange(4));
        infoButton4.setWidth(width/4 - 4);

        /** 发送频率设定按键 **/
        freq_set=findViewById(R.id.freq_button);
        freq_set.setText("设定");
        freq_set.setOnClickListener(v -> setfreq());
        /** 发送频率输入框 **/
        freq_edit=(EditText)findViewById(R.id.edit_freq);
        ViewGroup.LayoutParams setbtnPara = freq_set.getLayoutParams();
        BtnPara = freq_edit.getLayoutParams();
        BtnPara.width = width/2 -40 -setbtnPara.width ;
        freq_edit.setLayoutParams(BtnPara);
//        freq_edit.setWidth(width/4 + 4);

    }

    /** BLE扫描、连接  弹出窗  **/
    public void BLEconnect(){

        scanLeDevice();//扫描BLE设备

        BLEDeviceList = new ArrayList<String>();
        if (dialogflag!=0) {
        Log.e("BLE", "BLEList\t " + ListBLEAdapter.size());
        for(int p=0; p<ListBLEAdapter.size(); p++){
            Log.e("BLE", "BLEList[p]\t " + p);
            BLEDeviceList.add(p,"设备名："+ListBLEAdapter.getDeviceList().get(p).getName()+"\nMAC:"+ ListBLEAdapter.getDeviceList().get(p).getAddress());
        }
        String[] BLEList = (String[])BLEDeviceList.toArray(new String[BLEDeviceList.size()]);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("BLEList");

        alertBuilder.setSingleChoiceItems(BLEList, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Toast.makeText(MainActivity.this, items[i], Toast.LENGTH_SHORT).show();
                selectedDevice = ListBLEAdapter.getDevice(i);
                Toast.makeText(MainActivity.this, "设备名：" + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    myBluetoothGatt = selectedDevice.connectGatt(MainActivity.this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                }catch(Exception exp){
                    exp.printStackTrace();
                    Toast.makeText(MainActivity.this, "连接失败" , Toast.LENGTH_SHORT).show();
                }
                BLEdialog.dismiss();
            }
        });
        alertBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BLEdialog.dismiss();
            }
        });
            BLEdialog = alertBuilder.create();
            BLEdialog.show();
            dialogflag=1;
        }
        dialogflag++;
    }
     /*
    void initView() throws UnsupportedEncodingException {
        //BLE list initial
        searchList = findViewById(R.id.search_list);
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = ListAdapter.getDevice(position);
                Toast.makeText(MainActivity.this, "设备名：" + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                myBluetoothGatt = selectedDevice.connectGatt(MainActivity.this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        });
    }*/

    /** 蓝牙初始化、设置与连接 **/
    private void initBluetooth() {
        myBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if(!myBluetoothAdapter.isEnabled())
            myBluetoothAdapter.enable();
        myBluetoothLeScanner = myBluetoothAdapter.getBluetoothLeScanner();
        scanning = true;
        myHandler = new Handler();//
        ListBLEAdapter = new myBLEAdapter(MainActivity.this);//BLE设备列表
        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ListBLEAdapter.addDevice(result.getDevice());
                ListBLEAdapter.notifyDataSetChanged();
                Log.e(TAG, " \t"+ListBLEAdapter.size());
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
                    BLE = true;
                    sendView.setText("已连接:"+selectedDevice.getName());
                    Log.e(TAG, "onConnectionStateChange 查找成功");

                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    Log.e(TAG, "onConnectionStateChange 连接中......");
                    BLE = false;
                    sendView.setText(selectedDevice.getName()+"连接中......");

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "onConnectionStateChange 连接断开");
                    BLE = false;
                    sendView.setText(selectedDevice.getName()+"连接断开");
                    myBluetoothGatt.close();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.e(TAG, "onConnectionStateChange 连接断开中......");
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

    /** 启动定时器 包括wifi强度、移动网络信号强度、信息显示 **/
    private void initTimer() {
        TimerTask myTimerTask = new TimerTask() {
            @Override
            public void run() {
                mywifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);//获取wifi服务
                mywifiinfo = mywifiManager.getConnectionInfo();
                getDbm();
                showtext();
                showinfo();
            }
        };
            Timer myTimer = new Timer();
            myTimer.schedule(myTimerTask, 0, 100);
    }

    /** BLE蓝牙服务UUID设定 **/
    private void initUUID() {
//        HC08
//        UUID_SERVER = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
//        UUID_CHAR_READ = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
//        UUID_CHAR_WRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

        //ATK BLE01   2B
        UUID_SERVER = UUID.fromString("d973f2e0-b19e-11e2-9e96-080020f29a66");
        UUID_CHAR_READ = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        UUID_CHAR_WRITE = UUID.fromString("d973f2e2-b19e-11e2-9e96-0800200c9a66");
//      UUID_SERVER = UUID.fromString("d973f2e1-b19e-11e2-9e96-9e08000c9a66");

        /*
        UUID_SERVER = UUID.fromString("6e400001-b5a3-f393-e0A9-e50e24dcca9e");
        UUID_CHAR_WRITE = UUID.fromString("6e400002-b5a3-f393-e0A9-e50e24dcca9e");
        UUID_CHAR_READ = UUID.fromString("6e400003-b5a3-f393-e0A9-e50e24dcca9e");
         */
    }

    /** 设置曲线图X轴的显示 **/
    private void getAxisXLables() {
        for (int i = 0; i < timeline.length; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(timeline[i]));
        }
    }
    /**  曲线图的每个点的显示  **/
    private void getAxisPoints() {
        for (int i = 0; i < signal.length; i++) {
            mPointValues.add(new PointValue(i, (int)signal[i]));
        }
    }
    /** 设置并显示曲线图  **/
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
        /**坐标轴**/
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
        /**Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)**/
        Axis axisY = new Axis();  //Y轴
        axisY.setName("");//y轴标注
        axisY.setTextSize(7);//设置字体大小
        data.setAxisYLeft(axisY);  //Y轴设置在左边
        //data.setAxisYRight(axisY);  //y轴设置在右边

        /**设置行为属性，支持缩放、滑动以及平移**/
//        lineChart.setInteractive(true);
//        lineChart.setZoomType(ZoomType.HORIZONTAL);
//        lineChart.setMaxZoom((float) 2);//最大方法比例
//        lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);

        lineChart.setLineChartData(data);
        lineChart.setVisibility(View.VISIBLE);

        Viewport v = new Viewport(lineChart.getMaximumViewport());
        if (charttype==1){
            v.bottom = -130;
            v.top = -30;
        }
        else if(charttype==2){
            v.bottom = -30;
            v.top = 0;
        }
        else if(charttype==3){
            v.bottom = -130;
            v.top = -40;
        }
        else if(charttype==4){
            v.bottom = -20;
            v.top = 40;
        }
        lineChart.setMaximumViewport(v);
        v.left = 0;
        v.right = 30;
        lineChart.setCurrentViewport(v);

    }
    /**  清除曲线图  **/
    private void clearline(){
        for(int i=0;i<30;i++) {
            signal[i] = -130;
        }
        mPointValues.clear();
    }
    /** 初始化曲线图上的信号值 **/
    private void initialsignals(){
        for(int i=0;i<30;i++){
            signal1[i]=-130;
            signal2[i]=-30;
            signal3[i]=-130;
            signal4[i]=-20;
            signalwifi[i]=-130;
        }
    }
    /** 曲线图切换 **/
    private void chartchange(int tp){ charttype = tp; }
    /** 曲线图刷新  **/
    private void refresh(){
        TimerTask myTimerTask = new TimerTask() {
            @Override
            public void run() {
                mPointValues.clear();
                for(int label=0;label<29;label++) {
                    if (type=="Nr"||type=="Lte") {
                        signal1[label] = signal1[label + 1];
                        signal2[label] = signal2[label + 1];
                        signal3[label] = signal3[label + 1];
                        signal4[label] = signal4[label + 1];
                    }
                    signalwifi[label] = signalwifi[label + 1];
                }
                if (type=="Nr"||type=="Lte") {
                    if (dbm4 > -2 && dbm4 < 100)  signal4[29] = dbm4;
                    if (dbm3 > -128 && dbm3 < 0)  signal3[29] = dbm3;
                    if (dbm2 > -128 && dbm2 < 0)  signal2[29] = dbm2;
                    if (dbm1 > -128 && dbm1 < 0)  signal1[29] = dbm1;
                }
                signalwifi[29]=mywifiinfo.getRssi();
                if (mode) {
                    if (type=="Nr"||type=="Lte") {
                        if (charttype == 1) //signal = signal1;
                            System.arraycopy(signal1, 0, signal, 0, signal1.length);
                        else if (charttype == 2) //signal = signal2;
                            System.arraycopy(signal2, 0, signal, 0, signal2.length);
                        else if (charttype == 3) //signal = signal3;
                            System.arraycopy(signal3, 0, signal, 0, signal3.length);
                        else if (charttype == 4) //signal = signal4;
                            System.arraycopy(signal4, 0, signal, 0, signal4.length);
                    }
                }
                else{
//                    signal=signalwifi;
                    System.arraycopy(signalwifi, 0, signal, 0, signalwifi.length);
                }
                getAxisPoints();//获取坐标点
//              getAxisXLables();//获取x轴的标注
                initLineChart();
            }
        };
        Timer myTimer = new Timer();
        myTimer.schedule(myTimerTask, 10, 990);

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getBlePermissionFromSys();//check Permissions;

        /** 曲线图实例初始化  **/
        lineChart = (LineChartView)findViewById(R.id.line_chart);
        getAxisXLables();//获取x轴的标注
//        getAxisPoints();//获取坐标点
//        initLineChart();//初始化
        initialsignals();
        refresh();

        /**    **/
        try {
            initView();
            initUI();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        initTimer();

        /**  BLE蓝牙初始化 **/
        initBluetooth();
        initUUID();
        scanLeDevice(); //蓝牙扫描
        BLEconnect();
    }

}

//    public void getMobileDbm() {
//
//        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//
////        List<CellSignalStrengthLte> strengthNrList = null;
////        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
////            strengthNrList = tm.getSignalStrength().getCellSignalStrengths(CellSignalStrengthLte.class);
////        }
////        Log.e("123","strengthNrList" + strengthNrList);
//
//        int ss = 0;
//        int counter = 0;
//        int dbm1s = 0;
//        List<CellInfo> cellInfoList = tm.getAllCellInfo();
//        //List<Integer> dbms = new ArrayList<>();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
//        {
//            if (null != cellInfoList)
//            {
//                for (CellInfo cellInfo : cellInfoList)
//                {
//                    if (cellInfo instanceof CellInfoGsm)
//                    {
//                        CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm)cellInfo).getCellSignalStrength();
//                        ss = cellSignalStrengthGsm.getDbm();
//                        //Log.e("66666", "cellSignalStrengthGsm" + cellSignalStrengthGsm.toString());
//                        Log.e("66666", "gsm dbm\t " + ss );
//                    }
//                    else if (cellInfo instanceof CellInfoCdma)
//                    {
//                        CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma)cellInfo).getCellSignalStrength();
//                        ss = cellSignalStrengthCdma.getDbm();
//                        //Log.e("66666", "cellSignalStrengthCdma" + cellSignalStrengthCdma.toString() );
//                        Log.e("66666", "cdma dbm\t " + ss );
//                    }
//                    else if (cellInfo instanceof CellInfoWcdma)
//                    {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
//                        {
//                            CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma)cellInfo).getCellSignalStrength();
//                            ss = cellSignalStrengthWcdma.getDbm();
////                            Log.e("66666", "cellSignalStrengthWcdma" + cellSignalStrengthWcdma.toString() );
//                            Log.e("66666", "wcdma dbm\t " + ss );
//                        }
//                    }
//                    else if (cellInfo instanceof CellInfoLte)
//                    {
//                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)cellInfo).getCellSignalStrength();
//                        ss=cellSignalStrengthLte.getRssi();
//
////                        Log.e("66666", "cellSignalStrengthLte.getAsuLevel()\t" + cellSignalStrengthLte.getAsuLevel() );
////                        Log.e("66666", "cellSignalStrengthLte.getCqi()\t" + cellSignalStrengthLte.getCqi() );
////                        Log.e("66666", "cellSignalStrengthLte.getDbm()\t " + cellSignalStrengthLte.getDbm() );
////                        Log.e("66666", "cellSignalStrengthLte.getLevel()\t " + cellSignalStrengthLte.getLevel() );
////                        Log.e("66666", "cellSignalStrengthLte.getRsrp()\t " + cellSignalStrengthLte.getRsrp() );
////                        Log.e("66666", "cellSignalStrengthLte.getRssi()\t " + cellSignalStrengthLte.getRssi() );
////                        Log.e("66666", "cellSignalStrengthLte.getRssnr()\t " + cellSignalStrengthLte.getRssnr() );
////                        Log.e("66666", "cellSignalStrengthLte.getTimingAdvance()\t " + cellSignalStrengthLte.getTimingAdvance() );
////                        Log.e("66666", "LTE dbm\t " + ss );
//                    }
//                    else if (cellInfo instanceof CellInfoNr)
//                    {
//                        CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr)cellInfo).getCellSignalStrength();
//                        ss=cellSignalStrengthNr.getCsiRsrp();
//
////                        Log.e("66666", "cellSignalStrengthNr.getAsuLevel()\t" + cellSignalStrengthNr.getAsuLevel() );
////                        Log.e("66666", "cellSignalStrengthNr.getDbm()\t " + cellSignalStrengthNr.getDbm() );
////                        Log.e("66666", "cellSignalStrengthNr.getLevel()\t " + cellSignalStrengthNr.getLevel() );
////                        Log.e("66666", "cellSignalStrengthNr.getcsiRsrp()\t " + cellSignalStrengthNr.getCsiRsrp() );
////                        Log.e("66666", "cellSignalStrengthNr.getCsiRssi()\t " + cellSignalStrengthNr.getCsiRsrq());
////                        Log.e("66666", "cellSignalStrengthNr.getSsRsrp()\t " + cellSignalStrengthNr.getSsRsrp() );
////                        Log.e("66666", "cellSignalStrengthNr.getSsRsrq()\t " + cellSignalStrengthNr.getSsRsrq() );
////                        Log.e("66666", "NR dbm\t " + ss );
//                    }
//                    dbm1s=dbm1s+ss;
//                    counter++;
//                }
//            }
//            //Collections.sort(dbms);
////            Log.e("66666", "size\t " + counter );
//            if(counter!=0)
//                dbm1 = dbm1s/counter;
//            //if (dbms.size() > 1) {
//            //    dbm=dbms.get/dbms.size();
//            //}
//            //else
//            //    dbm = ss;
//        }
////         Log.e("66666", "last dbm\t " + dbm );
//    }
