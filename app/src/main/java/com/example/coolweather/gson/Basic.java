package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

//用注解来让json字段和java字段之间建立映射关系
public class Basic {

    //将Java对象中的一个字段与JSON数据中的"city"键进行映射，
    // 从而在序列化和反序列化过程中正确地读取和写入对应的数据。
    // 这个注解可以帮助确保在JSON数据和Java对象之间进行正确的转换。
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{

        @SerializedName("loc")
        public String updateTime;
    }




}
