package com.example.mealshare.HomePage;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MealShareAdapter extends FragmentStateAdapter {

    public MealShareAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch(position){
            case 0:
                return new AllFragment();
            case 1:
                return new NearMeFragment();
            case 2:
                return new EndingSoonBlankFragment();
            case 3:
                return new VegeFragment();
            default:
                return new AllFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
