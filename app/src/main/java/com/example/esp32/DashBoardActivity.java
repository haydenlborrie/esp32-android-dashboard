package com.example.esp32;

import static com.example.esp32.Util.Constants.REQUEST_ENABLE_BT;
import static com.example.esp32.Util.Constants.SELECT_DEVICE_REQUEST_CODE;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32.viewModels.DashBoardViewModel;

import java.util.ArrayList;

public class DashBoardActivity extends AppCompatActivity implements MyInterface {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    BluetoothDevice deviceToPair;

    TextView tempPX, tempDht, px, hum, heat;
    DashBoardViewModel dashBoardViewModel;
    private BluetoothService bluetoothService;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Bluetooth paired event
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                bluetoothService.connectBluetooth(deviceToPair);
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "onReceive: " + deviceHardwareAddress);
//                Caution: Performing device discovery consumes a lot of the Bluetooth adapter's resources.
//                After you have found a device to connect to, be certain that you stop discovery with
//                cancelDiscovery() before attempting a connection. Also, you shouldn't perform discovery
//                while connected to a device because the discovery process significantly reduces the
//                bandwidth available for any existing connections.
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tempPX = findViewById(R.id.temp_px);
        px = findViewById(R.id.px);
        hum = findViewById(R.id.humidity);
        tempDht = findViewById(R.id.temp_dht22);
        heat = findViewById(R.id.heat_index);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BluetoothManager.class);
        if (bluetoothManager == null) {
            Toast.makeText(this, "Bluetooth unavailable?", Toast.LENGTH_SHORT).show();
        } else {
            bluetoothService = new BluetoothService(this,
                    bluetoothManager);

            // if we have previously connected to a device try to connect

            // otherwise do a full connection routine

            bluetoothService.startConnectionAttempt();

            dashBoardViewModel = ViewModelProvider
                    .AndroidViewModelFactory
                    .getInstance(getApplication())
                    .create(DashBoardViewModel.class);
            dashBoardViewModel.setX(bluetoothService);
            initialiseObservers();
        }
    }

    private void initialiseObservers() {
        dashBoardViewModel.getTempDht().observe(this, tempDht -> tempPX.setText(tempDht));
        dashBoardViewModel.getHumidityDht().observe(this, humidity -> hum.setText(humidity));
        dashBoardViewModel.getTempBarometer().observe(this, temp -> tempDht.setText(temp));
        dashBoardViewModel.getPxBarometer().observe(this, px -> this.px.setText(px));
        dashBoardViewModel.getHeatIndexDht().observe(this, heatIndexC -> heat.setText(heatIndexC));
    }

    private String[] getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) ==
                PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        String[] requiredPermissionsArray = new String[permissions.size()];
        if (!permissions.isEmpty()) {
            for (int i = 0; i < permissions.size(); i++) {
                requiredPermissionsArray[i] = permissions.get(i);
            }
        }
        return requiredPermissionsArray;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(this, "User enabled bluetooth", Toast.LENGTH_SHORT).show();
            }
        }

        // after a user has selected a device to connect to
        else if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

            if (deviceToPair != null) {
                boolean notPaired = deviceToPair.createBond();

                if (notPaired) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(receiver, filter);
                } else
                    bluetoothService.connectBluetooth(deviceToPair);
            }
        } else {
            Toast.makeText(this, "fell through ...", Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    @Override
    public void pairingRequest() {
        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                // Find only devices that match this request filter.
                .addDeviceFilter(new BluetoothDeviceFilter.Builder().build())
                // Stop scanning as soon as one device matching the filter is found.
                // .setSingleDevice(true)
                .build();

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {
            // Called when a device is found. Launch the IntentSender so the user can
            // select the device they want to pair with.
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                try {
                    startIntentSenderForResult(chooserLauncher, SELECT_DEVICE_REQUEST_CODE,
                            null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("MainActivity", "Failed to send intent");
                }
            }

            @Override
            public void onFailure(CharSequence error) {
                // Handle the failure.
            }
        }, null);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    public void notifySuccessfulConnection() {
        Toast.makeText(this, "Connection successful!", Toast.LENGTH_SHORT).show();
    }
}