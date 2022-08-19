## SDK集成向导/SDK Integration

Access SDK是用Kotlin语言编写，如果您的app也是用Kotlin编写的话将会有利于集成，别担心，Java语言也可以集成我们的SDK。  

Access SDK is written in Kotlin, although there are ways to do integration with Java project, it's easier if your project is written in Kotlin too.

Access SDK版本信息： **TARGET SDK 31** /**MIN SDK 26**，App需要支持最小版本26才能集成Access SDK，并且因为SDK需要用蓝牙（BLE）和硬件设备进行进行交互，所以请体检检查和确保用户手机的蓝牙能力和硬件交互能力。

SDK is targeting **SDK 31** and **MIN SDK 26**, also this SDK uses Bluetooth(BLE) Adapter on device, so please check the device hardware availability first.

Access SDK被存储在一个私有的Maven仓库中，您需要有一个账户去下载和引入Access SDK， 如果您还没有此账户，请联系我们。

Before implementing SDK integration, you will need to have  **Dependency credential** to be able to use our service, please contact us and apply if you don't have this.

### 引入依赖/Download Dependency

在工程级别的gradle文件中，加入代码片段

Go to project level gradle file and add

``` allprojects {    
    repositories {   
          google()   
          jcenter()   
          maven {                 
          url 'https://teamaikesi.jfrog.io/artifactory/android-apps'    
          credentials { username = 'USERNAME' password = 'PASSWORD' }   
       }  
 } 
```
在需要使用sdk的module gradle文件中，加入代码片段并且编译

In module gradle file, add
```
implementation 'com.teamx:access-sdk:1.0.0'
``` 

### 初始化SDK/Init the SDK
我们建议您在Application类中初始化我们的SDK（初始化SDK不会消耗任何资源和启动任何的任务以及服务，只是把SDK所需要的发送给SDK便于SDK在APP中提供后续服务）

We suggest you to initialize the SDK in your Application class (init the SDK won't drain any resource or run any tasks, it's just a way for integrator to pass necessary information to SDK)

#### 设置notification/Setup notification
Access SDK在用蓝牙和硬件设备交互中，会启动一个前台服务来确保蓝牙交互不会被系统关闭，安卓系统要求每一个前台服务必须要带有一个notification，这里您可以自定义notification的图标和文字

Access SDK would start a foreground service and then scan nearby devices, by Android requirement, foreground service must have a notification, here you can pass notification icon and content

```  
val notificationInfo = ServiceNotificationInfo.Builder()
.setIcon(R.mipmap.ic_launcher)
.setTitle(getString(R.string.service_notification_title))
.setMessage(getString(R.string.service_notification_content))
.setForegroundServiceNotificationId(NOTIFICATION_ID)
.setNotificationChannelName(NOTIFICATION_CHANNEL_NAME)
.build()  
```  

然后生成Config对象, 进行SDK的初始化

After that, you need to build and pass **Config** object and pass to SDK init method

```  
val config = Config.Builder()
.setAppId("your app package")
.setServiceNotificationInfo(notificationInfo) 
.setDisableAutoUnlock(false) // not mendantory, default is false, is you want to disabletap-in mode, set this to true 
.build()   

AccessSdk.initAccessSdk(config) 
```
### 用户登录/User Login
Access SDK使用邮箱/大陆手机号+验证码的方式来登录用户

We use email/phone(Mainland China only) + verification code to auth an user

输入用户名（邮箱/手机号）换取验证码

Request Auth Code
```  
AccessSdk.UserModule.getVerificationCodeForUserName(username: String, sdkCallback: SDKCallback<Unit>) ```  
如果成功，验证码将发送到用户的邮箱/手机短信
At this point an verification code should be sent to the user's email or phone, now login with email + verification code  
```  

用户登录

User Sign In
```
AccessSdk.UserModule.signInWithVerificationCode(email: username, code: String, sdkCallback: SDKCallback<Unit>)  
```  

### 设置监听/Setup listeners
Access SDK使用了[Kotlin Sealed Class](https://kotlinlang.org/docs/sealed-classes.html#location-of-direct-subclasses)来展示SDK的不同状态

Access SDK uses Kotlin Sealed class to indicator different sdk events

App可以只设置一个监听器就可以监听所有SDK的事件

Single place for listening to SDK events
```  
AccessSDK.addServiceListener(object :ServiceListener{         
   override fun onStatus(status: ServiceStatusEvent) {  
   //此处返回SDK服务的不同状态的改变
   //ServiceStatusEvent indicates the status of the service    
   }      
   
   override fun onPresence(presence: ExternalPresence) {      
   //此处返回附近扫描到的并且有权限的蓝牙设备，用户后续的UI展示以及开门等交互
   //presence means neary devices, in presence you can check    
   //rssi (ble signal strangeth), deviceID, access state and other information 
   }  
       
   override fun onCommandResult(result: CommandResult) {    
   //每一次发送一个command，您将会拿到一个commandID，这里会返回command结果（可以用result中的id进行command匹配）  
   //once you send a command to a nearby device, you will get an command id, in this callback you can check the command result based on the mapping of command id    
   } 
}) 
```  
这里展示不同的ServiceStatusEvent情况

We use Kotlin sealed class to indicate different status, here is a list of possible status and errors
``` 
sealed class ServiceStatusEvent {      
    object ServiceStarted : ServiceStatusEvent()      
    object ServiceStopped : ServiceStatusEvent()      
	data class Error(val error: ServiceError) : ServiceStatusEvent() 
} 
sealed class ServiceError {      
    object PermissionNotSufficient : ServiceError()      
    object SdkNotInitialized : ServiceError()      
    object UserNotAuthenticated : ServiceError()      
    object BlueToothAdapterNotSupported : ServiceError()      
    object AdvertiserError : ServiceError() 
} 
```

### 启动和停止SDK服务/Start&Stop Service
注意SDK服务调用设备的蓝牙服务和附近的设备进行交互，请确保您的App拥有蓝牙服务必要的权限以及开关，在安卓12一下您将会需要位置权限，在安卓12及以上您将需要附近的设备权限。以及用户设备的蓝牙开关必须开启

Note that this service would start BLE scan, please make sure you have both location permission and BT permission on before starting this service. (in Android 12 it would be various bluetooth permissions, we need them all)

```  
AccessSdk.startService() 
AccessSdk.stopService()  
```  
### 发送一个command/Enqueue a command

一旦SDK的服务被开启，您在监听中收到了身边的presence设备，您可以用此方法发送一个command实现开门

Once service is started and you have **presence** object in the callback, you can enqueue a command to unlock the reader

```  
AccessSdk.enqueueCommand(presenceId,UserCommand.ManualUnlock)  
```  
### 终止/Terminate
如果您想彻底停止SDK的功能，请调用此方法

If you want to terminate SDK and service at all, call this
```  
AccessSdk.terminate()  
```  
### 其他/Others:
刷新用户权限和数据

Refresh User's Permission
```  
AccessSdk.UserModule.refreshUserPermission(sdkCallback: SDKCallback<Unit>)  
```  

登出

Revoke User's session (sign out)
```  
AccessSdk.UserModule.revokeUser(sdkCallback: SDKCallback<Unit>)  
```  

获取用户信息

Get User's information
```  
AccessSdk.UserModule.requestUser(sdkCallback: SDKCallback<User>) 
```  

查看是否登录

Check User Login Status
```  
AccessSdk.UserModule.isAuth() 
```  

如果需要额外的SDK使用只能，请联系我们

For other questions or integration help, please contact us and we are happy to help.