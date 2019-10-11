package com.dji.FPVDemo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class FPVDemoApplication extends Application{

    public static final String FLAG_CONNECTION_CHANGE = "fpv_tutorial_connection_change";

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private static BaseProduct mProduct;
    public Handler mHandler;

    private Application instance;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public FPVDemoApplication() {

    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        Camera camera = null;

        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        /**
         * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
         * the SDK Registration result and the product changing.
         */
        mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

            //Listens to the SDK registration result
            @Override
            public void onRegister(DJIError djiError) {
                if(djiError == DJISDKError.REGISTRATION_SUCCESS) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        }
                    });
                    DJISDKManager.getInstance().startConnectionToProduct();

                } else {

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });

                }
                Log.e("TAG", djiError.toString());
            }

            @Override
            public void onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect");
                notifyStatusChange();
            }
            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct));
                notifyStatusChange();

            }
            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                          BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                        @Override
                        public void onConnectivityChange(boolean isConnected) {
                            Log.d("TAG", "onComponentConnectivityChanged: " + isConnected);
                            notifyStatusChange();
                        }
                    });
                }

                Log.d("TAG",
                        String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent));

            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {

            }

        };
        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
            Toast.makeText(getApplicationContext(), "registering, pls wait...", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };

}
