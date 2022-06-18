# react-native-react-native-daily-step-counter

> WIP. Definitely not production ready. 🚧

It collects and lets you query step counts per date without accessing to healthkit or google fitness api.

## Abstract

Basically it's a [CMPedometer](https://developer.apple.com/documentation/coremotion/cmpedometer) implemented in React Native.

On ios, there's a `RNReactNativeDailyStepCounter.m` which is a mere implementation of `CMPedometer`.

On android, `PedometerImple.java` is as android implementation of `CMPedometer`. `PedometerImple` is actually a [SenserEventListener](https://developer.android.com/reference/android/hardware/SensorEventListener) which listens to step counter event and save daily step count to shared preferences.

Not knowing both android and ios, I'm not 100% sure if it works properly and is good to go. Any idea and opinios are welcome.

## Usage

### | Installing package

```
yarn add react-native-daily-step-counter
```

### | Getting Permission

( not implemented yet.)

sepcify permissions needed.

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest>
	...
    <!-- belog API ver 28 -->
    <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
    <!-- above API ver 29 -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
	...
	<application />

</manifest>
```

```plist
<!-- ios/your_build_target/info.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	...
	<key>NSMotionUsageDescription</key>
	<string>Need Motion Permission</string>
	...
</dict>
```

check and ask for permission. ( recommend using [react-native-permission](https://github.com/zoontek/react-native-permissions) ). FYI, it seems to me that calling `CMPedometer.queryPedometerDataFromDate` for the first time force ios to request for the permission. Below is how I dealt with permission.

```ts
class Pedometer {
	...
  private askPermissionIOS(): Promise<boolean> {
    return new Promise(res => {
      this.queryPedometerDataBetweenDates(
        new Date().getDate(),
        new Date().getDate(),
        (error, data) => {
          if (data?.numberOfSteps === null) return res(false);
          res(true);
        }
      );
    });
  }

  public async checkPermission() {
    if (Platform.OS === 'android') {
      const permission = await check(PERMISSIONS.ANDROID.ACTIVITY_RECOGNITION);
      return permission === 'granted';
    }
    if (Platform.OS === 'ios') {
      const permission = await this.ios_authorizationStatus();
      return permission === 'authorized';
    }
    return false;
  }

  public async requestPermission(): Promise<boolean> {
    if (Platform.OS === 'android') {
      const permission = await request(
        PERMISSIONS.ANDROID.ACTIVITY_RECOGNITION
      );
      return permission === 'granted';
    }
    if (Platform.OS === 'ios') {
      const granted = await this.askPermissionIOS();
      return granted;
    }
    return false;
  }
  ...
}
```

### | Getting step count

you can get step count in 2 ways. By imperatively query for it (`queryPedometerDataBetweenDates`) or by listening to step count change event ( `startPedometerUpdatesFromDate` )

```ts
const Screen = () => {
  const [stepCount, setStepCount] = useState<number>(0);
  const [streaming, setStreaming] = useState<boolean>(false);

  const gettingStepCount = async () => {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 1); // getting date from yesterday
    startDate.setHours(0, 0, 0);
    const endDate = new Date();

    StepCount.queryPedometerDataBetweenDates(
      startDate.getTime(), // react native can't pass Date object, so you should pass timestamp.
      endDate.getTime(),
      (error, data) => {
        setStepCount(data?.numberOfSteps ?? 0);
      }
    );
  };

  const startStreaming = () => {
	setStreaming(true);
  }

  // android need initial listner register process.
  useEffect(() => {
    // on android, you should call StepCounter.startPedometerUpdatesFromDate once to start collecting step count.
    // make sure you get permission before you call this.
    // don't call stopPedometerUpdates unless you want to stop collecting step count.
    if (Platform.OS === "android") StepCounter.startPedometerUpdatesFromDate();
  }, []);

  useEffect(() => {
    if (streaming) {
      const startDate = new Date();
      date.setHours(0, 0, 0); // ios collects hours, minutes, seconds. but android only works for date. step counts are saved per date YYYY-MM-DD.
      StepCounter.startPedometerUpdatesFromDate(
        startDate.getTime(),
        (pedometerData) => {
          setStepCount(pedometerData.numberOfSteps);
        }
      );
    }
  }, [streaming]);

  return (
    <View>
      {stepCount}
      <TouchableOpacity onPress={gettingStepCount}>
        <Text>fetch data</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={startStreaming}>
        <Text>start data streaming</Text>
      </TouchableOpacity>
    </View>
  );
};
```
