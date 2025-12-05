package com.example.mealshare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// You will need to add the following dependency to your build.gradle (module-level) if you haven't already:
// implementation 'androidx.lifecycle:lifecycle-extensions:2.2.0'
// OR the newer:
// implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
// implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'


public class SharedViewModel extends ViewModel {

    // MutableLiveData: Used internally in the ViewModel for changing the data value.
    // Initialized to the 'Fetching location...' status, which HomeFragment will display first.
    private final MutableLiveData<String> liveLocation = new MutableLiveData<>("Fetching location...");

    // LiveData: Exposed publicly. Fragments observe this read-only version.
    public LiveData<String> getLocation() {
        return liveLocation;
    }

    /**
     * Called by HomeFragment to deposit the new, fetched location address.
     * This automatically notifies all observing fragments (like AddFragment).
     * @param address The human-readable address string (e.g., "17, SS 2/73, Petaling Jaya").
     */
    public void setLocation(String address) {
        liveLocation.setValue(address);
    }
}