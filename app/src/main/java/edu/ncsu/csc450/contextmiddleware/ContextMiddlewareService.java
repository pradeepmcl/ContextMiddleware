package edu.ncsu.csc450.contextmiddleware;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ContextMiddlewareService extends Service {
    boolean getLocationUpdates = false;
    boolean notifyAtNewyork = false;
    IContextCallback notifyAtNewyorkCallback, locationUpdateCallback;
    private static final String TAG = "TESTGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "Failed to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "Failed to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "GPS provider does not exist " + ex.getMessage());
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("IRemote", "Bind Carrem");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
            try {
                if (location != null) {
                    if (getLocationUpdates) {
                        /*
                         * The callback function called here is implemented in the client application. The data sent back can be
                         * handled appropriately by changing the implementation on the application.
                         */
                        locationUpdateCallback.locationUpdateCallback(Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
                    }
                    if (notifyAtNewyork) {
                        Log.d("CARREM", "NY Notification: Invoked");
                        double cityThreshold = 20;
                        if (distanceFromCoordinates(location.getLatitude(), location.getLongitude()) < 20) {
                            /*
                             * Once the required location is detected, the client application is notified and the flag is reset.
                             */
                            notifyAtNewyorkCallback.notifyAtNewyorkCallback();
                            notifyAtNewyork = false;
                            Log.d("CARREM", "NY City: Detected");
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    /*
     * Initializing listeners for GPS and Network
     */
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    /*
    *   Implement functions in the interface.
    *   Since some functions and/or variables of the superclass might be required, it is better to call an external function from
    *   the interface.
     */
    private final IContextInterface.Stub mBinder = new IContextInterface.Stub() {

        /*
         * The void functions are asynchronous. Once these functions have been called, the Service completes execution and uses
         * the callback interface to communicate with the application.
         */
        public void registerForLocationUpdates(final IContextCallback callback) throws RemoteException {
            enableLocationUpdate(callback);
        }

        @Override
        public void notifyAtNewYork(final IContextCallback callback) throws RemoteException {
            enableNotificationatNewyork(callback);
        }

        /*
         * This function immediately returns the result without using a callback interface.
         * Avoid using functions that work synchronously if there is a possibility of delay in getting the required response.
         */
        public boolean isJackPluggedIn() {
            AudioManager audioManager = (AudioManager) ContextMiddlewareService.super.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            return audioManager.isWiredHeadsetOn();
        }

    };

    /*
     * enableLocationUpdate and enableNotificationatNewyork are used to set the flags and the callbacks.
     */
    void enableLocationUpdate(IContextCallback tCallback) {
        this.getLocationUpdates = true;
        this.locationUpdateCallback = tCallback;
    }

    void enableNotificationatNewyork(IContextCallback tCallback) {
        this.notifyAtNewyork = true;
        this.notifyAtNewyorkCallback = tCallback;
    }


    /*
     * Function to find the distance between given map coordinates.
     */
    private static double distanceFromCoordinates(double lat2, double lon2) {
        // New York City Coordinates
        double lat1 = 40.7128;
        double lon1 = -74.0059;
        double theta = lon1 - lon2;
        double dist = Math.sin(lat1 * Math.PI / 180.0) * Math.sin(lat2 * Math.PI / 180.0) + Math.cos(lat1 * Math.PI / 180.0) * Math.cos(lat2 * Math.PI / 180.0) * Math.cos(theta * Math.PI / 180.0);
        dist = Math.acos(dist);
        dist = dist * 180 / Math.PI;
        dist = dist * 60 * 1.1515;
        Log.d("Distance: ", Double.toString(dist));
        return (dist);
    }

}
