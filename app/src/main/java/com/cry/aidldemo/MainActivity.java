package com.cry.aidldemo;

import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.cry.aidldemo.binder.BinderHookHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ICompute iCompute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<File> sharedPrefFiles = getSharedPrefFiles(this);
        for (File sharedPrefFile : sharedPrefFiles) {
            System.out.println(sharedPrefFile.getAbsoluteFile());
        }

        try {
            BinderHookHelper.hookClipboardService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClipboardManager
                systemService = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        systemService.getPrimaryClip();
    }


    public List<File> getSharedPrefFiles(Context context) {
        List<File> files = new ArrayList<>();
        String rootPath = context.getApplicationInfo().dataDir + "/shared_prefs";
        File root = new File(rootPath);
        if (root.exists()) {
            for (File file : root.listFiles()) {
                if (file.getName().endsWith(".xml")) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public void add(View view) {
        try {
            iCompute.add(1, 2);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void bind(View view) {
        bindService(new Intent(this, RemoteService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(MainActivity.this, "connected!!  " + name, Toast.LENGTH_SHORT).show();
                MainActivity.this.iCompute = ICompute.Stub.asInterface(service);
                System.out.println("connected!!  " + name);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(MainActivity.this, "disconnected!!  " + name, Toast.LENGTH_SHORT).show();
                System.out.println("disconnected!!  " + name);
            }
        }, BIND_AUTO_CREATE);
    }

    public void register(View view) {
        try {
            iCompute.register(computeCallBack);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregister(View view) {
        try {
            iCompute.unregister(computeCallBack);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private IComputeCallBack computeCallBack = new IComputeCallBack.Stub() {
        @Override
        public void onResult(int result) throws RemoteException {
            System.out.println("onResult=" + result);
            Toast.makeText(MainActivity.this, "onResult=" + result, Toast.LENGTH_SHORT).show();
        }
    };
}
