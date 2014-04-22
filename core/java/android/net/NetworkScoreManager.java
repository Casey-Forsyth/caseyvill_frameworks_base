/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.net;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

/**
 * Class that manages communication between network subsystems and a network scorer.
 *
 * <p>You can get an instance of this class by calling
 * {@link android.content.Context#getSystemService(String)}:
 *
 * <pre>NetworkScoreManager manager =
 *     (NetworkScoreManager) getSystemService(Context.NETWORK_SCORE_SERVICE)</pre>
 *
 * <p>A network scorer is any application which:
 * <ul>
 * <li>Declares the {@link android.Manifest.permission#SCORE_NETWORKS} permission.
 * <li>Includes a receiver for {@link #ACTION_SCORE_NETWORKS} guarded by the
 *     {@link android.Manifest.permission#BROADCAST_SCORE_NETWORKS} permission which scores networks
 *     and (eventually) calls {@link #updateScores} with the results.
 * </ul>
 *
 * <p>The system keeps track of a default scorer application; at any time, only this application
 * will receive {@link #ACTION_SCORE_NETWORKS} broadcasts and will be permitted to call
 * {@link #updateScores}. Applications may determine the current default scorer with
 * {@link #getActiveScorerPackage()} and request to change the default scorer by sending an
 * {@link #ACTION_CHANGE_DEFAULT} broadcast with another scorer.
 *
 * @hide
 */
public class NetworkScoreManager {
    /**
     * Activity action: ask the user to change the default network scorer. This will show a dialog
     * that asks the user whether they want to replace the current default scorer with the one
     * specified in {@link #EXTRA_PACKAGE_NAME}. The activity will finish with RESULT_OK if the
     * default was changed or RESULT_CANCELED if it failed for any reason.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CHANGE_DEFAULT = "android.net.scoring.CHANGE_DEFAULT";

    /**
     * Extra used with {@link #ACTION_CHANGE_DEFAULT} to specify the new scorer package. Set with
     * {@link android.content.Intent#putExtra(String, String)}.
     */
    public static final String EXTRA_PACKAGE_NAME = "packageName";

    /**
     * Broadcast action: new network scores are being requested. This intent will only be delivered
     * to the current default scorer app. That app is responsible for scoring the networks and
     * calling {@link #updateScores} when complete. The networks to score are specified in
     * {@link #EXTRA_NETWORKS_TO_SCORE}, and will generally consist of all networks which have been
     * configured by the user as well as any open networks.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_SCORE_NETWORKS = "android.net.scoring.SCORE_NETWORKS";

    /**
     * Extra used with {@link #ACTION_SCORE_NETWORKS} to specify the networks to be scored, as an
     * array of {@link NetworkKey}s. Can be obtained with
     * {@link android.content.Intent#getParcelableArrayExtra(String)}}.
     */
    public static final String EXTRA_NETWORKS_TO_SCORE = "networksToScore";

    private final Context mContext;
    private final INetworkScoreService mService;

    /** @hide */
    public NetworkScoreManager(Context context) {
        mContext = context;
        IBinder iBinder = ServiceManager.getService(Context.NETWORK_SCORE_SERVICE);
        mService = INetworkScoreService.Stub.asInterface(iBinder);
    }

    /**
     * Obtain the package name of the current active network scorer.
     *
     * <p>At any time, only one scorer application will receive {@link #ACTION_SCORE_NETWORKS}
     * broadcasts and be allowed to call {@link #updateScores}. Applications may use this method to
     * determine the current scorer and offer the user the ability to select a different scorer via
     * the {@link #ACTION_CHANGE_DEFAULT} intent.
     * @return the full package name of the current active scorer, or null if there is no active
     *     scorer.
     */
    public String getActiveScorerPackage() {
        return NetworkScorerAppManager.getActiveScorer(mContext);
    }

    /**
     * Update network scores.
     *
     * <p>This may be called at any time to re-score active networks. Scores will generally be
     * updated quickly, but if this method is called too frequently, the scores may be held and
     * applied at a later time.
     *
     * @param networks the networks which have been scored by the scorer.
     * @return whether the update was successful.
     * @throws SecurityException if the caller is not the active scorer.
     */
    public boolean updateScores(ScoredNetwork[] networks) throws SecurityException {
        try {
            return mService.updateScores(networks);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Clear network scores.
     *
     * <p>Should be called when all scores need to be invalidated, i.e. because the scoring
     * algorithm has changed and old scores can no longer be compared to future scores.
     *
     * <p>Note that scores will be cleared automatically when the active scorer changes, as scores
     * from one scorer cannot be compared to those from another scorer.
     *
     * @return whether the clear was successful.
     * @throws SecurityException if the caller is not the active scorer or privileged.
     */
    public boolean clearScores() throws SecurityException {
        try {
            return mService.clearScores();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Set the active scorer to a new package and clear existing scores.
     *
     * @return true if the operation succeeded, or false if the new package is not a valid scorer.
     * @throws SecurityException if the caller does not hold the
     *      {@link android.Manifest.permission#BROADCAST_SCORE_NETWORKS} permission indicating that
     *      it can manage scorer applications.
     * @hide
     */
    public boolean setActiveScorer(String packageName) throws SecurityException {
        try {
            return mService.setActiveScorer(packageName);
        } catch (RemoteException e) {
            return false;
        }
    }
}