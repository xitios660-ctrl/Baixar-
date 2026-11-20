package com.guhflix.shell;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

/**
 * Signed, read-only access grant for a single Android TV installation.
 *
 * The issuing service keeps the RSA private key; the APK only receives its
 * pinned public key. No customer password or shared secret belongs here.
 *
 * Wire format:
 *   GFL1.base64url("1|userObjectId|installationUuid|iat|nbf|exp|nonceUuid")
 *       .base64url(RS256("GFL1." + base64url(payload)))
 *
 * Seconds since Unix epoch. Expiration is never later than the subscription's
 * paid expiry, and never more than 30 days after issuance.
 */
public final class OfflinePermit {
    public static final long MAX_SECONDS = 30L * 24 * 3600;
    private static final long SKEW_SECONDS = 120L;
    private static final Pattern OBJECT_ID = Pattern.compile("[a-f0-9]{24}");
    private static final Pattern UUID = Pattern.compile("[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}");

    public static final class Grant {
        public final String userId;
        public final String installationId;
        public final long issuedAt, notBefore, expiresAt;
        public final String serial;
        private Grant(String uid, String device, long issued, long nbf, long exp, String serial) {
            this.userId = uid; this.installationId = device;
            this.issuedAt = issued; this.notBefore = nbf;
            this.expiresAt = exp; this.serial = serial;
        }
    }

    public static final class InvalidLicense extends Exception {
        public InvalidLicense(String message) { super(message); }
    }

    private OfflinePermit() {}

    /**
     * lastObservedSeconds is the highest wall-clock time previously observed.
     * Store it in device-protected local storage and update it on every use.
     * This detects ordinary clock rollback, but a rooted TV is not tamper-proof.
     */
    public static Grant verify(String token, String publicKeyPem, String expectedInstallation,
                               long nowSeconds, long lastObservedSeconds) throws InvalidLicense {
        try {
            if (token == null || token.length() > 4096 || token.length() < 128
                    || expectedInstallation == null || !UUID.matcher(expectedInstallation).matches()) {
                throw new InvalidLicense("Licença ausente ou inválida.");
            }
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !"GFL1".equals(parts[0]) || parts[1].length() > 1024) {
                throw new InvalidLicense("Formato de licença desconhecido.");
            }
            byte[] raw = decode(parts[1]);
            String payload = new String(raw, StandardCharsets.UTF_8);
            if (!java.util.Arrays.equals(raw, payload.getBytes(StandardCharsets.UTF_8))) {
                throw new InvalidLicense("Conteúdo inválido.");
            }
            String[] fields = payload.split("\\|", -1);
            if (fields.length != 7 || !"1".equals(fields[0])
                    || !OBJECT_ID.matcher(fields[1]).matches()
                    || !UUID.matcher(fields[2]).matches()
                    || !UUID.matcher(fields[6]).matches()) {
                throw new InvalidLicense("Dados de licença inválidos.");
            }
            long iat = parseSeconds(fields[3]), nbf = parseSeconds(fields[4]);
            long exp = parseSeconds(fields[5]);
            if (!expectedInstallation.equalsIgnoreCase(fields[2])
                    || nbf < iat - SKEW_SECONDS || nbf > iat + SKEW_SECONDS
                    || exp <= iat || exp - iat > MAX_SECONDS) {
                throw new InvalidLicense("Licença incompatível com esta instalação.");
            }

            PublicKey key = parsePublicKey(publicKeyPem);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(("GFL1." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(decode(parts[2]))) {
                throw new InvalidLicense("Assinatura inválida.");
            }

            if (nowSeconds < 0 || lastObservedSeconds < 0
                    || nowSeconds + SKEW_SECONDS < lastObservedSeconds
                    || nowSeconds + SKEW_SECONDS < iat
                    || nowSeconds + SKEW_SECONDS < nbf
                    || nowSeconds >= exp) {
                throw new InvalidLicense("Licença vencida ou relógio da TV inválido.");
            }
            return new Grant(fields[1], fields[2], iat, nbf, exp, fields[6]);
        } catch (InvalidLicense e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidLicense("Não foi possível verificar a licença assinada.");
        }
    }

    public static PublicKey parsePublicKey(String pem) throws Exception {
        if (pem == null || !pem.contains("-----BEGIN PUBLIC KEY-----")
                || !pem.contains("-----END PUBLIC KEY-----")) {
            throw new IllegalArgumentException("Public key not configured");
        }
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(decode(body)));
        if (!(key instanceof RSAPublicKey)
                || ((RSAPublicKey) key).getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("Weak signing key");
        }
        return key;
    }

    private static long parseSeconds(String input) throws InvalidLicense {
        if (input.length() < 9 || input.length() > 11 || !input.matches("[0-9]+")) {
            throw new InvalidLicense("Carimbo de data inválido.");
        }
        try { return Long.parseLong(input); }
        catch (NumberFormatException e) { throw new InvalidLicense("Carimbo de data inválido."); }
    }

    /** Base64 and base64url, implemented without java.util.Base64 (Android API 23). */
    private static byte[] decode(String encoded) throws InvalidLicense {
        if (encoded.length() == 0 || encoded.length() > 8192)
            throw new InvalidLicense("Codificação inválida.");
        String value = encoded.replaceAll("=+$", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length() * 3 / 4);
        int bits = 0, buffer = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            int val;
            if (c >= 'A' && c <= 'Z') val = c - 'A';
            else if (c >= 'a' && c <= 'z') val = c - 'a' + 26;
            else if (c >= '0' && c <= '9') val = c - '0' + 52;
            else if (c == '-' || c == '+') val = 62;
            else if (c == '_' || c == '/') val = 63;
            else throw new InvalidLicense("Codificação inválida.");
            buffer = (buffer << 6) | val;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xff);
                buffer &= (1 << bits) - 1;
            }
        }
        if (bits >= 6 || buffer != 0) throw new InvalidLicense("Codificação inválida.");
        return out.toByteArray();
    }
}
