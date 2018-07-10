package com.cry.aidldemo;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class RemoteService extends Service {
    //IPC过程中，因为是对象的复制，所以监听事件并不能正常的移除。(反注册时，会重新构造一个参数)。
    // 所以需要用 RemoteCallbackList 这个类进行管理
    private RemoteCallbackList<IComputeCallBack> mCallbackList = new RemoteCallbackList<>();

    public RemoteService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        //具体实现的方法在stub里面
        ICompute.Stub stub = new ICompute.Stub() {

            @Override
            public void add(int a, int b) throws RemoteException {
                int result = a + b;
                //RemoteCallbackList 类的广播方法
                final int count = mCallbackList.beginBroadcast();
                for (int j = 0; j < count; j++) {
                    mCallbackList.getBroadcastItem(j).onResult(result);
                }
                mCallbackList.finishBroadcast();
            }

            @Override
            public void register(IComputeCallBack callback) throws RemoteException {
                mCallbackList.register(callback);
            }

            @Override
            public void unregister(IComputeCallBack callback) throws RemoteException {
                mCallbackList.unregister(callback);
            }

            //基本上每次IPC的过程都会调用到的onTransact方法。
            @Override
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                //进行权限校验
                int check = checkCallingOrSelfPermission("com.cry.aidldemo.ACCESS_REMOTE_SERVICE");
                if (check == PackageManager.PERMISSION_DENIED) {
                    return false;
                }
                String packageName="";
                String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
                if (packages != null && packages.length > 0) {
                    packageName = packages[0];
                }
                if (!packageName.startsWith("com.cry")){
                    return false;
                }
                return super.onTransact(code, data, reply, flags);
            }
        };

        return stub;
    }

}
