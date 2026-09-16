package com.example.mobile_embedded_system;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.maplibre.android.MapLibre;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_main);
    }
}
