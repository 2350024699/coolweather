package com.example.coolweather;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Basic;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weather_layout;
    private TextView title_city;
    private TextView title_update_time;
    private TextView degree_text;
    private TextView weather_info_text;
    private LinearLayout forecast_layout;
    private TextView aqi_text;
    private TextView pm25_text;
    private TextView comfort_text;
    private TextView car_wash_text;
    private TextView sport_text;
    //图片
    private ImageView bing_pic_img;
    //刷新布局
    public SwipeRefreshLayout swipeRefresh;

    //滑动菜单功能
    public DrawerLayout drawerLayout;
    private Button nav_button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将背景图和状态栏融合在一起
        if(Build.VERSION.SDK_INT >= 21){
            View decor_view = getWindow().getDecorView();
            decor_view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        //初始化各控件
        initView();

        nav_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weather_string = prefs.getString("weather",null);
        final String weather_id;
        if(weather_string != null){
            //有缓存是直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weather_string);
            weather_id = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            weather_id = getIntent().getStringExtra("weather_id");
            Toast.makeText(this, weather_id, Toast.LENGTH_SHORT).show();
            weather_layout.setVisibility(View.INVISIBLE);
            requestWeather(weather_id);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weather_id);
            }
        });

        String bing_pic = prefs.getString("bing_pic",null);
        if(bing_pic != null){
            //Log.e("WeatherActivity", bing_pic);
            //输出图片网址
            Toast.makeText(WeatherActivity.this, bing_pic, Toast.LENGTH_SHORT).show();
            Glide.with(this).load(bing_pic).into(bing_pic_img);
        }else{
            loadBingPic();
        }
    }

    //加载每日一图
    private void loadBingPic() {
        //String request_bing_pic = "http://guolin.tech/api/bing_pic";
        String request_bing_pic = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(request_bing_pic, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bing_pic = response.body().string();
                String image_url=" 1";
                try{
                    JSONObject jsonObject = new JSONObject(bing_pic);
                    JSONArray imagesArray = jsonObject.getJSONArray("images");
                    JSONObject imageObject = imagesArray.getJSONObject(0);
                    image_url = "https://www.bing.com" + imageObject.getString("url");
                    Log.d("Weather",image_url);
                    //Toast.makeText(WeatherActivity.this, image_url, Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    e.printStackTrace();
                }
                //Toast.makeText(WeatherActivity.this, image_url, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",image_url);
                editor.apply();
                String finalImage_url = image_url;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load("https://www.bing.com/th?id=OHR.TeatroColon_ZH-CN5378730986_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp").into(bing_pic_img);
                        Toast.makeText(WeatherActivity.this, "222", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Toast.makeText(WeatherActivity.this, "？？？", Toast.LENGTH_SHORT).show();
            }

        });

    }

    //根据天气id请求城市天气信息
    public void requestWeather(final String weather_id) {

        String weather_url = "http://guolin.tech/api/weather?cityid=" +
                weather_id + "&key=c08747f130b64855b598ba64ae569ce6";
        HttpUtil.sendOkHttpRequest(weather_url, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String response_text = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(response_text);
                //Log.d("GG", response_text.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",response_text);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            //Toast.makeText(WeatherActivity.this, "GG?", Toast.LENGTH_SHORT).show();
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        //结束刷新事件
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String city_name = weather.basic.cityName;
        // 表示获取分割后的字符串数组中第二个部分（索引为 1）的内容
        String update_time = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weather_info = weather.now.more.info;
        title_city.setText(city_name);
        //Toast.makeText(this, city_name, Toast.LENGTH_SHORT).show();
        title_update_time.setText(update_time);
        degree_text.setText(degree);
        weather_info_text.setText(weather_info);
        forecast_layout.removeAllViews();
        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecast_layout,false);
            TextView date_text = view.findViewById(R.id.date_text);
            TextView info_text = view.findViewById(R.id.info_text);
            TextView max_text = view.findViewById(R.id.max_text);
            TextView min_text = view.findViewById(R.id.min_text);
            date_text.setText(forecast.date);
            info_text.setText(forecast.more.info);
            max_text.setText(forecast.temperature.max);
            min_text.setText(forecast.temperature.min);
            forecast_layout.addView(view);
        }
        if(weather.aqi != null){
            aqi_text.setText(weather.aqi.city.aqi);
            pm25_text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
        comfort_text.setText(comfort);
        car_wash_text.setText(carWash);
        sport_text.setText(sport);
        weather_layout.setVisibility(View.VISIBLE);


    }

    private void initView() {
        weather_layout = findViewById(R.id.weather_layout);
        title_city = findViewById(R.id.title_city);
        title_update_time = findViewById(R.id.title_update_time);
        degree_text = findViewById(R.id.degree_text);
        weather_info_text = findViewById(R.id.weather_info_text);
        forecast_layout = findViewById(R.id.forecast_layout);
        aqi_text = findViewById(R.id.aqi_text);
        pm25_text = findViewById(R.id.pm25_text);
        comfort_text = findViewById(R.id.comfort_text);
        car_wash_text = findViewById(R.id.car_wash_text);
        sport_text = findViewById(R.id.sport_text);
        bing_pic_img = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_layout);
        drawerLayout = findViewById(R.id.drawer_layout);
        nav_button = findViewById(R.id.nav_button);

    }
}
