package com.example.esp32.viewModels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.esp32.BluetoothService;

public class DashBoardViewModel extends ViewModel {
    BluetoothService bts;

    public DashBoardViewModel() {}

    public void setX(BluetoothService bluetoothService) {
        this.bts = bluetoothService;
    }

    public MutableLiveData<String> getTempDht() {
        return bts.getTempDht();
    }

    public void setTempDht(MutableLiveData<String> tempDht) {
        bts.setTempDht(tempDht);
    }

    public MutableLiveData<String> getHumidityDht() {
        return bts.getHumidityDht();
    }

    public void setHumidityDht(MutableLiveData<String> humidityDht) {
        bts.setHumidityDht(humidityDht);
    }

    public MutableLiveData<String> getTempBarometer() {
        return bts.getTempBarometer();
    }

    public void setTempBarometer(MutableLiveData<String> tempBarometer) {
        bts.setTempBarometer(tempBarometer);
    }

    public MutableLiveData<String> getHeatIndexDht() {
        return bts.getHeatIndexDht();
    }

    public void setHeatIndexDht(MutableLiveData<String> heatIndexDht) {
        bts.setHeatIndexDht(heatIndexDht);
    }

    public MutableLiveData<String> getPxBarometer() {
        return bts.getPxBarometer();
    }

    public void setPxBarometer(MutableLiveData<String> pxBarometer) {
        bts.setPxBarometer(pxBarometer);
    }

}
