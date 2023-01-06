package com.example.esp32;

import static com.example.esp32.Util.Constants.REQUEST_ENABLE_BT;
import static com.example.esp32.Util.Constants.SELECT_DEVICE_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32.viewModels.DashBoardViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DashBoardActivity extends AppCompatActivity implements MyInterface {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    BluetoothDevice deviceToPair;

    TextView tempPX, tempDht, px, hum, heat;
    DashBoardViewModel dashBoardViewModel;
    private BluetoothService bluetoothService;
    //
    // private BluetoothAdapter bluetoothAdapter;
    // private BluetoothManager bluetoothManager;

    //     private final BroadcastReceiver receiver = new BroadcastReceiver() {
//         public void onReceive(Context context, Intent intent) {
//             String action = intent.getAction();
//
//             // Bluetooth paired event
//             if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
//                 connectBluetooth();
//             }
//
//             if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                 // Discovery has found a device. Get the BluetoothDevice
//                 // object and its info from the Intent.
//                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                 String deviceHardwareAddress = device.getAddress(); // MAC address
//                 Log.d(TAG, "onReceive: " + deviceHardwareAddress);
// //                Caution: Performing device discovery consumes a lot of the Bluetooth adapter's resources.
// //                After you have found a device to connect to, be certain that you stop discovery with
// //                cancelDiscovery() before attempting a connection. Also, you shouldn't perform discovery
// //                while connected to a device because the discovery process significantly reduces the
// //                bandwidth available for any existing connections.
//             }
//         }
//     };
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

        bluetoothService = new BluetoothService(this,
                (BluetoothManager) getSystemService(BluetoothManager.class));

        // if we have previously connected to a device try to connect

        // otherwise do a full connection routine

        bluetoothService.startConnectionAttempt();
        // bluetoothManager = getSystemService(BluetoothManager.class);
        // bluetoothAdapter = bluetoothManager.getAdapter();

        dashBoardViewModel = ViewModelProvider
                .AndroidViewModelFactory
                .getInstance(getApplication())
                .create(DashBoardViewModel.class);
        dashBoardViewModel.setX(bluetoothService);
        initialiseObservers();

        // if (bluetoothAvailable()) {
        //     AssociationRequest pairingRequest = new AssociationRequest.Builder()
        //             // Find only devices that match this request filter.
        //             .addDeviceFilter(new BluetoothDeviceFilter.Builder().build())
        //             // Stop scanning as soon as one device matching the filter is found.
        //             // .setSingleDevice(true)
        //             .build();
        //
        //     CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        //     deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {
        //         // Called when a device is found. Launch the IntentSender so the user can
        //         // select the device they want to pair with.
        //         @Override
        //         public void onDeviceFound(IntentSender chooserLauncher) {
        //             try {
        //                 startIntentSenderForResult(
        //                         chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
        //                 );
        //             } catch (IntentSender.SendIntentException e) {
        //                 Log.e("MainActivity", "Failed to send intent");
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(CharSequence error) {
        //             // Handle the failure.
        //         }
        //     }, null);
        //
        //
        //     // Register for broadcasts when a device is discovered.
        //     IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //     registerReceiver(receiver, filter);
        // }
    }

    // boolean bluetoothAvailable() {
    //     return bluetoothAdapter != null;
    // }

    // private void connectBluetooth() {
    //     UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //     try {
    //         BluetoothSocket socket = deviceToPair.createInsecureRfcommSocketToServiceRecord(uuid);
    //         bluetoothAdapter.cancelDiscovery();
    //         socket.connect();
    //         InputStream socketInputStream = socket.getInputStream();
    //         OutputStream socketOutputStream = socket.getOutputStream();
    //
    //         Flowable.create(new FlowableOnSubscribe<String>() {
    //                     @Override
    //                     public void subscribe(FlowableEmitter<String> emitter) throws Exception {
    //                         BufferedReader reader = new BufferedReader(new InputStreamReader(socketInputStream,
    //                                 StandardCharsets.UTF_8));
    //                         while (!emitter.isCancelled()) {
    //                             emitter.onNext(reader.readLine());
    //                         }
    //                         emitter.onComplete();
    //                     }
    //                 }, BackpressureStrategy.BUFFER)
    //                 .subscribeOn(Schedulers.io())
    //                 .observeOn(AndroidSchedulers.mainThread())
    //                 .subscribe(new Subscriber<String>() {
    //                     private Subscription subscription;
    //
    //                     @Override
    //                     public void onSubscribe(Subscription s) {
    //                         subscription = s;
    //                         requestDataPacket();
    //                         // subscription.request(1);
    //                         // try {
    //                         //     socketOutputStream.write('1');
    //                         // } catch (IOException e) {
    //                         //     e.printStackTrace();
    //                         // }
    //                     }
    //
    //                     @Override
    //                     public void onNext(String csvDataString) {
    //                         String[] dataArray = csvDataString.split(",");
    //
    //                         dashBoardViewModel.getHumidityDht().postValue(dataArray[Constants.DHT_HUMIDITY_INDEX]);
    //                         dashBoardViewModel.getTempDht().postValue(dataArray[Constants.DHT_TEMP_INDEX]);
    //                         dashBoardViewModel.getHeatIndexDht().postValue(dataArray[Constants.DHT_HEAT_INDEX]);
    //                         dashBoardViewModel.getPxBarometer().postValue(dataArray[Constants.BAROMETER_PX_INDEX]);
    //                         dashBoardViewModel.getTempBarometer().postValue(dataArray[Constants.BAROMETER_TEMP_INDEX]);
    //
    //                         // this call should be made at certain intervals
    //                         requestDataPacket();
    //
    //                         //this is a background thread
    //                         // try {
    //                         //     // Request more data by writing a 1 bit
    //                         //     socketOutputStream.write('1');
    //                         //     // Flowable requesting another batch of information
    //                         //     subscription.request(1);
    //                         // } catch (IOException e) {
    //                         //     e.printStackTrace();
    //                         // }
    //                     }
    //
    //                     @Override
    //                     public void onError(Throwable t) {
    //
    //                     }
    //
    //                     @Override
    //                     public void onComplete() {
    //
    //                     }
    //
    //                     private void requestDataPacket() {
    //                         try {
    //                             subscription.request(1);
    //                             socketOutputStream.write('1');
    //                         } catch (IOException e) {
    //                             e.printStackTrace();
    //                         }
    //                     }
    //                 });
    //
    //         Toast.makeText(this, "Connection successful!", Toast.LENGTH_SHORT).show();
    //
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

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
        if (requestCode == 5) {
            Toast.makeText(this, "fuck", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENABLE_BT) {
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
        //
    }

    @Override
    public void notifySuccessfulConnection() {
        Toast.makeText(this, "Connection successful!", Toast.LENGTH_SHORT).show();
    }
}
//
//     public static class MyBluetoothService {
//
//         private static Handler handler; // handler that gets info from Bluetooth service
//
//         // Defines several constants used when transmitting messages between the
//         // service and the UI.
//         private interface MessageConstants {
//
//             // ... (Add other message types here as needed.)
//         }
//
//         private static class ConnectedThread extends Thread {
//             private static BluetoothSocket mmSocket;
//             private static InputStream mmInStream;
//             private static OutputStream mmOutStream;
//             private final Context context;
//             private final Activity activity;
//             private byte[] mmBuffer; // mmBuffer store for the stream
//
//             public ConnectedThread(BluetoothSocket socket, Context context, Activity activity) throws IOException {
//                 mmSocket = socket;
//                 this.context = context;
//                 this.activity = activity;
//
//                 InputStream tmpIn = null;
//                 OutputStream tmpOut = null;
//
//                 // Get the input and output streams; using temp objects because
//                 // member streams are final.
//                 try {
//                     tmpIn = socket.getInputStream();
//                 } catch (IOException e) {
//                     Log.e(TAG, "Error occurred when creating input stream", e);
//                 }
//                 try {
//                     tmpOut = socket.getOutputStream();
//                 } catch (IOException e) {
//                     Log.e(TAG, "Error occurred when creating output stream", e);
//                 }
//
//                 mmInStream = tmpIn;
//                 mmOutStream = tmpOut;
//             }
//
//             @SuppressLint("MissingPermission")
//             public void run() {
//
// //                // Make a connection to the BluetoothSocket
// //                try {
// //                    // This is a blocking call and will only return on a
// //                    // successful connection or an exception
// //
// //
// //                } catch (IOException e) {
// //                    throw new RuntimeException(e);
// //                    // Close the socket
// ////                    try {
// ////                        mmSocket.close();
// ////                    } catch (IOException e2) {
// ////                        Log.e(TAG, "unable to close() socket during connection failure", e2);
// ////                    }
// //                }
//
//                 mmBuffer = new byte[1024];
//                 int numBytes; // bytes returned from read()
//
//                 // Keep listening to the InputStream until an exception occurs.
//                 while (true) {
//                     try {
//                         Log.d("x", "x");
//                         // Read from the InputStream.
//                         numBytes = mmInStream.read(mmBuffer);
//                         // Send the obtained bytes to the UI activity.
//                         Message readMsg = handler.obtainMessage(
//                                 MessageConstants.MESSAGE_READ, numBytes, -1,
//                                 mmBuffer);
//                         readMsg.sendToTarget();
//                     } catch (IOException e) {
//                         Log.d(TAG, "Input stream was disconnected", e);
//                         break;
//                     }
//                 }
//             }
//
//             // Call this from the main activity to send data to the remote device.
//             public void write(byte[] bytes) {
//                 try {
//                     mmOutStream.write(bytes);
//
//                     // Share the sent message with the UI activity.
//                     Message writtenMsg = handler.obtainMessage(
//                             MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
//                     writtenMsg.sendToTarget();
//                 } catch (IOException e) {
//                     Log.e(TAG, "Error occurred when sending data", e);
//
//                     // Send a failure message back to the activity.
//                     Message writeErrorMsg =
//                             handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
//                     Bundle bundle = new Bundle();
//                     bundle.putString("toast",
//                             "Couldn't send data to the other device");
//                     writeErrorMsg.setData(bundle);
//                     handler.sendMessage(writeErrorMsg);
//                 }
//             }
//
//             // Call this method from the main activity to shut down the connection.
//             public void cancel() {
//                 try {
//                     mmSocket.close();
//                 } catch (IOException e) {
//                     Log.e(TAG, "Could not close the connect socket", e);
//                 }
//             }
//         }
//     }
// }