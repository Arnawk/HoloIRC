package com.fusionx.lightirc.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionx.lightirc.irc.Server;
import com.fusionx.lightirc.irc.ServerConfiguration;
import com.fusionx.lightirc.uiircinterface.IRCBridgeService;
import com.fusionx.lightirc.uiircinterface.MessageSender;

public class ServiceFragment extends Fragment {
    private IRCBridgeService mService;
    private ServiceFragmentCallback mCallback;
    private MessageSender sender;

    /**
     * This method will only be called once when the retained Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        sender = MessageSender.getSender(mCallback.getServerTitle());

        if (mService == null) {
            setUpService();
        }
    }

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
            mCallback = (ServiceFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " +
                    "ServiceFragmentCallback");
        }
        if (sender == null) {
            sender = MessageSender.getSender(mCallback.getServerTitle());
        }
        sender.getBus().register(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mService != null) {
            mService.setServerDisplayed(mCallback.getServerTitle());
        }
        sender.setDisplayed(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mService != null) {
            mService.setServerDisplayed(null);
        }
        sender.setDisplayed(false);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        sender.getBus().unregister(getActivity());
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getActivity().getApplicationContext().unbindService(mConnection);
        mService = null;
    }

    /**
     * This is a worker fragment so return null for the view always
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return null;
    }

    public void setUpService() {
        final Intent service = new Intent(getActivity(), IRCBridgeService.class);
        service.putExtra("server", true);
        service.putExtra("serverName", mCallback.getServerTitle());
        service.putExtra("stop", false);
        service.putExtra("setBound", mCallback.getServerTitle());

        getActivity().getApplicationContext().startService(service);
        getActivity().getApplicationContext().bindService(service, mConnection, 0);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder binder) {
            mService = ((IRCBridgeService.IRCBinder) binder).getService();
            mCallback.setUpViewPager();

            mService.setServerDisplayed(mCallback.getServerTitle());

            if (getServer(true, mCallback.getServerTitle()) != null) {
                mCallback.repopulateFragmentsInPager();
            } else {
                final ServerConfiguration.Builder builder =
                        getActivity().getIntent().getParcelableExtra("server");
                mService.connectToServer(builder);
            }
            mCallback.serverIsAvailable();
        }

        // Should never occur
        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mCallback.onDisconnect(false, false);
            throw new IllegalArgumentException();
        }
    };

    public Server getServer(final boolean nullAllowed, final String serverTitle) {
        Server server;
        if (mService == null || (server = mService.getServer(serverTitle)) == null) {
            if (nullAllowed) {
                return null;
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            return server;
        }
    }

    public void removeServiceReference(final String serverTitle) {
        mService.setServerDisplayed(null);
        mService.onDisconnect(serverTitle);
        mService = null;
    }

    public interface ServiceFragmentCallback {
        public void setUpViewPager();

        public String getServerTitle();

        public void repopulateFragmentsInPager();

        public void onDisconnect(final boolean expected, final boolean retryPending);

        public void serverIsAvailable();
    }
}