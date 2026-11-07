package com.guhflix.shell;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class AccessActivity extends Activity {
    static final ExecutorService updateWork = Executors.newSingleThreadExecutor();
    boolean busy;
    TextView message;
    LinearLayout panel;
    EditText password;
    EditText user;
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable check = new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            AccessActivity.this.m12lambda$new$0$comguhflixshellAccessActivity();
        }
    };

    void m12lambda$new$0$comguhflixshellAccessActivity(){ m20lambda$validate$8$comguhflixshellAccessActivity(); }
    void m18lambda$validate$6$comguhflixshellAccessActivity(){ m21lambda$validate$9$comguhflixshellAccessActivity(); }
    void m1lambda$blocked$13$comguhflixshellAccessActivity(){ m20lambda$validate$8$comguhflixshellAccessActivity(); }
    void m2lambda$blocked$14$comguhflixshellAccessActivity(){ m21lambda$validate$9$comguhflixshellAccessActivity(); }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setFlags(1024, 1024);
        render("Verificando seu acesso…", false);
        m20lambda$validate$8$comguhflixshellAccessActivity();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.handler.post(this.check);
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.handler.removeCallbacks(this.check);
        super.onPause();
    }

    int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    void render(String str, boolean z) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{-13367284, -16250872, -16250872}));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(17);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(44.0f), dp(26.0f), dp(44.0f), dp(26.0f));
        scrollView.addView(linearLayout);
        this.panel = new LinearLayout(this);
        this.panel.setOrientation(1);
        this.panel.setPadding(dp(32.0f), dp(26.0f), dp(32.0f), dp(26.0f));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-233828334);
        gradientDrawable.setCornerRadius(dp(8.0f));
        gradientDrawable.setStroke(dp(1.0f), -13619148);
        this.panel.setBackground(gradientDrawable);
        linearLayout.addView(this.panel, new LinearLayout.LayoutParams(dp(570.0f), -2));
        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(new GuhflixUi.Wordmark());
        imageView.setContentDescription("GUHFLIX");
        imageView.setScaleType(ImageView.ScaleType.FIT_START);
        this.panel.addView(imageView, new LinearLayout.LayoutParams(dp(300.0f), dp(76.0f)));
        TextView textView = new TextView(this);
        textView.setText(z ? "SUA PRÓXIMA HISTÓRIA COMEÇA AQUI" : "SUA CONTA • GUHFLIX");
        textView.setTextColor(-4342339);
        textView.setTextSize(11.0f);
        textView.setLetterSpacing(0.16f);
        textView.setPadding(0, dp(8.0f), 0, dp(4.0f));
        this.panel.addView(textView);
        this.message = new TextView(this);
        this.message.setText(str);
        this.message.setTextColor(-1);
        this.message.setTextSize(19.0f);
        this.message.setPadding(0, dp(12.0f), 0, dp(20.0f));
        this.panel.addView(this.message);
        if (z) {
            this.user = new EditText(this);
            this.user.setSingleLine(true);
            this.user.setBackgroundTintList(ColorStateList.valueOf(-1767148));
            this.user.setHint("Usuário");
            this.user.setTextColor(-1);
            this.user.setHintTextColor(-5592406);
            this.user.setInputType(145);
            styleField(this.user);
            this.panel.addView(this.user, new LinearLayout.LayoutParams(-1, dp(54.0f)));
            this.password = new EditText(this);
            this.password.setSingleLine(true);
            this.password.setBackgroundTintList(ColorStateList.valueOf(-1767148));
            this.password.setHint("Senha");
            this.password.setTextColor(-1);
            this.password.setHintTextColor(-5592406);
            this.password.setInputType(129);
            styleField(this.password);
            this.panel.addView(this.password, new LinearLayout.LayoutParams(-1, dp(54.0f)));
            button("ENTRAR", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m13lambda$render$1$comguhflixshellAccessActivity();
                }
            });
            this.user.requestFocus();
        }
        setContentView(scrollView);
        this.panel.setAlpha(0.0f);
        this.panel.setTranslationY(dp(12.0f));
        this.panel.animate().alpha(1.0f).translationY(0.0f).setDuration(350L).start();
    }

    void styleField(EditText editText) {
        editText.setTextSize(18.0f);
        editText.setPadding(dp(18.0f), 0, dp(18.0f), 0);
        StateListDrawable stateListDrawable = new StateListDrawable();
        boolean[] zArr = {true, false};
        for (int i = 0; i < 2; i++) {
            boolean z = zArr[i];
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(z ? -13619148 : -14671837);
            gradientDrawable.setCornerRadius(dp(4.0f));
            gradientDrawable.setStroke(dp(z ? 2.0f : 1.0f), z ? -1 : -12303288);
            stateListDrawable.addState(z ? new int[]{R.attr.state_focused} : new int[0], gradientDrawable);
        }
        editText.setBackgroundTintList(null);
        editText.setBackground(stateListDrawable);
    }

    static /* synthetic */ void lambda$button$3(Button button, View view, boolean z) {
        button.setTextColor(z ? -16250872 : -1);
        button.setBackgroundTintList(ColorStateList.valueOf(z ? -1 : -1767148));
        button.animate().scaleX(z ? 1.04f : 1.0f).scaleY(z ? 1.04f : 1.0f).setDuration(160L).start();
    }

    void button(String str, final Runnable runnable) {
        final Button button = new Button(this);
        button.setText(str);
        button.setTextColor(-1);
        button.setAllCaps(false);
        button.setBackgroundTintList(ColorStateList.valueOf(-1767148));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(52.0f));
        layoutParams.topMargin = dp(8.0f);
        this.panel.addView(button, layoutParams);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z) {
                AccessActivity.lambda$button$3(button, view, z);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$validate$10$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m14lambda$validate$10$comguhflixshellAccessActivity(Gate.ApiError apiError) {
        this.busy = false;
        if (isFinishing()) {
            return;
        }
        if (apiError.status == 401) {
            Gate.clear(this);
            render("Entre uma vez para assistir em até 3 telas simultâneas.", true);
        } else {
            render(apiError.status == 0 ? "Sem conexão com o serviço. Seu acesso não foi liberado. Tente novamente em instantes." : apiError.getMessage(), false);
            button("TENTAR NOVAMENTE", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m20lambda$validate$8$comguhflixshellAccessActivity();
                }
            });
            button("SAIR DA CONTA", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m21lambda$validate$9$comguhflixshellAccessActivity();
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$validate$11$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m15lambda$validate$11$comguhflixshellAccessActivity() {
        try {
            final JSONObject jSONObjectRequest = Gate.request(this, "GET", "/api/app/access", null, true);
            runOnUiThread(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m19lambda$validate$7$comguhflixshellAccessActivity(jSONObjectRequest);
                }
            });
        } catch (Gate.ApiError e) {
            runOnUiThread(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m14lambda$validate$10$comguhflixshellAccessActivity(e);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$validate$4$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$validate$4$comguhflixshellAccessActivity() {
        finish();
    }

    /* JADX INFO: renamed from: lambda$validate$7$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$validate$7$comguhflixshellAccessActivity(JSONObject jSONObject) {
        this.busy = false;
        if (isFinishing()) {
            return;
        }
        String strOptString = jSONObject.optString("state");
        if (!"active".equals(strOptString)) {
            blocked(strOptString);
            return;
        }
        Gate.catalogChecked();
        if (!getIntent().getBooleanExtra("manage", false)) {
            startActivity(new Intent().setClassName(getPackageName(), "br.com.saimo.tv.EscolhaActivity"));
            finish();
        } else {
            render("Sua assinatura está ativa.\nAté 3 reproduções simultâneas por conta.\nPlano: R$ 30,00 / 30 dias.", false);
            button("VOLTAR AO CATÁLOGO", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m16lambda$validate$4$comguhflixshellAccessActivity();
                }
            });
            button("VERIFICAR ATUALIZAÇÃO", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m17lambda$validate$5$comguhflixshellAccessActivity();
                }
            });
            button("SAIR DA CONTA", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m18lambda$validate$6$comguhflixshellAccessActivity();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: validate, reason: merged with bridge method [inline-methods] and merged with bridge method [inline-methods] and merged with bridge method [inline-methods] */
    public void m20lambda$validate$8$comguhflixshellAccessActivity() {
        if (this.busy || isFinishing()) {
            return;
        }
        this.busy = true;
        Gate.work.execute(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m15lambda$validate$11$comguhflixshellAccessActivity();
            }
        });
    }

    void blocked(String str) {
        render("blocked".equals(str) ? "ACESSO BLOQUEADO\nSua conta foi bloqueada pelo administrador. Entre em contato com nosso atendimento." : "ACESSO BLOQUEADO\nSeu período de acesso terminou. Para renovar por R$ 30,00 mensais, entre em contato com nosso atendimento.\nWhatsApp: (11) 97073-1504", false);
        button("ENTRAR EM CONTATO", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m0lambda$blocked$12$comguhflixshellAccessActivity();
            }
        });
        button("VERIFICAR RENOVAÇÃO", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m1lambda$blocked$13$comguhflixshellAccessActivity();
            }
        });
        button("SAIR DA CONTA", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m2lambda$blocked$14$comguhflixshellAccessActivity();
            }
        });
        this.handler.removeCallbacks(this.check);
        this.handler.postDelayed(this.check, 15000L);
    }

    /* JADX INFO: renamed from: lambda$blocked$12$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m0lambda$blocked$12$comguhflixshellAccessActivity() {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/5511970731504")));
        } catch (Exception e) {
            this.message.setText(((Object) this.message.getText()) + "\nWhatsApp: (11) 97073-1504");
        }
    }

    /* JADX INFO: renamed from: lambda$login$15$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m7lambda$login$15$comguhflixshellAccessActivity() {
        this.password.setText("");
        this.busy = false;
        m20lambda$validate$8$comguhflixshellAccessActivity();
    }

    /* JADX INFO: renamed from: lambda$login$16$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$login$16$comguhflixshellAccessActivity(Exception exc) {
        this.busy = false;
        this.password.setText("");
        this.message.setText(exc.getMessage());
    }

    /* JADX INFO: renamed from: lambda$login$17$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m9lambda$login$17$comguhflixshellAccessActivity(String str, String str2) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("username", str);
            jSONObject.put("password", str2);
            jSONObject.put("installationId", Gate.installation(this));
            jSONObject.put("label", Build.MANUFACTURER + " " + Build.MODEL);
            jSONObject.put("client", "tv");
            Gate.saveTokens(this, Gate.request(this, "POST", "/api/auth/login", jSONObject, false));
            runOnUiThread(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m7lambda$login$15$comguhflixshellAccessActivity();
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m8lambda$login$16$comguhflixshellAccessActivity(e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: login, reason: merged with bridge method [inline-methods] */
    public void m13lambda$render$1$comguhflixshellAccessActivity() {
        if (this.busy) {
            return;
        }
        final String lowerCase = this.user.getText().toString().trim().toLowerCase(Locale.ROOT);
        final String string = this.password.getText().toString();
        if (lowerCase.isEmpty() || string.isEmpty()) {
            this.message.setText("Informe usuário e senha.");
            return;
        }
        this.busy = true;
        this.message.setText("Conectando com segurança…");
        Gate.work.execute(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m9lambda$login$17$comguhflixshellAccessActivity(lowerCase, string);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: checkUpdate, reason: merged with bridge method [inline-methods] */
    public void m17lambda$validate$5$comguhflixshellAccessActivity() {
        OtaManager.check(this,true);
    }

    /* JADX INFO: renamed from: lambda$checkUpdate$18$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$checkUpdate$18$comguhflixshellAccessActivity() {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://guhflix-tv-download.onrender.com/")));
        } catch (Exception e) {
            this.message.setText("Abra no Downloader: guhflix-tv-download.onrender.com");
        }
    }

    /* JADX INFO: renamed from: lambda$checkUpdate$19$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m4lambda$checkUpdate$19$comguhflixshellAccessActivity(int i, int i2, String str) {
        this.busy = false;
        if (isFinishing()) {
            return;
        }
        if (i <= i2) {
            this.message.setText("Seu GUHFLIX está atualizado. Versão " + str + ".");
        } else {
            this.message.setText("Nova versão disponível: " + str);
            button("ABRIR DOWNLOAD OFICIAL", new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    AccessActivity.this.m3lambda$checkUpdate$18$comguhflixshellAccessActivity();
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$checkUpdate$20$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m5lambda$checkUpdate$20$comguhflixshellAccessActivity() {
        this.busy = false;
        if (isFinishing()) {
            return;
        }
        this.message.setText("Não foi possível verificar a atualização. Confira a conexão e tente novamente.");
    }

    /* JADX INFO: renamed from: lambda$checkUpdate$21$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m6lambda$checkUpdate$21$comguhflixshellAccessActivity() {
        runOnUiThread(() -> OtaManager.check(this,true));
    }

    /* JADX INFO: renamed from: lambda$logout$22$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m10lambda$logout$22$comguhflixshellAccessActivity() {
        this.busy = false;
        render("Entre com sua conta GUHFLIX.", true);
    }

    /* JADX INFO: renamed from: lambda$logout$23$com-guhflix-shell-AccessActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$logout$23$comguhflixshellAccessActivity() {
        try {
            Gate.request(this, "POST", "/api/auth/logout", new JSONObject(), true);
        } catch (Exception e) {
        }
        Gate.clear(this);
        runOnUiThread(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m10lambda$logout$22$comguhflixshellAccessActivity();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX INFO: renamed from: logout, reason: merged with bridge method [inline-methods] and merged with bridge method [inline-methods] and merged with bridge method [inline-methods] */
    public void m21lambda$validate$9$comguhflixshellAccessActivity() {
        if (this.busy) {
            return;
        }
        this.busy = true;
        Gate.work.execute(new Runnable() { // from class: com.guhflix.shell.AccessActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                AccessActivity.this.m11lambda$logout$23$comguhflixshellAccessActivity();
            }
        });
    }
}
