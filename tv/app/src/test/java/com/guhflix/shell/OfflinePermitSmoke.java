package com.guhflix.shell;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

/** Zero-dependency cryptographic contract tests. Execute with javac/java. */
public final class OfflinePermitSmoke {
    private static final String USER = "0123456789abcdef01234567";
    private static final String DEVICE = "11111111-2222-3333-4444-555555555555";
    private static int verified = 0;

    private static String token(KeyPair key, long iat, long exp, String device) throws Exception {
        String payload = "1|" + USER + "|" + device + "|" + iat + "|" + iat
                + "|" + exp + "|" + UUID.randomUUID();
        String header = "GFL1." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key.getPrivate());
        signer.update(header.getBytes(StandardCharsets.US_ASCII));
        return header + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signer.sign());
    }

    private static String publicPem(KeyPair key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(key.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private static void rejects(String scenario, RunnableCheck check) throws Exception {
        try { check.run(); throw new AssertionError("Accepted invalid grant: " + scenario); }
        catch (OfflinePermit.InvalidLicense expected) { verified++; }
    }
    @FunctionalInterface interface RunnableCheck { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair key = generator.generateKeyPair();
        KeyPair other = generator.generateKeyPair();
        long now = 1_795_100_000L;
        long day = 86400L;
        String pem = publicPem(key);
        String good = token(key, now, now + 30 * day, DEVICE);
        OfflinePermit.Grant g = OfflinePermit.verify(good, pem, DEVICE, now + 5 * day, now + 4 * day);
        if (!USER.equals(g.userId) || g.expiresAt != now + 30 * day)
            throw new AssertionError("Valid grant metadata differs");
        verified++;

        rejects("foreign installation", () -> OfflinePermit.verify(good, pem,
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", now + day, now));
        rejects("foreign signer", () -> OfflinePermit.verify(good, publicPem(other),
                DEVICE, now + day, now));
        String[] parts = good.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." +
                parts[2].substring(0, 10) + (parts[2].charAt(10) == 'A' ? "B" : "A")
                + parts[2].substring(11);
        rejects("modified signature", () -> OfflinePermit.verify(tampered, pem,
                DEVICE, now + day, now));
        rejects("time expired", () -> OfflinePermit.verify(good, pem,
                DEVICE, now + 30 * day, now + 29 * day));
        rejects("more than 30 days", () -> OfflinePermit.verify(
                token(key, now, now + 31 * day, DEVICE), pem, DEVICE, now + day, now));
        rejects("time rollback", () -> OfflinePermit.verify(good, pem,
                DEVICE, now + day, now + 4 * day));
        rejects("before issue", () -> OfflinePermit.verify(good, pem,
                DEVICE, now - day, 0));
        rejects("corrupt token", () -> OfflinePermit.verify("GFL1.%%%.notvalid", pem,
                DEVICE, now, now));
        rejects("missing key", () -> OfflinePermit.verify(good, "", DEVICE, now, now));
        rejects("wrong kind of token", () -> OfflinePermit.verify(
                good.replaceFirst("GFL1", "GFL2"), pem, DEVICE, now + day, now));
        System.out.println("PASS: " + verified + " independent offline-license assertions");
    }
}
