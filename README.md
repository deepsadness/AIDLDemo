# AIDL
理解AIDL的机制。
### AIDL文件。
### AIDL文件生成的接口的构造。
- Innterface
- Stub
    - 这个类继承了`android.os.Binder`。具体实现了`Innterface`的接口。
    - `onTransact`方法。在每次IPC的过程中都会调用。可以用作权限检查的
    - `asInnterface`方法。
    `queryLocalInterface`先查询本地的缓存，如果没有的话，就创建远程的代理对象`Stub.Proxy`。
    - Proxy内部类。是远程的代理类