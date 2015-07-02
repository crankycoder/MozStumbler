/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.mozstumbler.service.stumblerthread.scanners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.mozilla.mozstumbler.service.Prefs;
import org.mozilla.mozstumbler.svclocator.services.log.LoggerUtil;

import java.util.List;

public class BaseWifiManagerProxy extends BroadcastReceiver {
    static final String LOG_TAG = LoggerUtil.makeLogTag(BaseWifiManagerProxy.class);

    final Context mAppContext;
    WifiScanner mWifiScanner;

    public BaseWifiManagerProxy(Context appContext) {
        mAppContext = appContext;
    }

    public boolean isScanEnabled() {
        WifiManager manager = getWifiManager();
        boolean scanEnabled = manager.isWifiEnabled();
        if (Build.VERSION.SDK_INT >= 18) {
            scanEnabled |= manager.isScanAlwaysAvailable();
        }
        return scanEnabled;
    }

    public boolean runWifiScan() {
        if (Prefs.getInstance(mAppContext).isSimulateStumble()) {

            // This intent will signal the WifiScanner class to ask for new scan results
            // by invoking getScanResults
            Intent i = new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            onReceive(mAppContext, i);
            return true;
        } else {
            return getWifiManager().startScan();
        }
    }

    public List<ScanResult> getScanResults() {

        WifiManager manager = getWifiManager();
        if (manager == null) {
            return null;
        }
        return getWifiManager().getScanResults();

    }

    private WifiManager getWifiManager() {
        return (WifiManager) mAppContext.getSystemService(Context.WIFI_SERVICE);
    }

    public synchronized void registerReceiver(WifiScanner wifiScanner) {
        mWifiScanner = wifiScanner;
    }

    public void unregisterReceiver() {
        try {
            mAppContext.unregisterReceiver(this);
        } catch (IllegalArgumentException ex) {
            // doesn't matter - this is safe to ignore as it just means that
            // we've just been running in simulation mode.
        }
    }

    public void onReceive(Context c, Intent intent) {
        // this is the hook we need to call to send in fake
        // Wifi signals.
        // WifiScanner will expect and intent with
        // action ==  WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
        // and the getScanResults() method will then be called to return a list of
        // Wifi scans.
        mWifiScanner.onProxyReceive(c, intent);
    }

    public WifiManager.WifiLock createWifiLock() {
        return getWifiManager().createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "MozStumbler");
    }

}
