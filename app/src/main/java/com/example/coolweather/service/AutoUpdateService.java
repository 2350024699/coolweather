package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.example.coolweather.WeatherActivity;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int auHour = 8 * 60 * 60 * 1000;//8小时的毫秒数
        Long triggerAtTime = SystemClock.elapsedRealtime() + auHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }

    //更新天气信息
    private void updateWeather() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weather_string = prefs.getString("weather",null);
        if(weather_string != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weather_string);
            String weather_id = weather.basic.weatherId;
            String weather_url = "http://guolin.tech/api/weather?cityid=" +
                    weather_id + "&key=c08747f130b64855b598ba64ae569ce6";
            HttpUtil.sendOkHttpRequest(weather_url, new Callback() {

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String response_text = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(response_text);
                    if(weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",response_text);
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    //更新bing每日一图
    private void updateBingPic() {

        String request_bing_pic = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(request_bing_pic, new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bing_pic = response.body().string();
                String image_url=" ";
                try{
                    JSONObject jsonObject = new JSONObject(bing_pic);
                    JSONArray imagesArray = jsonObject.getJSONArray("images");
                    JSONObject imageObject = imagesArray.getJSONObject(0);
                    image_url = "https://www.bing.com" + imageObject.getString("url");
                }catch (Exception e){
                    e.printStackTrace();
                }
                //Toast.makeText(WeatherActivity.this, image_url, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",image_url);
                editor.apply();

            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

        });

    }

}