package com.example.googlemapsapipractice;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    private void init(){

        Intent intent=new Intent(this,MapActivity.class);
        startActivity(intent);
    }


    private boolean isGoogleServiceAvailable(){
        int available= GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if(available==ConnectionResult.SUCCESS){
            Log.v(TAG,"isGoogleServiceAvailable: Google Play Service Working" );
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.v(TAG,"isGoogleServiceAvailable: Google Play Service has an error but can be resolved" );
            Dialog dialog=GoogleApiAvailability.getInstance().getErrorDialog(this,available,ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else {
            Log.v(TAG,"isGoogleServiceAvailable: Google Play Service has an error" );
            Toast.makeText(this, "Google Play Service has an error", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @OnClick(R.id.bt_map) void OpenMap(){

        if(isGoogleServiceAvailable()==true){
            init();
        }
    }

}
