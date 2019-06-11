package com.example.qlinechart;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.example.qlinechart.view.QLineChart;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mTopYAxisTv;
    private TextView mBottomYAxisTv;
    private GridView mXAxisGv;
    private QLineChart mLineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<QLineChart.Entry> lineData = new ArrayList<>();
        lineData.add(new QLineChart.Entry(0, 0.06f));
        lineData.add(new QLineChart.Entry(1, 0.07f));
        lineData.add(new QLineChart.Entry(2, 0.08f));
        lineData.add(new QLineChart.Entry(3, 0.09f));

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        mTopYAxisTv = findViewById(R.id.tv_top_y_axis);
        mBottomYAxisTv = findViewById(R.id.tv_bottom_y_axis);
        mTopYAxisTv.setText("9.00");
        mBottomYAxisTv.setText("6.88");

        mXAxisGv = findViewById(R.id.gv_x_axis);
        mXAxisGv.setNumColumns(lineData.size());
        mXAxisGv.setAdapter(new XAxisAdapter(lineData));

        mLineChart = findViewById(R.id.view_line_chart);
        mLineChart.setMinAndMaxYAxis(0.06f, 0.09f);
        mLineChart.setLabelCount(lineData.size() - 1);
        mLineChart.setAxisPadding(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13, metrics),
                0,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics));
        mLineChart.setBrokenLineData(lineData);
        mLineChart.setGridDashedLineColor(Color.parseColor("#FFDEDEDE"));
        mLineChart.setGridDashedLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics));
        mLineChart.setBrokenLineColor(Color.parseColor("#FFF0594E"));
        mLineChart.setBrokenLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, metrics));
        mLineChart.setHighlightingColor(Color.parseColor("#FFFDBB74"));
        mLineChart.setHighlightingWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics));
        mLineChart.setHighlightingListener(new QLineChart.OnHighlightingListener() {
            @Override
            public void onValue(float x, float y) {
                Log.e("test", x + ", " + y);
            }
        });
    }

    private static class XAxisAdapter extends BaseAdapter {
        List<QLineChart.Entry> mLineData;

        public XAxisAdapter(List<QLineChart.Entry> lineData) {
            this.mLineData = lineData;
        }

        @Override
        public int getCount() {
            return mLineData == null ? 0 : mLineData.size();
        }

        @Override
        public Object getItem(int position) {
            return mLineData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            TextView itemTv = new TextView(parent.getContext());
            itemTv.setTextColor(Color.parseColor("#FF919191"));
            itemTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
            itemTv.setText("第" + ((int) mLineData.get(position).getX() + 1) + "季");
            return itemTv;
        }
    }
}
