package com.example.sproutify.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    private final int NUM_PAGES = 2;

    public MainPagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Créer le fragment approprié selon la position
        return TracksFragment.newInstance(position == 1); // True si c'est l'onglet favoris (position 1)
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}
