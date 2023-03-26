package com.example.recordingapp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

public class AppListAdapter extends ArrayAdapter<ApplicationInfo> {

    private Context mContext;
    private List<ApplicationInfo> mApps;
    private int mSelectedPosition = -1;

    public AppListAdapter(Context context, List<ApplicationInfo> apps) {
        super(context, R.layout.item_app_layout, apps);
        mContext = context;
        mApps = apps;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_app_layout, parent, false);
        }

        ApplicationInfo app = mApps.get(position);
        TextView appNameTextView = view.findViewById(R.id.app_name);
        appNameTextView.setText(app.loadLabel(mContext.getPackageManager()));

        RadioButton radioButton = view.findViewById(R.id.radio_button);
        radioButton.setChecked(position == mSelectedPosition);
        radioButton.setTag(position);
        radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectedPosition = (Integer) buttonView.getTag();
                    notifyDataSetChanged();
                    ((MainActivity) mContext).enableStartButton(true);
                }
            }
        });

        return view;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

}