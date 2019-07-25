
package com.example.googlemapsapipractice.Model;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Route {

    @SerializedName("lat")
    @Expose
    private String lat;
    @SerializedName("lng")
    @Expose
    private String lng;

    public static List<LatLng> getLatLngList(List<Route> routes){
     List<LatLng> latLngList=new ArrayList<>();
     for(int i=0;i<routes.size();i++){
         latLngList.add(new LatLng(Double.parseDouble(routes.get(i).lat)
                 ,Double.parseDouble(routes.get(i).lng)));
     }

     return latLngList;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

}
