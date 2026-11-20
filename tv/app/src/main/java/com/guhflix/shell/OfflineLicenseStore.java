package com.guhflix.shell;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Offline grant cache. This class intentionally fails CLOSED until the
 * administrator's real RSA public key has been bundled in the APK asset:
 *   assets/gfilmes/license-public.pem
 *
 * Never trust the server response or a locally stored timestamp without
 * verifying its signature and device binding.
 */
public final class OfflineLicenseStore {
    private static final String PREFS = "guhflix-offline-grant-v1";
    private static final String ASSET = "gfilmes/license-public.pem";
    private static final long CLOCK_TOLERANCE = 120L;
    private static volatile String publicKey;

    private OfflineLicenseStore() {}

    private static String pinnedKey(Context ctx) {
        if (publicKey != null) return publicKey;
        try (InputStream input = ctx.getAssets().open(ASSET)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[2048];
            int n;
            while ((n = input.read(chunk)) >= 0) {
                if (buffer.size() + n > 8192) return null;
                buffer.write(chunk, 0, n);
            }
            String pem = buffer.toString("UTF-8");
            OfflinePermit.parsePublicKey(pem);
            publicKey = pem;
            return pem;
        } catch (Exception notProvisioned) {
            return null;
        }
    }

    public static boolean provisioned(Context ctx) {
        return pinnedKey(ctx) != null;
    }

    /** Check on every launch and before playing. Expired grants are never renewed locally. */
    public static synchronized boolean valid(Context ctx) {
        String pem = pinnedKey(ctx);
        if (pem == null) return false;
        SharedPreferences store = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String token = store.getString("grant", "");
        long now = System.currentTimeMillis() / 1000L;
        long last = store.getLong("highestObservedSeconds", 0L);
        try {
            OfflinePermit.verify(token, pem, Gate.installation(ctx), now, last);
            // Make clock rollback detectable on ordinary non-rooted TVs.
            return store.edit().putLong("highestObservedSeconds", Math.max(now, last)).commit();
        } catch (OfflinePermit.InvalidLicense invalid) {
            return false;
        }
    }

    /**
     * Optional best-effort retrieval after *online* successful login.
     * The server's /api/app/offline-license must have a real private key
     * and must verify the customer's active paid expiry.
     */
    public static synchronized boolean refresh(Context ctx) {
        String pem = pinnedKey(ctx);
        if (pem == null) return false;
        try {
            JSONObject response = Gate.request(ctx, "GET", "/api/app/offline-license", null, true);
            String received = response.getString("token");
            long now = System.currentTimeMillis() / 1000L;
            SharedPreferences store = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long last = store.getLong("highestObservedSeconds", 0L);
            OfflinePermit.verify(received, pem, Gate.installation(ctx), now, last);
            return store.edit().putString("grant", received)
                    .putLong("highestObservedSeconds", Math.max(last, now)).commit();
        } catch (Exception unavailable) {
            return false;
        }
    }

    /** Explicit logout or confirmed blocked/expired status revokes cached grant. */
    public static synchronized void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }

    /** Offline never overrides a confirmed account denial or wrong password. */
    public static boolean temporaryNetworkFailure(Gate.ApiError error) {
        return error != null && (error.status == 0 || error.status == 502
                || error.status == 503 || error.status == 504);
    }
}
