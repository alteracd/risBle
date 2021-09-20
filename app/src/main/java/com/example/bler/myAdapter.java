package com.example.bler;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class myAdapter extends BaseAdapter{

    private Context myContext;
    private LayoutInflater myLayoutInflater;
    private ArrayList<BluetoothDevice> myBluetoothDeviceList;

    public myAdapter(Context context) {
        super();
        this.myContext = context;
        this.myLayoutInflater = LayoutInflater.from(this.myContext);
        this.myBluetoothDeviceList = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        if(!myBluetoothDeviceList.contains(device))
            myBluetoothDeviceList.add(device);
    }

    public void clear() {
        myBluetoothDeviceList.clear();
    }

    public boolean isEmpty() {
        return myBluetoothDeviceList.isEmpty();
    }

    public int size() {
        return myBluetoothDeviceList.size();
    }

    public BluetoothDevice getDevice(int positon) {
        return myBluetoothDeviceList.get(positon);
    }

    @Override
    public int getCount() {
        return myBluetoothDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return myBluetoothDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        myAdapterViewHolder ViewHolder = null;
        if(convertView == null) {
            convertView = myLayoutInflater.inflate(R.layout.my_adapter_item, null);
            ViewHolder = new myAdapterViewHolder();
            ViewHolder.devcieName = convertView.findViewById(R.id.device_name);
            ViewHolder.deviceAddress = convertView.findViewById(R.id.device_address);
            convertView.setTag(ViewHolder);
        }else {
            ViewHolder = (myAdapterViewHolder) convertView.getTag();
        }
        String Name =  "设备名: " + myBluetoothDeviceList.get(position).getName();
        String Address = "MAC: " + myBluetoothDeviceList.get(position).getAddress();
        ViewHolder.devcieName.setText(Name);
        ViewHolder.deviceAddress.setText(Address);
        return convertView;
    }

    static class myAdapterViewHolder {
        public TextView devcieName;
        public TextView deviceAddress;
    }
}