package com.example.esp32;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableSubscriber;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";

    public static class MyBluetoothService {

        private static Handler handler; // handler that gets info from Bluetooth service

        // Defines several constants used when transmitting messages between the
        // service and the UI.
        private interface MessageConstants {
            public static final int MESSAGE_READ = 0;
            public static final int MESSAGE_WRITE = 1;
            public static final int MESSAGE_TOAST = 2;

            // ... (Add other message types here as needed.)
        }

        private static class ConnectedThread extends Thread {
            private static BluetoothSocket mmSocket;
            private static InputStream mmInStream;
            private static OutputStream mmOutStream;
            private final Context context;
            private final Activity activity;
            private byte[] mmBuffer; // mmBuffer store for the stream

            public ConnectedThread(BluetoothSocket socket, Context context, Activity activity) throws IOException {
                mmSocket = socket;
                this.context = context;
                this.activity = activity;

                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;

            }


            @SuppressLint("MissingPermission")
            public void run() {

//                // Make a connection to the BluetoothSocket
//                try {
//                    // This is a blocking call and will only return on a
//                    // successful connection or an exception
//
//
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                    // Close the socket
////                    try {
////                        mmSocket.close();
////                    } catch (IOException e2) {
////                        Log.e(TAG, "unable to close() socket during connection failure", e2);
////                    }
//                }

                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        Log.d("x", "x");
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);
                        // Send the obtained bytes to the UI activity.
                        Message readMsg = handler.obtainMessage(
                                MessageConstants.MESSAGE_READ, numBytes, -1,
                                mmBuffer);
                        readMsg.sendToTarget();
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }

            // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes) {
                try {
                    mmOutStream.write(bytes);

                    // Share the sent message with the UI activity.
                    Message writtenMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                    writtenMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);

                    // Send a failure message back to the activity.
                    Message writeErrorMsg =
                            handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString("toast",
                            "Couldn't send data to the other device");
                    writeErrorMsg.setData(bundle);
                    handler.sendMessage(writeErrorMsg);
                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }
    }

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int BLUETOOTH_CONNECT_REQUEST_CODE = 3;
    private static final int SELECT_DEVICE_REQUEST_CODE = 4;


    //     // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                connectBluetooth();
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

//                if (ActivityCompat.checkSelfPermission(this,
//                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

//                Caution: Performing device discovery consumes a lot of the Bluetooth adapter's resources.
//                After you have found a device to connect to, be certain that you stop discovery with
//                cancelDiscovery() before attempting a connection. Also, you shouldn't perform discovery
//                while connected to a device because the discovery process significantly reduces the
//                bandwidth available for any existing connections.
            }
        }
    };


    Context context;
    private MainActivity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    BluetoothDevice deviceToPair;
    MyBluetoothService.ConnectedThread connectedThread;
    Charset charset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        activity = this;

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAvailable()) {
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
                        startIntentSenderForResult(
                                chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                        );
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
            // IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
        }
    }

    private String[] getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_DENIED) {
            permissions.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
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

    boolean bluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    private void connectBluetooth() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            BluetoothSocket socket = deviceToPair.createInsecureRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            socket.connect();
            InputStream i = socket.getInputStream();


            // val reader = BufferedReader(InputStreamReader(inputStream, charset))
            // while (!emitter.isCancelled && !closed.get()) {
            //     synchronized(inputStream) {
            //         try {
            //             val receivedString = reader.readLine()
            //             if (!TextUtils.isEmpty(receivedString)) {
            //                 emitter.onNext(receivedString)
            //             }
            //         } catch (e: Exception) {
            //             if (!emitter.isCancelled && !closed.get()) {
            //                 emitter.onError(e)
            //             }
            //         }
            //     }


            // Observable.create(new ObservableOnSubscribe<String>() {
            //             @Override
            //             public void subscribe(ObservableEmitter<String> emitter) throws Exception {
            //                 BufferedReader reader = new BufferedReader(new InputStreamReader(i,
            //                         StandardCharsets.UTF_8));
            //                 while (!emitter.isDisposed()) {
            //                     emitter.onNext(reader.readLine());
            //                 }
            //             }
            //         })
            //         .subscribeOn(Schedulers.io())
            //         .observeOn(AndroidSchedulers.mainThread())
            //         .subscribe(new Observer<String>() {
            //             @Override
            //             public void onSubscribe(Disposable d) {
            //                 Log.d(TAG, "onSubscribe: ");
            //             }
            //
            //             @Override
            //             public void onNext(String s) {
            //                 Log.d(TAG, "onNext: " + s);
            //             }
            //
            //             @Override
            //             public void onError(Throwable e) {
            //
            //             }
            //
            //             @Override
            //             public void onComplete() {
            //                 Log.d(TAG, "onComplete: ");
            //             }
            //         });

            Flowable.create(new FlowableOnSubscribe<String>() {
                        @Override
                        public void subscribe(FlowableEmitter<String> emitter) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(i,
                                    StandardCharsets.UTF_8));
                            while (!emitter.isCancelled()) {
                                emitter.onNext(reader.readLine());
                            }
                            emitter.onComplete();
                        }
                    }, BackpressureStrategy.BUFFER)

                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<String>() {
                        private Subscription subscription;

                        @Override
                        public void onSubscribe(Subscription s) {
                            subscription=s;
                            subscription.request(3);
                        }

                        @Override
                        public void onNext(String s) {
                            Log.d(TAG, s);
                            subscription.request(1);
                        }

                        @Override
                        public void onError(Throwable t) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });

            Toast.makeText(context, "Connection successful!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } else if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

            if (deviceToPair != null) {
                boolean notPaired = deviceToPair.createBond();

                if (notPaired) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(receiver, filter);
                } else
                    connectBluetooth();
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
}