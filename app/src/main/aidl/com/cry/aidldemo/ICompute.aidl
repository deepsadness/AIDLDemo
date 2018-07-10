// ICompute.aidl
package com.cry.aidldemo;

import com.cry.aidldemo.IComputeCallBack;

interface ICompute {
    void add(int a,int b);

    void register(IComputeCallBack callback);
    void unregister(IComputeCallBack callback);
}
