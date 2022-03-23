package com.example.bler;

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
    private ArrayList<String> mycells;

    public myAdapter(Context context) {
        super();
        this.myContext = context;
        this.myLayoutInflater = LayoutInflater.from(this.myContext);
        this.mycells = new ArrayList<String>();
    }

    public void addCell(String cellstring) {
        int flag=1;
        for(String cell:mycells) {
            String[] cell_string = cellstring.split("\\+");
            String[] mycell_string = cell.split("\\+");
            if (cell_string[0]==mycell_string[0]){
                flag=0;
                break;
            }
        }
        if(flag==1)
            mycells.add(cellstring);
    }

    public void clear() {
        mycells.clear();
    }

    public boolean isEmpty() {
        return mycells.isEmpty();
    }

    public int size() {
        return mycells.size();
    }

    public String getCell(int positon) {
        return mycells.get(positon);
    }

    @Override
    public int getCount() {
        return mycells.size();
    }

    @Override
    public Object getItem(int position) {
        return mycells.get(position);
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
            ViewHolder.PCI = convertView.findViewById(R.id.cellPCI);
            ViewHolder.celldbm = convertView.findViewById(R.id.celldbm);
            convertView.setTag(ViewHolder);
        }else {
            ViewHolder = (myAdapterViewHolder) convertView.getTag();
        }
        String[] cellstring= mycells.get(position).split("\\+");
        String CellPCI =  "PCI: " + cellstring[0];
        String Celldbm = "dbm: " + cellstring[1];
        ViewHolder.PCI.setText(CellPCI);
        ViewHolder.celldbm.setText(Celldbm);
        return convertView;
    }

    static class myAdapterViewHolder {
        public TextView PCI;
        public TextView celldbm;
    }
}