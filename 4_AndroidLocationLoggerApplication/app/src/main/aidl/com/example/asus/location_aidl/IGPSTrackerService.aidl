// IGPSTrackerService.aidl
package com.example.asus.location_aidl;



// Declare any non-default types here with import statements

interface IGPSTrackerService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   // void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
    //        double aDouble, String aString);

    double getLatitude();
    double getLongitude();
    float getDistance();
    float getSpeed();
}
