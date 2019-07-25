package com.example.googlemapsapipractice.API;


import com.example.googlemapsapipractice.Model.GoogleMapsDirectionReqsponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface RetrofitApi {

    @POST
    Call<GoogleMapsDirectionReqsponse>getMapRoute(@Url String url);

}