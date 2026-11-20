package com.guhflix.station;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

/**
 * Signing-only core for a trusted operator station (never compile this into
 * a customer APK). The caller MUST have authenticated the operator, verified
 * that the client account is enabled, and checked the paid subscription.
 *
 * This class deliberately has no network endpoint or payment shortcuts.
 */
public final class LicenseIssuer {
    public static final long MAX_DURATION_SECONDS = 30L * 24 * 3600;
    private LicenseIssuer() {}

    public static String issue(PrivateKey key, String userId, String installationId,
                               long nowSeconds, long paidUntilSeconds) throws Exception {
        if (key == null || !"RSA".equalsIgnoreCase(key.getAlgorithm()))
            throw new IllegalArgumentException("Signing key is not provisioned.");
        if (userId == null || !userId.matches("[a-f0-9]{24}")
                || installationId == null ||
                !installationId.matches("[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"))
            throw new IllegalArgumentException("Invalid customer or installation.");
        if (nowSeconds < 1000000000L || paidUntilSeconds <= nowSeconds)
            throw new IllegalArgumentException("No active paid entitlement.");
        long expires = Math.min(paidUntilSeconds, Math.addExact(nowSeconds, MAX_DURATION_SECONDS));
        String payload = "1|" + userId + "|" + installationId + "|" + nowSeconds
                + "|" + nowSeconds + "|" + expires + "|" + UUID.randomUUID();
        String prefix = "GFL1." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(prefix.getBytes(StandardCharsets.US_ASCII));
        return prefix + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signer.sign());
    }
}
