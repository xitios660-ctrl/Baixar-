package com.guhflix.shell;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

/* JADX INFO: loaded from: classes2.dex */
public final class IntroActivity extends Activity {
    boolean leaving;
    long start;
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable finishIntro = new Runnable() { // from class: com.guhflix.shell.IntroActivity$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            IntroActivity.this.open();
        }
    };

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(128);
        this.start = SystemClock.uptimeMillis();
        setContentView(new Intro(this));
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.handler.postDelayed(this.finishIntro, Math.max(0L, 4200 - (SystemClock.uptimeMillis() - this.start)));
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.handler.removeCallbacks(this.finishIntro);
        super.onPause();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void open() {
        if (this.leaving || isFinishing()) {
            return;
        }
        this.leaving = true;
        startActivity(new Intent().setClassName(getPackageName(), "com.guhflix.shell.AccessActivity").setAction("android.intent.action.MAIN"));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 23 && i != 66) {
            return super.onKeyDown(i, keyEvent);
        }
        open();
        return true;
    }

    final class Intro extends View {
        final Camera camera;
        final Matrix matrix;
        final Paint paint;

        Intro(Activity activity) {
            super(activity);
            this.paint = new Paint(3);
            this.camera = new Camera();
            this.matrix = new Matrix();
            setLayerType(2, null);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            int width = getWidth();
            int height = getHeight();
            float fUptimeMillis = (SystemClock.uptimeMillis() - IntroActivity.this.start) / 4200.0f;
            canvas.drawColor(-16250872);
            float fPow = 1.0f - ((float) Math.pow(1.0f - Math.max(0.0f, Math.min(1.0f, (fUptimeMillis - 0.05f) / 0.65f)), 3.0d));
            float f = width;
            float f2 = f * 0.5f;
            float f3 = height;
            this.paint.setShader(new RadialGradient(f2, f3 * 0.49f, Math.max(1.0f, 0.48f * f), new int[]{1889533963, 403570688, 526344}, (float[]) null, Shader.TileMode.CLAMP));
            canvas.drawRect(0.0f, 0.0f, f, f3, this.paint);
            this.paint.setShader(null);
            int i = 0;
            while (i < 34) {
                float f4 = i;
                this.paint.setColor(Color.argb((int) ((Math.sin(f4 + (4.0f * fUptimeMillis)) * 45.0d) + 35.0d), 229, 9, 20));
                canvas.drawCircle((((97.31f * f4) % 1000.0f) / 1000.0f) * f, ((((61.17f * f4) + (18.0f * fUptimeMillis)) % 600.0f) / 600.0f) * f3, (i % 2) + 1, this.paint);
                i++;
                fUptimeMillis = fUptimeMillis;
            }
            float f5 = fUptimeMillis;
            for(int ray=0;ray<22;ray++){
                float x=f2+(ray-11)*f*.038f;
                float light=(float)Math.sin(Math.min(1f,fUptimeMillis)*Math.PI);
                this.paint.setColor(Color.argb((int)(light*(60-ray%4*10)),229,9,20));
                this.paint.setStrokeWidth(2f+(ray%3));
                canvas.drawLine(x-f*.12f,0,x+f*.12f,f3,this.paint);
            }
            canvas.save();
            canvas.translate(f2, 0.51f * f3);
            this.camera.save();
            this.camera.rotateY((1.0f - fPow) * (-24.0f));
            this.camera.getMatrix(this.matrix);
            this.camera.restore();
            canvas.concat(this.matrix);
            float fMin = Math.min(f * 0.20f, 0.3f * f3);
            this.paint.setTextSize(fMin);
            this.paint.setTypeface(Typeface.create("sans-serif-condensed", 1));
            this.paint.setTextAlign(Paint.Align.CENTER);
            float f6 = (fPow * 0.14f) + 0.86f;
            canvas.scale(f6, f6);
            GuhflixUi.Wordmark wordmark = new GuhflixUi.Wordmark();
            float f7 = -fMin;
            wordmark.setBounds((int) (f7 * 2.4f), (int) (f7 * 0.62f), (int) (2.4f * fMin), (int) (0.64f * fMin));
            wordmark.draw(canvas);
            this.paint.setColor(-657935);
            this.paint.setTextSize(0.105f * fMin);
            this.paint.setTypeface(Typeface.create("sans-serif-medium", 0));
            canvas.drawText("S E U  U N I V E R S O  E M  C E N A", 0.0f, fMin * 0.73f, this.paint);
            canvas.restore();
            this.paint.setColor(-1767148);
            float f8 = 0.43f * f;
            float f9 = 0.79f * f3;
            canvas.drawRoundRect(f8, f9, f8 + (f * 0.14f * Math.min(1.0f, f5)), f9 + 3.0f, 2.0f, 2.0f, this.paint);
            if (!isShown() || f5 >= 1.1f) {
                return;
            }
            postInvalidateOnAnimation();
        }
    }
}
