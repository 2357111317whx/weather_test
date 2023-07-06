package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChildcityActivity extends AppCompatActivity {

    private TextView weatherDataTextView;
    private Button shoucang;
    private String citycode;
    private String cityname;
    private static final String API_BASE_URL = "http://t.weather.sojson.com/api/weather/city/";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.childcity);
        ListView listView = findViewById(R.id.listView2);
        Button soucang=findViewById(R.id.shoucang);
        weatherDataTextView = findViewById(R.id.weather_data_textview2);
        Intent intent = getIntent();

        // 检查 Intent 对象是否存在并且包含指定的额外数据

            // 从 Intent 中提取额外的数据
            int cityId = intent.getIntExtra("intent_cityId", -1);

            // 使用获取到的 cityId 值进行后续操作
            // 在这里处理接收到的城市 ID 数据
            // ...

       soucang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToFavorites(cityname,citycode);
            }
        });
        // 创建一个用于显示省市数据的列表
        ArrayList<City> cities = new ArrayList<>();
        try {
            // 从city.json文件中读取数据

            JSONArray cityJsonArray = new JSONArray(loadJSONFromAsset());
            for (int i = 0; i < cityJsonArray.length(); i++) {
                JSONObject cityJsonObject = cityJsonArray.getJSONObject(i);
                int id = cityJsonObject.getInt("id");
                int pid = cityJsonObject.getInt("pid");
                String cityCode = cityJsonObject.optString("city_code");
                String cityName = cityJsonObject.getString("city_name");
                String postCode = cityJsonObject.optString("post_code");
                String areaCode = cityJsonObject.optString("area_code");
                if (pid==cityId){
                    City city = new City(id, pid, cityCode, cityName, postCode, areaCode);
                    cities.add(city);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        CityListAdapter adapter = new CityListAdapter(this, cities);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 获取点击的城市对象
                City selectedCity = cities.get(position);

                String cityCode=selectedCity.getCityCode();
                // 判断点击的是省级还是市级行政单位
                if (selectedCity.getPid() == 0) {
                    // 点击的是省级行政单位，可以根据需要处理
                    //Toast.makeText(ChildcityActivity.this, "点击了省级行政单位：" + selectedCity.getCityName(), Toast.LENGTH_SHORT).show();
                    new ChildcityActivity.FetchWeatherDataTask().execute(cityCode);

                } else {
                    // 点击的是市级行政单位，可以根据需要处理
                   // Toast.makeText(ChildcityActivity.this, "点击了市级行政单位：" + selectedCity.getCityName(), Toast.LENGTH_SHORT).show();

                    // 执行查询天气数据的操作
                    String cityId = selectedCity.getCityCode();
                    new ChildcityActivity.FetchWeatherDataTask().execute(cityCode);
                }
            }
        });
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream inputStream = getAssets().open("city.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    private   class FetchWeatherDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String cityId = params[0];
            String apiUrl = API_BASE_URL + cityId;
            citycode=cityId;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            if (data != null) {
                displayWeatherData(data);

            } else {
                Toast.makeText(ChildcityActivity.this, "获取天气数据失败", Toast.LENGTH_SHORT).show();
            }
        }

        private void saveWeatherDataToCache(String cityId, String data) {
            SharedPreferences sharedPreferences = getSharedPreferences("WeatherCache", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(cityId, data);
            editor.apply();
        }
    }
    private void displayWeatherData(String data) {
        try {
            JSONObject jsonData = new JSONObject(data);

            // Extract the city information from the "cityInfo" object
            JSONObject cityInfoObject = jsonData.optJSONObject("cityInfo");
            String city = "";
            if (cityInfoObject != null) {
                city = cityInfoObject.optString("city");
            }

            // Extract the weather data from the "data" object
            JSONObject dataObject = jsonData.optJSONObject("data");
            String date = "";
            String time = "";
            String wendu = "";
            String shidu = "";
            String pm25 = "";
            if (dataObject != null) {
                date = dataObject.optString("date");
                time = dataObject.optString("time");
                wendu = dataObject.optString("wendu");
                shidu = dataObject.optString("shidu");
                pm25 = dataObject.optString("pm25");
            }
            SharedPreferences sharedPreferences = getSharedPreferences("WeatherCache", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("city", city);
            cityname=city;
            editor.putString("date", jsonData.optString("date"));
            editor.putString("time", jsonData.optString("time"));
            editor.putString("wendu", wendu);
            editor.putString("shidu", shidu);
            editor.putString("pm25", pm25);
            editor.apply();

            // Construct the weather information string and display it in the TextView
            String cityText = "市：" + city + "\n";
            String dateText = "日期：" + jsonData.optString("date") + "\n";

            String temperatureText = wendu + "°C\n";
            String humidityText = "湿度：" + shidu + "\n";
            String pm25Text = "PM2.5：" + pm25;
            String updateTimeText = "数据更新时间：" + jsonData.optString("time") + "\n";

            SpannableStringBuilder weatherInfoBuilder = new SpannableStringBuilder();
            weatherInfoBuilder.append(cityText);
            weatherInfoBuilder.append(dateText);
            //weatherInfoBuilder.append(updateTimeText);


// 设置温度样式
            SpannableString temperatureSpan = new SpannableString(temperatureText);
            temperatureSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, temperatureText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            temperatureSpan.setSpan(new android.text.style.RelativeSizeSpan(2.6f), 0, temperatureText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            temperatureSpan.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK), 0, temperatureText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            temperatureSpan.setSpan(new android.text.style.AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, temperatureText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            weatherInfoBuilder.append(temperatureSpan);

// 设置湿度样式
            SpannableString humiditySpan = new SpannableString(humidityText);
            humiditySpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, humidityText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            humiditySpan.setSpan(new android.text.style.RelativeSizeSpan(1.2f), 0, humidityText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            weatherInfoBuilder.append(humiditySpan);

// 设置PM2.5样式
            SpannableString pm25Span = new SpannableString(pm25Text);
            pm25Span.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, pm25Text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pm25Span.setSpan(new android.text.style.RelativeSizeSpan(1.2f), 0, pm25Text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            weatherInfoBuilder.append(pm25Span);

            // 设置数据更新时间样式
            SpannableString updateTimeSpan = new SpannableString(updateTimeText);
            updateTimeSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, updateTimeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateTimeSpan.setSpan(new android.text.style.RelativeSizeSpan(0.8f), 0, updateTimeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateTimeSpan.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK), 0, updateTimeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateTimeSpan.setSpan(new android.text.style.AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, updateTimeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            weatherInfoBuilder.append(updateTimeSpan);

            weatherDataTextView.setText(weatherInfoBuilder);



        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void addToFavorites(String cityName, String cityCode) {
        // 将城市添加到收藏
        SharedPreferences sharedPreferences = getSharedPreferences("Favorites", Context.MODE_PRIVATE);
        Set<String> citySet = sharedPreferences.getStringSet("citySet", new HashSet<>());

        // 使用HashSet保存城市数据，保证唯一性
        HashSet<String> updatedCitySet = new HashSet<>(citySet);

        // 使用城市名和城市代码之间的分隔符进行拼接，例如使用冒号分隔
        String cityEntry = cityName + ":" + cityCode;
        updatedCitySet.add(cityEntry);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("citySet", updatedCitySet);
        editor.apply();

        Toast.makeText(this, "已将城市添加到收藏", Toast.LENGTH_SHORT).show();
    }


}


