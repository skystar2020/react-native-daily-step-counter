import {
  EmitterSubscription,
  EventSubscriptionVendor,
  NativeEventEmitter,
  NativeModule,
  NativeModules,
  Platform,
} from "react-native";

const NativeStepCounter: NativeStepCounter =
  NativeModules.RNReactNativeDailyStepCounter;

interface NativeStepCounter extends EventSubscriptionVendor, NativeModule {
  startPedometerUpdatesFromDate: (date: number) => void;
  stopPedometerUpdates: () => void;
  queryPedometerDataBetweenDates: (
    startDate: number,
    endDate: number,
    handler: (error: any, data: PedometerData) => void
  ) => void;

  authorizationStatus: (
    callback: (
      error: any,
      status:
        | 'denied'
        | 'authorized'
        | 'restricted'
        | 'not_determined'
        | 'not_available'
    ) => void
  ) => void;
}

export type PedometerData = {
  startDate?: string;
  endDate?: string;
  date: string;
  numberOfSteps: number;
};

class Pedometer {
  private subscription?: EmitterSubscription;
  private EventEmitter: NativeEventEmitter;
  constructor() {
    if (Platform.OS === "ios")
      this.EventEmitter = new NativeEventEmitter(NativeStepCounter);
    else this.EventEmitter = new NativeEventEmitter();
  }

  public queryPedometerDataBetweenDates(
    startDate: number,
    endDate: number,
    handler: (error: any, data: PedometerData) => void
  ) {
    NativeStepCounter.queryPedometerDataBetweenDates(
      startDate,
      endDate,
      handler
    );
  }

  public async queryPedometerDataBetweenDatesAsync(
    startDate: number,
    endDate: number
  ) {
    return new Promise((res, rej) => {
      NativeStepCounter.queryPedometerDataBetweenDates(
        startDate,
        endDate,
        (error, data: PedometerData) => {
          if (error) return rej(error);
          res(data);
        }
      );
    });
  }

  public startPedometerUpdatesFromDate(
    date: number,
    handler?: (data: PedometerData) => void
  ) {
    if (this.subscription) {
      this.stopPedometerUpdates();
    }
    NativeStepCounter.startPedometerUpdatesFromDate(date);
    if (handler) {
      this.subscription = this.EventEmitter.addListener(
        "stepCountChanged",
        handler
      );
    }
  }

  public stopPedometerUpdates() {
    NativeStepCounter.stopPedometerUpdates();
    if (this.subscription) {
      this.subscription.remove();
      this.subscription = undefined;
    }
  }

  private ios_authorizationStatus() {
    if (Platform.OS !== 'ios') return false;
    return new Promise((res, rej) => {
      NativeStepCounter.authorizationStatus(
        (
          error,
          status:
            | 'denied'
            | 'authorized'
            | 'restricted'
            | 'not_determined'
            | 'not_available'
        ) => {
          if (error) return rej(error);
          res(status);
        }
      );
    });
  }
}

const StepCounter = new Pedometer();

export default StepCounter;
