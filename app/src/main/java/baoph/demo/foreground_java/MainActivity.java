package baoph.demo.foreground_java;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText edtdataIntent;
    private Button btnStartService;
    private Button btnStopService;
    private Button btnCheckBLE;
    private Button btnStartBLE;
    private Button btnStopBLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtdataIntent = findViewById(R.id.edt_data_intent);
        btnStartService = findViewById(R.id.btn_start_service);
        btnStopService = findViewById(R.id.btn_stop_service);
        btnCheckBLE = findViewById(R.id.btn_check);
        btnStartBLE = findViewById(R.id.btn_start_ble);
        btnStopBLE = findViewById(R.id.btn_stop_ble);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService();
            }
        });

        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService();
            }
        });
        btnCheckBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBluetooth();
            }
        });
        btnStartBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBLEService();
            }
        });
        btnStopBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopBLEService();
            }
        });
    }

    private void startBLEService(){
        Toast.makeText(this, "Start BLE Service", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, BLEService.class);
        startService(intent);
    }
    private void stopBLEService(){
        Intent intent = new Intent(this, BLEService.class);
        stopService(intent);
    }

    private void checkBluetooth(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bluetooth is supported", Toast.LENGTH_SHORT).show();
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "BLE is supported", Toast.LENGTH_SHORT).show();
        }
    }
    private void startService(){
        Intent intent = new Intent(this, ForegroundService.class);
        intent.putExtra("key_data_intent", edtdataIntent.getText().toString().trim());
        startService(intent);
    }

    private void stopService(){
        Intent intent = new Intent(this, ForegroundService.class);
        stopService(intent);
    }
}