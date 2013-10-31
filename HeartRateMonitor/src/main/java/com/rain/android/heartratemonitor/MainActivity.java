package com.rain.android.heartratemonitor;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.rain.android.heartratemonitor.fragments.HeartRateFragment;
import com.rain.android.heartratemonitor.fragments.LogFragment;

public class MainActivity extends Activity implements HeartRateFragment.OnHeartRateListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private LogFragment mLogFragment;
    private HeartRateFragment mHeartRateFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
    }

    @Override
    public void onListening() {

        if (mLogFragment != null) {
            mLogFragment.clearLog();
        }

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch(position) {
                case 1:

                    if (mLogFragment == null) {
                        mLogFragment = LogFragment.newInstance();
                    }

                    return mLogFragment;
                default:

                    if (mHeartRateFragment == null) {
                        mHeartRateFragment = HeartRateFragment.newInstance(MainActivity.this);
                    }

                    return mHeartRateFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
