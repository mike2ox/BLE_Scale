package com.example.moonh.inbody_scale;

/**
 * Created by moonh on 2017-05-12.
 */
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BluetoothLeService extends Service{
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.moonh.inbody_scale.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.moonh.inbody_scale.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.moonh.inbody_scale.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.moonh.inbody_scale.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.moonh.inbody_scale.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    //String a="";
    //String b = "";
    //int i = 0;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public void writeCustomCharacteristic(byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        ////이부분이 의심됨
		/*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        } if(mCustomService !=null) {
            Log.w(TAG, "BLE가 되는겨!!!!!!!!!!!!!!!!!");
        }
		/*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"));
        mWriteCharacteristic.setValue(value);
        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            Log.w(TAG, "Failed to write characteristic");
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        // Log.e("uuidtest", characteristic.getUuid().toString());

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {

            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                //Log.e(TAG, "Heart rate format UINT16.!!!!!!!!");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                //Log.e(TAG, "Heart rate format UINT8.!!!!!!!!");
            }
/////////////////////////////////이부분이 BLE앱 전용 코드
            String mValue = "";
            try {
                StringBuilder sb = new StringBuilder();
                String str = getHexString(characteristic.getValue()).toUpperCase();

                sb.append(str.substring(6, 10));

                float mValue1 = (float)getHexToDec(sb.toString());

                NumberFormat numberformat = new DecimalFormat("###,###.##");
                // UI상에서 보여줄 값

                String strVal = numberformat.format(mValue1/100.0D);

                intent.putExtra(EXTRA_DATA, strVal);
                LogUtil.e(characteristic.getValue().toString());
                LogUtil.e(characteristic.getUuid().toString());
                LogUtil.e(str);
                LogUtil.e("!!!");

                StringBuilder stringbuilder = new StringBuilder();
                StringBuilder stringbuilder2 = new StringBuilder();
                StringBuilder stringbuilder3 = new StringBuilder();
                StringBuilder stringbuilder4 = new StringBuilder();
                StringBuilder stringbuilder5 = new StringBuilder();
                StringBuilder stringbuilder6 = new StringBuilder();
                StringBuilder stringbuilder7 = new StringBuilder();

                float weight_Value = (float)getHexToDec(stringbuilder.append(str.substring(10, 14)).toString());
                float fat_Value = (float)getHexToDec(stringbuilder2.append(str.substring(14, 18)).toString());
                float water_Value = (float)getHexToDec(stringbuilder3.append(str.substring(18, 22)).toString());
                float muscle_Value = (float)getHexToDec(stringbuilder4.append(str.substring(22, 26)).toString());
                float BMR_Value = (float)getHexToDec(stringbuilder5.append(str.substring(26, 30)).toString());
                float visceralfat_Value = (float)getHexToDec(stringbuilder6.append(str.substring(30, 34)).toString());
                float bone_Value = (float)getHexToDec(stringbuilder7.append(str.substring(34, 36)).toString());


                NumberFormat numformat1 = new DecimalFormat("###,###.##");
                NumberFormat numformat2 = new DecimalFormat("###,###.##");
                NumberFormat numformat3 = new DecimalFormat("###,###.##");
                NumberFormat numformat4 = new DecimalFormat("###,###.##");
                NumberFormat numformat5 = new DecimalFormat("###,###.##");
                NumberFormat numformat6 = new DecimalFormat("###,###.##");
                NumberFormat numformat7 = new DecimalFormat("###,###.##");

                String wegiht_Val = numformat1.format(weight_Value/10.0D);
                String fat_Val = numformat2.format(fat_Value/10.0D);
                String water_Val = numformat3.format(water_Value/10.0D);
                String muscle_Val = numformat4.format(muscle_Value/10.0D);
                String BMR_Val = numformat5.format(BMR_Value/1.0D);
                String visceralfat_Val = numformat6.format(visceralfat_Value/10.0D);
                String bone_Val = numformat7.format(bone_Value/10.0D);


                Log.e("데이터 정보", "체중 = " + wegiht_Val + "kg" + "\n"+
                        "fat = " + fat_Val + "%" + "\n"+
                        "체수분 = " + water_Val + "%" + "\n"+
                        "근육량 = " + muscle_Val + "%" + "\n"+
                        "BMR = " + BMR_Val + "kcal" + "\n"+
                        "visfat = " + visceralfat_Val + "%" + "\n"+
                        "골격 = " + bone_Val + "kg" + "\n");


            } catch (Exception e) {e.printStackTrace();}

            final int heartRate = characteristic.getIntValue(format, 1);

            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public void writeCustomCharacteristic(int value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            //            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
		/*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"));
        if(mCustomService == null){
            //            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
		/*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"));
        mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            //            Log.w(TAG, "Failed to write characteristic");
        }
    }

    public static float changeOnePoint(float paramFloat, int paramInt)
    {
        return new BigDecimal(paramFloat).setScale(paramInt, 4).floatValue();
    }

    public static int dec(String hex){
        String[] temp = hex.split("0x");
        StringBuffer dec = null;
        for(String strArr : temp){
            dec = new StringBuffer();
            dec.append(strArr);
        }
        return Integer.parseInt(dec.toString(), 16);
    }

    public static int hexToTen(String paramString)
    {
        if ((paramString == null) || ((paramString != null) && ("".equals(paramString)))) {}
        for (int i = 0;; i = Integer.valueOf(paramString, 16).intValue()) {
            return i;
        }
    }

    private float getHexToDec(String hex) {
        long v = Long.parseLong(hex, 16);
        return Float.parseFloat(String.valueOf(v));
    }

    public String byteToBinaryString(byte n) {
        StringBuilder sb = new StringBuilder("00000000");
        for (int bit = 0; bit < 8; bit++) {
            if (((n >> bit) & 1) > 0) {
                sb.setCharAt(7 - bit, '1');
            }
        }
        return sb.toString();
    }

    private String getHexString(byte b) {
        try {
            return Integer.toString( ( b & 0xff ) + 0x100, 16).substring( 1 );
        } catch (Exception e) {	return null; }
    }

    private String getHexString(byte[] b) {
        String result = "";
        try {
            for (int i=0; i < b.length; i++) {
                result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
            return result;
        } catch (Exception e) {	return null; }
    }
//////////////////////////////여기까지 블루케어 자체 코드
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);    //GATT서버 연결
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
