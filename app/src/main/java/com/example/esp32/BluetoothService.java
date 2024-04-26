package com.example.esp32;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

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
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService {
    private final MyInterface myInterface;
    private final BluetoothAdapter bluetoothAdapter;

    MutableLiveData<String> tempDht, humidityDht, tempBarometer, heatIndexDht, pxBarometer;

    public BluetoothService(MyInterface myInterface, BluetoothAdapter bluetoothAdapter) {
        this.myInterface = myInterface;
        this.bluetoothAdapter = bluetoothAdapter;
        tempDht = new MutableLiveData<>();
        humidityDht = new MutableLiveData<>();
        heatIndexDht = new MutableLiveData<>();
        tempBarometer = new MutableLiveData<>();
        pxBarometer = new MutableLiveData<>();
    }

    public MutableLiveData<String> getTempDht() {
        return tempDht;
    }

    public void setTempDht(MutableLiveData<String> tempDht) {
        this.tempDht = tempDht;
    }

    public MutableLiveData<String> getHumidityDht() {
        return humidityDht;
    }

    public void setHumidityDht(MutableLiveData<String> humidityDht) {
        this.humidityDht = humidityDht;
    }

    public MutableLiveData<String> getTempBarometer() {
        return tempBarometer;
    }

    public void setTempBarometer(MutableLiveData<String> tempBarometer) {
        this.tempBarometer = tempBarometer;
    }

    public MutableLiveData<String> getHeatIndexDht() {
        return heatIndexDht;
    }

    public void setHeatIndexDht(MutableLiveData<String> heatIndexDht) {
        this.heatIndexDht = heatIndexDht;
    }

    public MutableLiveData<String> getPxBarometer() {
        return pxBarometer;
    }

    public void setPxBarometer(MutableLiveData<String> pxBarometer) {
        this.pxBarometer = pxBarometer;
    }

    public void startConnectionAttempt() {
        myInterface.pairingRequest();
    }


    public String connectBluetooth(BluetoothDevice deviceToPair) {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            BluetoothSocket socket = deviceToPair.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();

            bluetoothAdapter.cancelDiscovery();
            InputStream socketInputStream = socket.getInputStream();
            OutputStream socketOutputStream = socket.getOutputStream();

            Flowable.create((FlowableOnSubscribe<String>) emitter -> {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socketInputStream,
                                StandardCharsets.UTF_8));
                        while (!emitter.isCancelled()) {
                            emitter.onNext(reader.readLine());
                        }
                        emitter.onComplete();
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

        } catch (IOException e) {
            e.printStackTrace();
            myInterface.notifyUnSuccessfulConnection();
        }
        return deviceToPair.getAddress();
    }
}
