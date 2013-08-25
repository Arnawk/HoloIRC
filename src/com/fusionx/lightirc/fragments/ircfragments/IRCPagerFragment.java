package com.fusionx.lightirc.fragments.ircfragments;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.fusionx.irc.ChannelUser;
import com.fusionx.irc.Server;
import com.fusionx.lightirc.R;
import com.fusionx.lightirc.adapters.IRCPagerAdapter;
import com.fusionx.lightirc.misc.FragmentType;

import java.util.ArrayList;

public class IRCPagerFragment extends Fragment implements ServerFragment.ServerFragmentCallback,
        ChannelFragment.ChannelFragmentCallback, UserFragment.UserFragmentCallbacks {
    private ViewPager mViewPager = null;
    private IRCPagerInterface mCallback = null;
    private IRCPagerAdapter mAdapter = null;

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (IRCPagerInterface) activity;
        } catch (ClassCastException ex) {
            throw new ClassCastException(activity.toString() + " must implement IRCPagerInterface");
        }
    }

    /**
     * Since the fragment is retained, when the activity detaches, a new activity is created so
     * null the callback when the old activity detaches
     */
    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    /**
     * Retain the fragment through config changes
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    /**
     * Create the view by inflating a generic view pager
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_pager, container);
    }

    /**
     * Creates the ServerFragment object
     */
    public void createServerFragment() {
        if (mAdapter == null) {
            mAdapter = new IRCPagerAdapter(getChildFragmentManager());
            mAdapter.addServerFragment(mCallback.getServerTitle());
        }
        final TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[]
                {android.R.attr.windowBackground});
        final int background = a.getResourceId(0, 0);
        mViewPager = (ViewPager) getView().findViewById(R.id.pager);
        mViewPager.setBackgroundResource(background);
        mViewPager.setAdapter(mAdapter);
    }

    /**
     * Get the currently displayed fragment
     *
     * @return - returns the currently displayed fragment
     */
    private IRCFragment getCurrentItem() {
        return mAdapter.getItem(mViewPager.getCurrentItem());
    }

    /**
     * Creates a UserFragment with the specified nick
     *
     * @param userNick - the nick of the user we are PMing
     */
    public void createPMFragment(final String userNick) {
        final UserFragment userFragment = new UserFragment();
        final Bundle bundle = new Bundle();
        bundle.putString("title", userNick);
        userFragment.setArguments(bundle);

        final int position = mAdapter.addFragment(userFragment);

        mViewPager.setCurrentItem(position, true);
    }

    /**
     * Selects the ServerFragment regardless of what is currently selected in the ViewPager
     */
    public void selectServerFragment() {
        mViewPager.setCurrentItem(0, true);
    }

    /**
     * If the currently displayed fragment is the one being removed then switch
     * to one tab back. Then remove the fragment regardless.
     *
     * @param fragmentTitle - name of the fragment to be removed
     */
    public void switchFragmentAndRemove(final String fragmentTitle) {
        final int index = mAdapter.getIndexFromTitle(fragmentTitle);
        if (fragmentTitle.equals(getCurrentTitle())) {
            mViewPager.setCurrentItem(index - 1, true);
        }
        mAdapter.removeFragment(index);
    }

    /**
     * Method called when a new ChannelFragment is to be created
     *
     * @param channelName - name of the channel joined
     * @param forceSwitch - whether the channel should be forcibly switched to
     */
    public void createChannelFragment(final String channelName, final boolean forceSwitch) {
        final boolean switchToTab = channelName.equals(getActivity().getIntent().getStringExtra
                ("mention")) || forceSwitch;

        final ChannelFragment channel = new ChannelFragment();
        final Bundle bundle = new Bundle();
        bundle.putString("title", channelName);

        channel.setArguments(bundle);

        final int position = mAdapter.addFragment(channel);

        if (switchToTab) {
            mViewPager.setCurrentItem(position, true);
        }
    }

    /**
     * Get the handler object to send the message to.
     *
     * @param destination - the title of the tab we are trying to get
     * @param type        - the type of the fragment we are trying to get the handler from
     * @return - the handler object we are trying to get
     */
    public Handler getFragmentHandler(final String destination, final FragmentType type) {
        final String nonNullDestination = destination != null ? destination : mCallback
                .getServerTitle();
        if (mAdapter != null) {
            final IRCFragment fragment = mAdapter.getFragment(nonNullDestination,
                    type);
            return fragment == null ? null : fragment.getHandler();
        } else {
            return null;
        }
    }

    public void onMentionRequested(final ArrayList<ChannelUser> users) {
        if (getCurrentType().equals(FragmentType.Channel)) {
            final ChannelFragment channel = (ChannelFragment) getCurrentItem();
            channel.onUserMention(users);
        }
    }

    public void onUnexpectedDisconnect() {
        mViewPager.setCurrentItem(0, true);

        mAdapter.removeAllButServer();
        mAdapter.disableAllEditTexts();
    }

    public String getCurrentTitle() {
        return getCurrentItem().getTitle();
    }

    public FragmentType getCurrentType() {
        return getCurrentItem().getType();
    }

    public void setCurrentItemIndex(final int position) {
        mAdapter.setCurrentItemIndex(position);
    }

    public void setTabStrip(PagerSlidingTabStrip tabs) {
        tabs.setViewPager(mViewPager);
        mAdapter.setTabStrip(tabs);
    }

    @Override
    public void updateUserList(String channelName) {
        mCallback.updateUserList(channelName);
    }

    @Override
    public Server getServer(boolean nullAllowed) {
        return mCallback.getServer(nullAllowed);
    }

    public void connectedToServer() {
        final ServerFragment fragment = (ServerFragment) mAdapter.getFragment(mCallback
                .getServerTitle(), FragmentType.Server);
        fragment.onConnectedToServer();
    }

    public void writeMessageToServer(final String message) {
        final ServerFragment fragment = (ServerFragment) mAdapter.getFragment(mCallback
                .getServerTitle(), FragmentType.Server);
        if (fragment != null) {
            fragment.appendToTextView(message + "\n");
        }
    }

    public interface IRCPagerInterface {
        public String getServerTitle();

        public void updateUserList(String channelName);

        public Server getServer(boolean nullAllowed);
    }
}