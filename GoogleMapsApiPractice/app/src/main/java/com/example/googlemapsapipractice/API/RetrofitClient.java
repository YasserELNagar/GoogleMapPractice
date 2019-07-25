package com.example.googlemapsapipractice.API;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static Retrofit getInstance(){

        // setting custom timeouts
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.connectTimeout(160, TimeUnit.SECONDS);
        client.readTimeout(160, TimeUnit.SECONDS);
        client.writeTimeout(160, TimeUnit.SECONDS);

        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl("https://maps.googleapis.com/maps/api/directions/").client(client.build()).addConverterFactory(GsonConverterFactory.create());

        return builder.build();
    }
}
