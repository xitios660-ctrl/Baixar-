package com.guhflix.shell;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public final class Gate {
    static final String API = br.com.saimo.tv.BuildConfig.GUHFLIX_API;
    static volatile long checkedAt;
    static boolean checking;
    static final ExecutorService work = Executors.newSingleThreadExecutor();
    static final Handler main = new Handler(Looper.getMainLooper());
    static volatile String access = "";
    static final Map<Activity, Playback> players = new WeakHashMap();
    static final Set<Activity> visible = Collections.newSetFromMap(new WeakHashMap());

    static final class ApiError extends Exception {
        final String code;
        final int status;

        ApiError(int i, String str, String str2) {
            super(str2);
            this.status = i;
            this.code = str;
        }
    }

    static String installation(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("guhflix-device", 0);
        String string = sharedPreferences.getString("installation", null);
        if (string != null) {
            return string;
        }
        String string2 = UUID.randomUUID().toString();
        sharedPreferences.edit().putString("installation", string2).commit();
        return string2;
    }

    static SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias("guhflix-session")) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder("guhflix-session", 3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            keyGenerator.generateKey();
        }
        return (SecretKey) keyStore.getKey("guhflix-session", null);
    }

    static void saveTokens(Context context, JSONObject jSONObject) throws Exception {
        String strOptString = jSONObject.optString("refreshToken", "");
        if (!strOptString.isEmpty()) {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, key());
            if (!context.getSharedPreferences("guhflix-session", 0).edit().putString("refresh", Base64.encodeToString(cipher.getIV(), 2) + ":" + Base64.encodeToString(cipher.doFinal(strOptString.getBytes("UTF-8")), 2)).commit()) {
                throw new IOException("Não foi possível salvar a sessão.");
            }
        }
        access = jSONObject.getString("accessToken");
    }

    static String refresh(Context context) throws Exception {
        String string = context.getSharedPreferences("guhflix-session", 0).getString("refresh", "");
        if (string.isEmpty()) {
            throw new ApiError(401, "NO_SESSION", "Entre na sua conta");
        }
        String[] strArrSplit = string.split(":");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, key(), new GCMParameterSpec(128, Base64.decode(strArrSplit[0], 0)));
        return new String(cipher.doFinal(Base64.decode(strArrSplit[1], 0)), "UTF-8");
    }

    static void clear(Context context) {
        access = "";
        checkedAt = 0L;
        context.getSharedPreferences("guhflix-session", 0).edit().clear().commit();
    }

    static synchronized JSONObject request(Context context,String method,String path,JSONObject body,boolean authenticated) throws ApiError {
        try {
            if(authenticated && access.isEmpty()) renew(context);
            try {return raw(method,path,body,authenticated?access:"");}
            catch(ApiError e) {
                if(!authenticated || e.status!=401)throw e;
                renew(context);
                return raw(method,path,body,access);
            }
        } catch(ApiError e){throw e;}
        catch(Exception e){throw new ApiError(0,"NETWORK","Não foi possível conectar. Verifique a internet e tente novamente.");}
    }

    static void renew(Context context) throws Exception {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("refreshToken", refresh(context));
            jSONObject.put("client", "tv");
            saveTokens(context, raw("POST", "/api/auth/refresh", jSONObject, ""));
        } catch (ApiError e) {
            if (e.status == 401) {
                clear(context);
            }
            throw e;
        } catch (GeneralSecurityException e2) {
            clear(context);
            throw new ApiError(401, "KEY_LOST", "Entre novamente na sua conta.");
        }
    }

    static JSONObject raw(String method, String path, JSONObject body, String bearer) throws ApiError {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(API + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (!bearer.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) {
                connection.setDoOutput(true);connection.setRequestProperty("Content-Type", "application/json");
                try(OutputStream out = connection.getOutputStream()) {out.write(body.toString().getBytes("UTF-8"));}
            }
            int code = connection.getResponseCode();
            InputStream in = code < 400 ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if(in != null)try(InputStream stream=in){byte[] buffer=new byte[8192];int n;while((n=stream.read(buffer))!=-1){if(bytes.size()+n>2097152)throw new IOException("Resposta excedeu o limite");bytes.write(buffer,0,n);}}
            String text=bytes.toString("UTF-8");
            JSONObject result=text.trim().startsWith("[")?new JSONObject().put("items",new JSONArray(text)):new JSONObject(text);
            if(code>=400)throw new ApiError(code,result.optString("error"),result.optString("message","Serviço temporariamente indisponível"));
            return result;
        }catch(ApiError e){throw e;}catch(Exception e){throw new ApiError(0,"NETWORK","Não foi possível conectar ao GUHFLIX. Confira a internet e tente novamente.");}
        finally {if(connection!=null)connection.disconnect();}
    }

    static void catalogChecked() {
        checkedAt = SystemClock.elapsedRealtime();
    }

    static /* synthetic */ void lambda$resumed$0(Activity activity, JSONObject jSONObject) {
        checking = false;
        if (!visible.contains(activity) || activity.isFinishing()) {
            return;
        }
        if ("active".equals(jSONObject.optString("state"))) {
            catalogChecked();
        } else {
            openAccess(activity);
        }
    }

    static /* synthetic */ void lambda$resumed$1(Activity activity) {
        checking = false;
        if (!visible.contains(activity) || activity.isFinishing()) {
            return;
        }
        openAccess(activity);
    }

    static /* synthetic */ void lambda$resumed$2(final Activity activity) {
        try {
            final JSONObject jSONObjectRequest = request(activity, "GET", "/api/app/access", null, true);
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$resumed$0(activity, jSONObjectRequest);
                }
            });
        } catch (ApiError e) {
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$resumed$1(activity);
                }
            });
        }
    }

    public static void resumed(final Activity activity) {
        visible.add(activity);
        if ((activity instanceof AccessActivity) || (activity instanceof IntroActivity) || activity.getClass().getSimpleName().contains("Player") || SystemClock.elapsedRealtime() - checkedAt < 30000 || checking) {
            return;
        }
        checking = true;
        work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$resumed$2(activity);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void openAccess(Activity activity) {
        activity.startActivity(new Intent(activity, (Class<?>) AccessActivity.class).addFlags(67108864));
        activity.finish();
    }

    static final class Playback {
        Runnable expiry;
        final String id = UUID.randomUUID().toString();
        boolean initialized;
        long leaseUntil;
        final Bundle state;
        boolean stopped;
        Runnable tick;

        Playback(Bundle bundle) {
            this.state = bundle;
        }
    }

    public static void paused(Activity activity) {
        visible.remove(activity);
    }

    public static boolean initialized(Activity activity) {
        Playback playback = players.get(activity);
        return playback != null && playback.initialized;
    }

    public static void beginPlayer(Activity activity, Bundle bundle) {
        Playback playback = new Playback(bundle);
        players.put(activity, playback);
        loading(activity, "Preparando reprodução…");
        start(activity, playback);
    }

    static /* synthetic */ void lambda$start$4(final Activity activity, final Playback playback) {
        if (activity.isFinishing() || activity.isDestroyed() || playback.stopped) {
            stop(activity, playback);
            return;
        }
        try {
            playback.initialized = true;
            activity.getClass().getMethod("guhflixInitialize", Bundle.class).invoke(activity, playback.state);
            GuhflixUi.style(activity);
            playback.tick = new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.heartbeat(activity, playback);
                }
            };
            main.postDelayed(playback.tick, 35000L);
        } catch (Exception e) {
            playback.initialized = false;
            stop(activity, playback);
            loading(activity, "Não foi possível iniciar o player. Volte e tente novamente.");
        }
    }

    static /* synthetic */ void lambda$start$5(Activity activity, Playback playback, Exception exc) {
        if (activity.isFinishing()) {
            return;
        }
        failure(activity, playback, exc);
    }

    static /* synthetic */ void lambda$start$6(final Playback playback, final Activity activity) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("playbackId", playback.id);
            request(activity, "POST", "/api/app/playback/start", jSONObject, true);
            grantLease(activity, playback, jElapsedRealtime);
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$start$4(activity, playback);
                }
            });
        } catch (Exception e) {
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$start$5(activity, playback, e);
                }
            });
        }
    }

    static void start(final Activity activity, final Playback playback) {
        work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$start$6(playback, activity);
            }
        });
    }

    static int dp(Activity activity, float f) {
        return Math.round(f * activity.getResources().getDisplayMetrics().density);
    }

    static void loading(Activity activity, String str) {
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setGravity(17);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(activity, 40.0f), dp(activity, 24.0f), dp(activity, 40.0f), dp(activity, 24.0f));
        linearLayout.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{-13367284, -16250872, -16250872}));
        LinearLayout linearLayout2 = new LinearLayout(activity);
        linearLayout2.setTag("guhflix-gate-panel");
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(activity, 24.0f), dp(activity, 18.0f), dp(activity, 24.0f), dp(activity, 18.0f));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(dp(activity, 570.0f), -2));
        TextView textView = new TextView(activity);
        textView.setText("GUHFLIX");
        textView.setTypeface(Typeface.create("sans-serif-condensed", 1));
        textView.setTextColor(-1767148);
        textView.setTextSize(48.0f);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(activity);
        textView2.setPadding(0, dp(activity, 12.0f), 0, dp(activity, 22.0f));
        textView2.setText(str);
        textView2.setTextSize(20.0f);
        textView2.setTextColor(-1);
        linearLayout2.addView(textView2);
        activity.setContentView(linearLayout);
        linearLayout2.setAlpha(0.0f);
        linearLayout2.animate().alpha(1.0f).setDuration(250L).start();
    }

    static /* synthetic */ void lambda$styleButtons$7(Button button, View view, boolean z) {
        button.setBackgroundTintList(ColorStateList.valueOf(z ? -56015 : -6682864));
        button.animate().scaleX(z ? 1.04f : 1.0f).scaleY(z ? 1.04f : 1.0f).setDuration(160L).start();
    }

    static void styleButtons(Activity activity, LinearLayout linearLayout) {
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            if (linearLayout.getChildAt(i) instanceof Button) {
                final Button button = (Button) linearLayout.getChildAt(i);
                button.setTextColor(-1);
                button.setAllCaps(false);
                button.setBackgroundTintList(ColorStateList.valueOf(-6682864));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(activity, 52.0f));
                layoutParams.topMargin = dp(activity, 8.0f);
                button.setLayoutParams(layoutParams);
                button.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda19
                    @Override // android.view.View.OnFocusChangeListener
                    public final void onFocusChange(View view, boolean z) {
                        Gate.lambda$styleButtons$7(button, view, z);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void failure(final Activity activity, final Playback playback, Exception exc) {
        loading(activity, exc.getMessage());
        LinearLayout linearLayout = (LinearLayout) activity.getWindow().getDecorView().findViewWithTag("guhflix-gate-panel");
        boolean z = exc instanceof ApiError;
        if (z && "SCREEN_LIMIT".equals(((ApiError) exc).code)) {
            Button button = new Button(activity);
            button.setText("GERENCIAR TELAS");
            linearLayout.addView(button);
            button.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda14
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    Gate.sessions(activity, playback);
                }
            });
            Button button2 = new Button(activity);
            button2.setText("TENTAR NOVAMENTE");
            linearLayout.addView(button2);
            button2.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda15
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    Gate.lambda$failure$9(activity, playback, view);
                }
            });
        } else if (z) {
            ApiError apiError = (ApiError) exc;
            if (apiError.status == 401 || apiError.status == 403) {
                Button button3 = new Button(activity);
                button3.setText("VERIFICAR CONTA");
                linearLayout.addView(button3);
                button3.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda16
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        Gate.openAccess(activity);
                    }
                });
            }
        }
        Button button4 = new Button(activity);
        button4.setText("VOLTAR");
        linearLayout.addView(button4);
        button4.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                activity.finish();
            }
        });
        styleButtons(activity, linearLayout);
        button4.requestFocus();
    }

    static /* synthetic */ void lambda$failure$9(Activity activity, Playback playback, View view) {
        loading(activity, "Verificando telas…");
        start(activity, playback);
    }

    static /* synthetic */ void lambda$sessions$12(Activity activity, Playback playback) {
        loading(activity, "Verificando telas…");
        start(activity, playback);
    }

    static /* synthetic */ void lambda$sessions$14(final Activity activity, String str, final Playback playback) {
        try {
            request(activity, "POST", "/api/app/sessions/" + str + "/revoke", new JSONObject(), true);
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$sessions$12(activity, playback);
                }
            });
        } catch (Exception e) {
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.failure(activity, playback, e);
                }
            });
        }
    }

    static /* synthetic */ void lambda$sessions$16(JSONArray jSONArray, final Activity activity, final Playback playback, DialogInterface dialogInterface, int i) {
        final String strOptString = jSONArray.optJSONObject(i).optString("id");
        new AlertDialog.Builder(activity).setMessage("Encerrar esta sessão? Será necessário entrar novamente nesse dispositivo.").setPositiveButton("Encerrar", new DialogInterface.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda21
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface2, int i2) {
                Gate.work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda18
                    @Override // java.lang.Runnable
                    public final void run() {
                        Gate.lambda$sessions$14(activity, strOptString, playback);
                    }
                });
            }
        }).setNegativeButton("Cancelar", (DialogInterface.OnClickListener) null).show();
    }

    static /* synthetic */ void lambda$sessions$17(final Activity activity, final JSONArray jSONArray, final Playback playback) {
        if (activity.isFinishing()) {
            return;
        }
        String[] strArr = new String[jSONArray.length()];
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
            strArr[i] = jSONObjectOptJSONObject.optString("label") + (jSONObjectOptJSONObject.optBoolean("playbackActive") ? " · reproduzindo" : " · conectado");
        }
        new AlertDialog.Builder(activity).setTitle("Encerrar uma sessão").setItems(strArr, new DialogInterface.OnClickListener() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda22
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                Gate.lambda$sessions$16(jSONArray, activity, playback, dialogInterface, i2);
            }
        }).setNegativeButton("Voltar", (DialogInterface.OnClickListener) null).show();
    }

    static /* synthetic */ void lambda$sessions$19(final Activity activity, final Playback playback) {
        try {
            final JSONArray jSONArray = request(activity, "GET", "/api/app/sessions", null, true).getJSONArray("items");
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$sessions$17(activity, jSONArray, playback);
                }
            });
        } catch (Exception e) {
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.failure(activity, playback, e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sessions(final Activity activity, final Playback playback) {
        work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$sessions$19(activity, playback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void heartbeat(final Activity activity, final Playback playback) {
        if (playback.stopped || activity.isDestroyed()) {
            return;
        }
        work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$heartbeat$21(playback, activity);
            }
        });
    }

    static /* synthetic */ void lambda$heartbeat$20(Playback playback, ApiError apiError, Activity activity) {
        if (playback.stopped) {
            return;
        }
        if (apiError.status == 0 && SystemClock.elapsedRealtime() < playback.leaseUntil) {
            main.postDelayed(playback.tick, 10000L);
            return;
        }
        stop(activity, playback);
        activity.finish();
        activity.startActivity(new Intent(activity, (Class<?>) AccessActivity.class));
    }

    static /* synthetic */ void lambda$heartbeat$21(final Playback playback, final Activity activity) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("playbackId", playback.id);
            request(activity, "POST", "/api/app/playback/heartbeat", jSONObject, true);
            grantLease(activity, playback, jElapsedRealtime);
            main.postDelayed(playback.tick, 35000L);
        } catch (Exception e) {
            final ApiError apiError = e instanceof ApiError ? (ApiError) e : new ApiError(0, "NETWORK", "Sem conexão");
            main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    Gate.lambda$heartbeat$20(playback, apiError, activity);
                }
            });
        }
    }

    static void grantLease(final Activity activity, final Playback playback, long j) {
        playback.leaseUntil = j + 110000;
        main.post(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$grantLease$23(playback, activity);
            }
        });
    }

    static /* synthetic */ void lambda$grantLease$22(Playback playback, Activity activity) {
        if (playback.stopped) {
            return;
        }
        stop(activity, playback);
        activity.finish();
        activity.startActivity(new Intent(activity, (Class<?>) AccessActivity.class));
    }

    static /* synthetic */ void lambda$grantLease$23(final Playback playback, final Activity activity) {
        if (playback.stopped) {
            return;
        }
        if (playback.expiry != null) {
            main.removeCallbacks(playback.expiry);
        }
        playback.expiry = new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$grantLease$22(playback, activity);
            }
        };
        main.postDelayed(playback.expiry, Math.max(0L, playback.leaseUntil - SystemClock.elapsedRealtime()));
    }

    static /* synthetic */ void lambda$stop$24(Playback playback, Activity activity) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("playbackId", playback.id);
            request(activity, "POST", "/api/app/playback/stop", jSONObject, true);
        } catch (Exception e) {
        }
    }

    static void stop(final Activity activity, final Playback playback) {
        if (playback.stopped) {
            return;
        }
        playback.stopped = true;
        if (playback.expiry != null) {
            main.removeCallbacks(playback.expiry);
        }
        if (playback.tick != null) {
            main.removeCallbacks(playback.tick);
        }
        work.execute(new Runnable() { // from class: com.guhflix.shell.Gate$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                Gate.lambda$stop$24(playback, activity);
            }
        });
    }

    public static void stopped(Activity activity) {
        Playback playback = players.get(activity);
        if (playback != null) {
            stop(activity, playback);
            if (activity.isFinishing()) {
                return;
            }
            activity.finish();
        }
    }

    public static void destroyed(Activity activity) {
        Playback playbackRemove = players.remove(activity);
        if (playbackRemove != null) {
            stop(activity, playbackRemove);
        }
        visible.remove(activity);
    }
}
