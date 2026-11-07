package com.guhflix.shell;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/* JADX INFO: loaded from: classes2.dex */
public final class CinemaScene extends View implements ViewTreeObserver.OnPreDrawListener {
    private float blend;
    private boolean closed;
    private Drawable current;
    private ValueAnimator fade;
    private final View host;
    private final Paint paint;
    private Drawable previous;
    private Drawable source;
    private final ViewGroup stage;

    private CinemaScene(Activity activity, ViewGroup viewGroup, View view) {
        super(activity);
        this.blend = 1.0f;
        this.paint = new Paint(3);
        this.stage = viewGroup;
        this.host = view;
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(2);
    }

    static int id(Activity activity, String str) {
        return activity.getResources().getIdentifier(str, "id", activity.getPackageName());
    }

    public static CinemaScene attach(Activity activity) {
        View viewFindViewById = activity.findViewById(id(activity, "inicioDestaque"));
        CinemaScene cinemaScene = null;
        if (viewFindViewById != null && (viewFindViewById.getParent() instanceof View) && (((View) viewFindViewById.getParent()).getParent() instanceof ViewGroup)) {
            ViewGroup viewGroup = (ViewGroup) ((View) viewFindViewById.getParent()).getParent();
            if (!(viewGroup instanceof FrameLayout)) {
                return null;
            }
            cinemaScene = new CinemaScene(activity, viewGroup, activity.getWindow().getDecorView());
            viewGroup.addView(cinemaScene, 0, new ViewGroup.LayoutParams(-1, -1));
            cinemaScene.host.getViewTreeObserver().addOnPreDrawListener(cinemaScene);
            if (motion(activity)) {
                viewFindViewById.setAlpha(0.0f);
                viewFindViewById.setTranslationY(activity.getResources().getDisplayMetrics().density * 18.0f);
                viewFindViewById.animate().alpha(1.0f).translationY(0.0f).setDuration(460L).setInterpolator(new DecelerateInterpolator()).start();
            }
        }
        return cinemaScene;
    }

    static boolean motion(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", 1.0f) > 0.0f;
        } catch (Exception e) {
            return true;
        }
    }

    /* JADX INFO: renamed from: lambda$onPreDraw$0$com-guhflix-shell-CinemaScene, reason: not valid java name */
    /* synthetic */ void m22lambda$onPreDraw$0$comguhflixshellCinemaScene(ValueAnimator valueAnimator) {
        this.blend = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.ViewTreeObserver.OnPreDrawListener
    public boolean onPreDraw() {
        View viewFindFocus;
        if (this.closed || (viewFindFocus = this.host.findFocus()) == null) {
            return true;
        }
        ImageView imageViewFindArtwork = null;
        for (int i = 0; i < 4 && viewFindFocus != this.stage && (((imageViewFindArtwork = findArtwork(viewFindFocus)) == null || imageViewFindArtwork.getDrawable() == null) && (viewFindFocus.getParent() instanceof View)); i++) {
            viewFindFocus = (View) viewFindFocus.getParent();
        }
        if (imageViewFindArtwork != null && imageViewFindArtwork.getDrawable() != null && imageViewFindArtwork.getDrawable() != this.source) {
            Drawable drawable = imageViewFindArtwork.getDrawable();
            if (drawable.getConstantState() == null) {
                return true;
            }
            this.source = drawable;
            Drawable drawableMutate = drawable.getConstantState().newDrawable(getResources()).mutate();
            if (this.fade != null) {
                this.fade.cancel();
            }
            this.previous = this.current;
            this.current = drawableMutate;
            this.blend = motion(getContext()) ? 0.0f : 1.0f;
            if (this.blend == 0.0f) {
                this.fade = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.fade.setDuration(420L);
                this.fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.guhflix.shell.CinemaScene$$ExternalSyntheticLambda0
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        CinemaScene.this.m22lambda$onPreDraw$0$comguhflixshellCinemaScene(valueAnimator);
                    }
                });
                this.fade.addListener(new AnimatorListenerAdapter() { // from class: com.guhflix.shell.CinemaScene.1
                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        CinemaScene.this.previous = null;
                    }
                });
                this.fade.start();
            } else {
                this.previous = null;
                invalidate();
            }
        }
        return true;
    }

    private ImageView findArtwork(View view) {
        String[] strArr = {"capaImagem", "secaoCapa1", "secaoCapa2", "secaoCapa3"};
        for (int i = 0; i < 4; i++) {
            View viewFindViewById = view.findViewById(getResources().getIdentifier(strArr[i], "id", getContext().getPackageName()));
            if ((viewFindViewById instanceof ImageView) && viewFindViewById.getVisibility() == 0) {
                ImageView imageView = (ImageView) viewFindViewById;
                if (imageView.getDrawable() != null) {
                    return imageView;
                }
            }
        }
        return null;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(-16250872);
        float f = width;
        float f2 = height;
        this.paint.setShader(new RadialGradient(f * 0.8f, f2 * 0.18f, Math.max(width, height) * 0.7f, new int[]{-13037546, -16250872}, (float[]) null, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, f, f2, this.paint);
        this.paint.setShader(null);
        drawArtwork(canvas, this.previous, 1.0f - this.blend);
        drawArtwork(canvas, this.current, this.blend);
        this.paint.setShader(new LinearGradient(0.0f, 0.0f, f, 0.0f, new int[]{-16250872, -435681272, 940050440}, new float[]{0.0f, 0.43f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, f, f2, this.paint);
        this.paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, f2, new int[]{537397256, 940050440, -16250872}, new float[]{0.0f, 0.38f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, f, f2, this.paint);
        this.paint.setShader(null);
    }

    private void drawArtwork(Canvas canvas, Drawable drawable, float f) {
        if (drawable == null || f <= 0.0f) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        float fMax = Math.max(1, drawable.getIntrinsicWidth());
        float f2 = height;
        float fMax2 = Math.max(1, drawable.getIntrinsicHeight());
        float fMax3 = Math.max((width * 0.74f) / fMax, (0.88f * f2) / fMax2);
        int i = (int) (fMax * fMax3);
        int i2 = width - i;
        float f3 = f2 * 0.4f;
        float f4 = ((int) (fMax2 * fMax3)) * 0.5f;
        drawable.setBounds(i2, (int) (f3 - f4), i + i2, (int) (f3 + f4));
        drawable.setAlpha((int) (f * 190.0f));
        drawable.draw(canvas);
    }

    public void close() {
        this.closed = true;
        if (this.host.getViewTreeObserver().isAlive()) {
            this.host.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        if (this.fade != null) {
            this.fade.cancel();
            this.fade.removeAllUpdateListeners();
            this.fade.removeAllListeners();
        }
        this.current = null;
        this.previous = null;
        this.source = null;
    }
}
