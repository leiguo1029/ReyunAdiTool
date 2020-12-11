// IRecvDataService.aidl
package com.fear1ess.reyunaditool;

// Declare any non-default types here with import statements

interface IDoCommandService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

   // void handleData(int cmd, in Map args);
    String doCommand(int cmd, String args);
}