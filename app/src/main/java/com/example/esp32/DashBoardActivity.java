package com.example.esp32;

import static com.example.esp32.Util.Constants.REQUEST_ENABLE_BT;
import static com.example.esp32.Util.Constants.SELECT_DEVICE_REQUEST_CODE;

import android.bluetooth.BluetoothAdapter;
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32.databinding.DashboardActivityBinding;
import com.example.esp32.viewModels.DashBoardViewModel;

import java.util.Optional;
import java.util.Set;

public class DashBoardActivity extends AppCompatActivity implements MyInterface {
    private static final String TAG = "DashBoardActivity";

    private boolean connected;
    private String previousConnectionMacAdd;
    private BluetoothDevice deviceToPair;

    private BluetoothService bluetoothService;
    private DashBoardViewModel dashBoardViewModel;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                if (!connected) {
                    // Bluetooth paired event
                    bluetoothService.connectBluetooth(deviceToPair);
                    connected = true;
                } else {
                    connected = false;
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice deviceFound = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Store the mac address of the foundDevice
                String deviceHardwareAddress = deviceFound.getAddress(); // MAC address

//                Caution: Performing device discovery consumes a lot of the Bluetooth adapter's resources.
//                After you have found a device to connect to, be certain that you stop discovery with
//                cancelDiscovery() before attempting a connection. Also, you shouldn't perform discovery
//                while connected to a device because the discovery process significantly reduces the
//                bandwidth available for any existing connections.
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //     // Get the BluetoothDevice object from the Intent
                //
                //     // Check if the connected device is one we had comm with
                //     if(device.getAddress().equals(partnerDevAdd)==true)
                //         isConnected=true;
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //     // Get the BluetoothDevice object from the Intent
                //
                //     // Check if the connected device is one we had comm with
                //     if(device.getAddress().equals(partnerDevAdd)==true)
                //         isConnected=false;
            }
        }
    };
    private DashboardActivityBinding binding;
    private boolean receiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DashboardActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences sharedPreferences = getSharedPreferences("ble", Context.MODE_PRIVATE);
        if (sharedPreferences.contains("last_device_mac")) {
            previousConnectionMacAdd = sharedPreferences.getString("last_mac", "");
        }

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothService = new BluetoothService(this, bluetoothAdapter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> i = bluetoothAdapter.getBondedDevices();
        Optional<BluetoothDevice> match = i.stream().filter(bluetoothDevice ->
                bluetoothDevice.getAddress().compareTo("7C:9E:BD:60:61:CE") == 0).findFirst();

        if (match.isPresent()) {
            BluetoothDevice device = match.get();

            boolean notPaired = device.createBond();

            if (notPaired) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(receiver, filter);
                receiverRegistered = true;
            } else {
                bluetoothService.connectBluetooth(device);
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                // filter.addAction(BluetoothDevice.);
                registerReceiver(receiver, filter);
                receiverRegistered = true;
            }
        } else {
            bluetoothService.startConnectionAttempt();
        }

        dashBoardViewModel = ViewModelProvider
                .AndroidViewModelFactory
                .getInstance(getApplication())
                .create(DashBoardViewModel.class);
        dashBoardViewModel.setX(bluetoothService);

        initialiseObservers();
    }


    private void initialiseObservers() {
        dashBoardViewModel.getTempDht().observe(this, tempDht -> binding.tempPx.setText(tempDht));
        dashBoardViewModel.getHumidityDht().observe(this, humidity -> binding.humidity.setText(humidity));
        dashBoardViewModel.getTempBarometer().observe(this, temp -> binding.tempDht22.setText(temp));
        dashBoardViewModel.getPxBarometer().observe(this, px -> binding.px.setText(px));
        dashBoardViewModel.getHeatIndexDht().observe(this, heatIndexC -> binding.heatIndex.setText(heatIndexC));
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
                } else {
                    previousConnectionMacAdd = bluetoothService.connectBluetooth(deviceToPair);
                    if (!previousConnectionMacAdd.isEmpty()) {
                        SharedPreferences sharedPreferences = getSharedPreferences("ble", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("last_device_mac", previousConnectionMacAdd);
                        editor.apply();
                    }
                }
            }
        } else {
            Toast.makeText(this, "fell through ...", Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // bluetoothAdapter.cancelDiscovery();

        // Don't forget to unregister the ACTION_FOUND receiver.
        if (receiverRegistered) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // resume requests
    }

    @Override
    protected void onStop() {
        super.onStop();
        // halt requests
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
    public void notifyUnSuccessfulConnection() {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_LONG).show();
    }
}