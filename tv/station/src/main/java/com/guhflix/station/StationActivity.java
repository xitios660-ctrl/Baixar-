package com.guhflix.station;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Operator-TV proof of hardware-keystore signing.
 *
 * It deliberately DOES NOT host a public API, copy client credentials,
 * renew real accounts, or accept arbitrary remote commands. A secure
 * authenticated service and account synchronization are still required.
 */
public final class StationActivity extends Activity {
    private static final String ALIAS = "gfilmes-station-rsa-sha256-v1";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status, details;
    private volatile String exportedPublicPem;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(8, 9, 14));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(45, 32, 45, 32);
        scroll.addView(column);
        setContentView(scroll);

        TextView title = text("GFILMES CENTRAL", 34);
        title.setTextColor(Color.rgb(237, 25, 55));
        column.addView(title);
        column.addView(text("DESENVOLVIMENTO • Ainda não é uma central pública de ativações.", 18));
        status = text("Preparando chave privada protegida pelo Android…", 19);
        column.addView(status);
        details = text("A chave privada não deve sair desta TV.\n"
                + "Os clientes não podem ativar nem renovar por este protótipo.\n"
                + "Faltam autenticação do administrador, sincronização do cadastro e HTTPS.", 17);
        column.addView(details);
        button(column, "COPIAR CHAVE PÚBLICA", () -> {
            if (exportedPublicPem == null) return;
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                    ClipData.newPlainText("Gfilmes station public RSA key", exportedPublicPem));
            status.setText("Chave pública copiada. A chave privada permaneceu na TV.");
        });
        button(column, "COMPARTILHAR CHAVE PÚBLICA", () -> {
            if (exportedPublicPem == null) return;
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, exportedPublicPem);
            startActivity(Intent.createChooser(share, "Exportar apenas a chave pública"));
        });
        if (BuildConfig.DEBUG) {
            button(column, "GERAR LICENÇA FICTÍCIA PARA TESTE", this::signDemo);
        }
        worker.execute(() -> {
            try {
                KeyPair pair = keyPair();
                String pub = "-----BEGIN PUBLIC KEY-----\n"
                        + Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                        .encodeToString(pair.getPublic().getEncoded())
                        + "\n-----END PUBLIC KEY-----";
                exportedPublicPem = pub;
                String fingerprint = toHex(MessageDigest.getInstance("SHA-256")
                        .digest(pair.getPublic().getEncoded()));
                runOnUiThread(() -> {
                    status.setText("Chave RSA-2048 protegida no Android Keystore.");
                    details.setText("Impressão digital da chave pública:\n" + fingerprint
                            + "\n\nAtenção: esta edição é apenas para testar o assinador."
                            + "\nNão distribua licenças fictícias a clientes.");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("Não foi possível preparar a chave no dispositivo."));
            }
        });
    }

    private void signDemo() {
        if (!BuildConfig.DEBUG) return;
        worker.execute(() -> {
            try {
                long now = System.currentTimeMillis() / 1000L;
                String token = LicenseIssuer.issue(keyPair().getPrivate(),
                        "0123456789abcdef01234567",
                        "11111111-2222-3333-4444-555555555555",
                        now, now + 86400L);
                runOnUiThread(() -> {
                    status.setText("Licença FICTÍCIA gerada para testar assinatura.");
                    details.setText("Não funciona em contas reais:\n" + token);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("Falha ao gerar a licença de teste."));
            }
        });
    }

    private static KeyPair keyPair() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) {
            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) store.getEntry(ALIAS, null);
            return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setKeySize(2048)
                .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(2048,
                        RSAKeyGenParameterSpec.F4))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build();
        generator.initialize(spec);
        return generator.generateKeyPair();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) out.append(String.format("%02x", item & 0xff));
        return out.toString();
    }

    private TextView text(String content, int size) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(0, 12, 0, 17);
        return view;
    }
    private void button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(170, 15, 39)));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 10;
        parent.addView(button, params);
    }
    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
