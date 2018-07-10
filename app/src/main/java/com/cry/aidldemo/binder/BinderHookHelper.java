package com.cry.aidldemo.binder;

import android.content.ClipData;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * 回顾如何去找这个hook点。
 * 1. 通过getService方法，可以得到缓存在sCache中的binder对象。然后通过这binder对象来调用方法。其实就是一个IPC的过程
 * 2. 这个binder对象，其实就是 IClipboard 这个innterface来定义的
 * 3. 所以其实我们就是要hook 这个 IClipboard 对象。
 * 4. 从aidl中我们知道，得到对象是通过IClipboard$Stub的asInnterface方法。这个方法中先去查本地缓存，如果没有的话，就创建远程代理对象。
 *
 * 所以我们的目标就是讲这个本地缓存返回，缓存成我们自己的对象。就是queryLocalInterface 这个方法，返回我们代理之后的 innterface就可以了。
 *
 *
 * 对Binder进行Hook
 * 因为各个系统的服务都是通过binder进行调用。而binder的获取是通过getService方法。
 * 这个方法有一个缓存的对象sCache。我们只要替换掉这个集合里的binder就可以为所欲为
 * <p>
 * 1. Hook ServiceManager 的queryLocalInterface,返回我们自己的Binder
 * 2. Hook Binder 具体想要hook的方法，做想做的事情
 * 3. 调用Service方法的其实就是 通过aidl的 innterface来做的。所以调用方法的时候，需要转成Innterface
 * <p>
 *
 * Created by a2957 on 2018/7/10.
 */
public class BinderHookHelper {
    public static void hookClipboardService() throws Exception {
        final String CLIPBOARD_SERVICE = "clipboard";
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Method getService = serviceManager.getDeclaredMethod("getService", String.class);

        //先得到这个对象
        IBinder rawBinder = (IBinder) getService.invoke(null, CLIPBOARD_SERVICE);
        //然后创建动态代理.这个代理类，要hook掉binder中的queryLocalInterface。让他返回我们自己的本地对象
        IBinder hookBinder = (IBinder) Proxy.newProxyInstance(serviceManager.getClassLoader(), new Class[]{IBinder.class}, new ClipServiceProxyHookHandler(rawBinder));
        //替换掉sCache中的binder
        Field cacheField = serviceManager.getDeclaredField("sCache");
        cacheField.setAccessible(true);
        Map<String, IBinder> cache = (Map) cacheField.get(null);
        cache.put(CLIPBOARD_SERVICE, hookBinder);
    }

    public static class ClipServiceProxyHookHandler implements InvocationHandler {

        IBinder rawBinder;
        Class<?> stub;
        Class<?> iinterface;

        public ClipServiceProxyHookHandler(IBinder rawBinder) {
            this.rawBinder = rawBinder;
            //获得这个stub的class stub是真正实现了方法
            try {
                this.stub = Class.forName("android.content.IClipboard$Stub");
                //innterface 我们从aidl里面知道。这个是一个外包的一个接口。
                // stub 通过asInnterface可以转成
                //本地的对象，或者一个代理的对象
                this.iinterface = Class.forName("android.content.IClipboard");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //hook 调用queryLocalInterface 方法。
            // 返回我们自己的binder.stub.这个stub的部分方法由我们自己实现
            if ("queryLocalInterface".equals(method.getName())) {
                //第一个参数传入classLoader
                //第二参数，传入需要返回的接口类型。这里就是IClipboard Innterface这个接口类型
                //第三个传入代理的方法
                return Proxy.newProxyInstance(
                        proxy.getClass().getClassLoader(),
                        new Class[]{iinterface},
                        new BinderHookHandler(rawBinder, stub)
                );
            }
            return method.invoke(rawBinder, args);
        }
    }

    public static class BinderHookHandler implements InvocationHandler {
        private String TAG = "BinderHookHandler";

        Object hookInnterface;

        public BinderHookHandler(IBinder rawBinder, Class<?> stub) {
            Method asInterface = null;
            try {
                asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
//                asInterface.setAccessible(true);
                hookInnterface = asInterface.invoke(null, rawBinder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 把剪切版的内容替换为 "you are hooked"
            if ("getPrimaryClip".equals(method.getName())) {
                Log.d(TAG, "hook getPrimaryClip");
                return ClipData.newPlainText(null, "you are hooked");
            }

            // 欺骗系统,使之认为剪切版上一直有内容
            if ("hasPrimaryClip".equals(method.getName())) {
                return true;
            }
            //这里的rawBinder 需要换成android.content.IClipboard 才能进行调用方法
            return method.invoke(hookInnterface, args);
        }
    }


}
