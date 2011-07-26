package org.kontalk.service;

import java.util.List;

import org.kontalk.authenticator.Authenticator;
import org.kontalk.client.AbstractMessage;
import org.kontalk.client.EndpointServer;
import org.kontalk.client.PollingClient;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

/**
 * The polling thread.
 * @author Daniele Ricci
 * @version 1.0
 */
public class PollingThread extends Thread {
    private final static String TAG = PollingThread.class.getSimpleName();

    private final Context mContext;
    private final EndpointServer mServer;
    private String mAuthToken;
    private boolean mRunning;

    private PollingClient mClient;
    private MessageListener mListener;

    public PollingThread(Context context, EndpointServer server) {
        mServer = server;
        mContext = context;
    }

    public void setMessageListener(MessageListener listener) {
        this.mListener = listener;
    }

    public void run() {
        mRunning = true;
        mAuthToken = Authenticator.getDefaultAccountToken(mContext);
        Log.i(TAG, "using token: " + mAuthToken);

        Account acc = Authenticator.getDefaultAccount(mContext);
        Log.d(TAG, "using account name " + acc.name + " as my number");
        mClient = new PollingClient(mContext, mServer, mAuthToken, acc.name);

        while(mRunning) {
            try {
                List<AbstractMessage<?>> list = mClient.poll();
                if (list != null) {
                    Log.i("PollingThread", list.toString());

                    if (mListener != null)
                        mListener.incoming(list);
                }

                // success - wait just 1s
                if (mRunning)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
            }
            catch (Exception e) {
                Log.e(TAG, "polling error", e);
                // error - wait longer - 5s
                if (mRunning)
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {}
            }
        }
    }

    /**
     * Shuts down this polling thread gracefully.
     */
    public synchronized void shutdown() {
        Log.w(TAG, "shutting down");
        mRunning = false;
        if (mClient != null)
            mClient.abort();
        interrupt();
        // do not join - just discard the thread

        Log.w(TAG, "exiting");
        mClient = null;
    }
}
