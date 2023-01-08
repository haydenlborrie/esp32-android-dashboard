package com.example.esp32;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.esp32.Util.Constants;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private final MyInterface myInterface;
    private BluetoothAdapter bluetoothAdapter;

    MutableLiveData<String> tempDht, humidityDht, tempBarometer, heatIndexDht, pxBarometer;

    public void setTempDht(MutableLiveData<String> tempDht) {
        this.tempDht = tempDht;
    }

    public void setHumidityDht(MutableLiveData<String> humidityDht) {
        this.humidityDht = humidityDht;
    }

    public void setTempBarometer(MutableLiveData<String> tempBarometer) {
        this.tempBarometer = tempBarometer;
    }

    public void setHeatIndexDht(MutableLiveData<String> heatIndexDht) {
        this.heatIndexDht = heatIndexDht;
    }

    public void setPxBarometer(MutableLiveData<String> pxBarometer) {
        this.pxBarometer = pxBarometer;
    }

    public MutableLiveData<String> getTempDht() {
        return tempDht;
    }

    public MutableLiveData<String> getHumidityDht() {
        return humidityDht;
    }

    public MutableLiveData<String> getTempBarometer() {
        return tempBarometer;
    }

    public MutableLiveData<String> getHeatIndexDht() {
        return heatIndexDht;
    }

    public MutableLiveData<String> getPxBarometer() {
        return pxBarometer;
    }

    public BluetoothService(MyInterface myInterface,
                            BluetoothManager bluetoothManager) {
        this.myInterface = myInterface;
        bluetoothAdapter = bluetoothManager.getAdapter();
        tempDht = new MutableLiveData<>();
        humidityDht = new MutableLiveData<>();
        heatIndexDht = new MutableLiveData<>();
        tempBarometer = new MutableLiveData<>();
        pxBarometer = new MutableLiveData<>();
    }

    public void startConnectionAttempt() {
        if (bluetoothAvailable()) {
            myInterface.pairingRequest();
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
        }

    }

    boolean bluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public void connectBluetooth(BluetoothDevice deviceToPair) {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            BluetoothSocket socket = deviceToPair.createInsecureRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            socket.connect();
            InputStream socketInputStream = socket.getInputStream();
            OutputStream socketOutputStream = socket.getOutputStream();

            Flowable.create(new FlowableOnSubscribe<String>() {
                        @Override
                        public void subscribe(FlowableEmitter<String> emitter) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socketInputStream,
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
                            subscription = s;
                            requestDataPacket();
                        }

                        @Override
                        public void onNext(String csvDataString) {
                            String[] dataArray = csvDataString.split(",");

                            humidityDht.postValue(dataArray[Constants.DHT_HUMIDITY_INDEX]);
                            tempDht.postValue(dataArray[Constants.DHT_TEMP_INDEX]);
                            heatIndexDht.postValue(dataArray[Constants.DHT_HEAT_INDEX]);
                            pxBarometer.postValue(dataArray[Constants.BAROMETER_PX_INDEX]);
                            tempBarometer.postValue(dataArray[Constants.BAROMETER_TEMP_INDEX]);

                            // this call should be made at certain intervals
                            requestDataPacket();
                        }

                        @Override
                        public void onError(Throwable t) {

                        }

                        @Override
                        public void onComplete() {

                        }

                        private void requestDataPacket() {
                            try {
                                subscription.request(1);
                                socketOutputStream.write('1');
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            myInterface.notifySuccessfulConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    // @Override
    // protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    //     if (requestCode == 5) {
    //         Toast.makeText(this, "fuck", Toast.LENGTH_SHORT).show();
    //     } else if (requestCode == REQUEST_ENABLE_BT) {
    //         if (resultCode == RESULT_CANCELED) {
    //             Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT)
    //                     .show();
    //         } else {
    //             Toast.makeText(this, "User enabled bluetooth", Toast.LENGTH_SHORT).show();
    //         }
    //     } else if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
    //         deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
    //
    //         if (deviceToPair != null) {
    //             boolean notPaired = deviceToPair.createBond();
    //
    //             if (notPaired) {
    //                 IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    //                 registerReceiver(receiver, filter);
    //             } else
    //                 connectBluetooth(deviceToPair);
    //         }
    //     } else {
    //         Toast.makeText(this, "fell through ...", Toast.LENGTH_SHORT).show();
    //         super.onActivityResult(requestCode, resultCode, data);
    //     }
    // }


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
            // Call this method from the main activity to shut down the connection.
            public void cancel () {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }

            @SuppressLint("MissingPermission")
            public void run() {


               // Make a connection to the BluetoothSocket
               // try {
               //     // This is a blocking call and will only return on a
               //     // successful connection or an exception
               //
               //
               // } catch (IOException e) {
               //     throw new RuntimeException(e);
               //     // Close the socket
               //     try {
               //         mmSocket.close();
               //     } catch (IOException e2) {
               //         Log.e(TAG, "unable to close() socket during connection failure", e2);
               //     }
               // }

                //     mmBuffer = new byte[1024];
                //     int numBytes; // bytes returned from read()
                //
                //     // Keep listening to the InputStream until an exception occurs.
                //     while (true) {
                //         try {
                //             Log.d("x", "x");
                //             // Read from the InputStream.
                //             numBytes = mmInStream.read(mmBuffer);
                //             // Send the obtained bytes to the UI activity.
                //             Message readMsg = handler.obtainMessage(
                //                     DashBoardActivity.MyBluetoothService.MessageConstants.MESSAGE_READ, numBytes, -1,
                //                     mmBuffer);
                //             readMsg.sendToTarget();
                //         } catch (IOException e) {
                //             Log.d(TAG, "Input stream was disconnected", e);
                //             break;
                //         }
                //     }
                // }

                // // Call this from the main activity to send data to the remote device.
                // public void write(byte[] bytes) {
                //     try {
                //         mmOutStream.write(bytes);
                //
                //         // Share the sent message with the UI activity.
                //         Message writtenMsg = handler.obtainMessage(
                //                Constants.MESSAGE_WRITE, -1, -1, mmBuffer);
                //         writtenMsg.sendToTarget();
                //     } catch (IOException e) {
                //         Log.e(TAG, "Error occurred when sending data", e);
                //
                //         // Send a failure message back to the activity.
                //         Message writeErrorMsg =
                //                 handler.obtainMessage(DashBoardActivity.MyBluetoothService.MessageConstants.MESSAGE_TOAST);
                //         Bundle bundle = new Bundle();
                //         bundle.putString("toast",
                //                 "Couldn't send data to the other device");
                //         writeErrorMsg.setData(bundle);
                //         handler.sendMessage(writeErrorMsg);
                //     }
                // }
            }
        }
    }
}
