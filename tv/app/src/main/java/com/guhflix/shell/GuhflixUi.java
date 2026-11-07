package com.guhflix.shell;

import android.R;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes2.dex */
public final class GuhflixUi {
    private static final WeakHashMap<Activity, ReferenceHome> homeScreens = new WeakHashMap<>();
    private static final WeakHashMap<Activity, CinemaScene> scenes = new WeakHashMap<>();
    private static final WeakHashMap<Activity, ViewTreeObserver.OnGlobalFocusChangeListener> focusListeners = new WeakHashMap<>();

    /* JADX INFO: renamed from: com.guhflix.shell.GuhflixUi$1, reason: invalid class name */
    static class AnonymousClass1 implements Application.ActivityLifecycleCallbacks {
        AnonymousClass1() {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(final Activity activity, Bundle bundle) {
            activity.getWindow().getDecorView().post(new Runnable() { // from class: com.guhflix.shell.GuhflixUi$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GuhflixUi.style(activity);
                }
            });
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
            Gate.resumed(activity);
            CloudRemote.resumed(activity);
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
            Gate.paused(activity);
            CloudRemote.paused(activity);
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
            Gate.stopped(activity);
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
            ReferenceHome referenceHome = (ReferenceHome) GuhflixUi.homeScreens.remove(activity);
            if (referenceHome != null) {
                referenceHome.close();
            }
            CinemaScene cinemaScene = (CinemaScene) GuhflixUi.scenes.remove(activity);
            if (cinemaScene != null) {
                cinemaScene.close();
            }
            ViewTreeObserver.OnGlobalFocusChangeListener onGlobalFocusChangeListener = (ViewTreeObserver.OnGlobalFocusChangeListener) GuhflixUi.focusListeners.remove(activity);
            ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
            if (onGlobalFocusChangeListener != null && viewTreeObserver.isAlive()) {
                viewTreeObserver.removeOnGlobalFocusChangeListener(onGlobalFocusChangeListener);
            }
            Gate.destroyed(activity);
        }
    }

    public static void install(Application application) {
        try {
            Field declaredField = Class.forName("j").getDeclaredField("n");
            declaredField.setAccessible(true);
            declaredField.setBoolean(null, true);
        } catch (Exception e) {
        }
        application.registerActivityLifecycleCallbacks(new AnonymousClass1());
        CloudRemote.init(application);
    }

    private static int dp(Activity activity, float f) {
        return Math.round(f * activity.getResources().getDisplayMetrics().density);
    }

    public static void style(final Activity activity) {
        int i;
        if (activity instanceof IntroActivity) {
            return;
        }
        View decorView = activity.getWindow().getDecorView();
        activity.getWindow().setStatusBarColor(-16250872);
        activity.getWindow().setNavigationBarColor(-16250872);
        decorView.setSystemUiVisibility(4358);
        int identifier = activity.getResources().getIdentifier("menuGlobalAbas", "id", activity.getPackageName());
        View viewFindViewById = identifier == 0 ? null : activity.findViewById(identifier);
        if (viewFindViewById != null && (viewFindViewById.getParent() instanceof View) && (((View) viewFindViewById.getParent()).getParent() instanceof LinearLayout)) {
            LinearLayout linearLayout = (LinearLayout) ((View) viewFindViewById.getParent()).getParent();
            linearLayout.setPadding(dp(activity, 28.0f), dp(activity, 8.0f), dp(activity, 28.0f), dp(activity, 8.0f));
            linearLayout.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-16250872, -1073215480}));
            if (linearLayout.getChildAt(0) instanceof ImageView) {
                ImageView imageView = (ImageView) linearLayout.getChildAt(0);
                imageView.setImageDrawable(new Wordmark());
                imageView.setContentDescription("GUHFLIX");
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(activity, 102.0f), dp(activity, 26.0f));
                layoutParams.setMarginEnd(dp(activity, 14.0f));
                imageView.setLayoutParams(layoutParams);
            }
        }
        if (viewFindViewById != null && (viewFindViewById.getParent() instanceof View) && (((View) viewFindViewById.getParent()).getParent() instanceof LinearLayout)) {
            LinearLayout linearLayout2 = (LinearLayout) ((View) viewFindViewById.getParent()).getParent();
            if (linearLayout2.findViewWithTag("guhflix-account") == null) {
                Button button = new Button(activity);
                button.setTag("guhflix-account");
                button.setText("Conta / iPhone");
                button.setTextColor(-1);
                button.setBackgroundTintList(ColorStateList.valueOf(-1767148));
                button.setOnClickListener(new View.OnClickListener() { // from class: com.guhflix.shell.GuhflixUi$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        Activity activity2 = activity;
                        activity2.startActivity(new Intent(activity2, HubActivity.class));
                    }
                });
                linearLayout2.addView(button, new LinearLayout.LayoutParams(dp(activity, 122.0f), dp(activity, 40.0f)));
            }
        }
        if (viewFindViewById instanceof LinearLayout) {
            LinearLayout linearLayout3 = (LinearLayout) viewFindViewById;
            for (int i2 = 0; i2 < linearLayout3.getChildCount(); i2++) {
                View childAt = linearLayout3.getChildAt(i2);
                if (childAt instanceof TextView) {
                    TextView textView = (TextView) childAt;
                    textView.setTextSize(12.0f);
                    textView.setGravity(17);
                    textView.setPadding(dp(activity, 12.0f), dp(activity, 8.0f), dp(activity, 12.0f), dp(activity, 8.0f));
                    textView.setCompoundDrawablePadding(dp(activity, 6.0f));
                    Drawable drawable = textView.getCompoundDrawablesRelative()[0];
                    if (drawable != null) {
                        drawable.setBounds(0, 0, dp(activity, 22.0f), dp(activity, 22.0f));
                        textView.setCompoundDrawablesRelative(null, drawable, null, null);
                    }
                    StateListDrawable stateListDrawable = new StateListDrawable();
                    int i3 = 0;
                    while (i3 < 3) {
                        GradientDrawable gradientDrawable = new GradientDrawable();
                        gradientDrawable.setColor(i3 == 0 ? -1767148 : -15658733);
                        gradientDrawable.setCornerRadius(dp(activity, 7.0f));
                        int iDp = dp(activity, 1.0f);
                        if (i3 == 0) {
                            i = -1;
                        } else {
                            i = i3 == 1 ? -1767148 : -13291467;
                        }
                        gradientDrawable.setStroke(iDp, i);
                        stateListDrawable.addState(i3 == 0 ? new int[]{R.attr.state_focused} : i3 == 1 ? new int[]{R.attr.state_selected} : new int[0], gradientDrawable);
                        i3++;
                    }
                    textView.setBackground(stateListDrawable);
                    textView.setTextColor(-1);
                }
            }
        }
        if (activity.getClass().getSimpleName().contains("Player") || (activity instanceof AccessActivity) || focusListeners.containsKey(activity)) {
            return;
        }
        ReferenceHome referenceHomeInstall = ReferenceHome.install(activity);
        if (referenceHomeInstall != null) {
            homeScreens.put(activity, referenceHomeInstall);
        }
        CinemaScene cinemaSceneAttach = CinemaScene.attach(activity);
        if (cinemaSceneAttach != null) {
            scenes.put(activity, cinemaSceneAttach);
        }
        ViewTreeObserver.OnGlobalFocusChangeListener onGlobalFocusChangeListener = new ViewTreeObserver.OnGlobalFocusChangeListener() { // from class: com.guhflix.shell.GuhflixUi$$ExternalSyntheticLambda1
            @Override // android.view.ViewTreeObserver.OnGlobalFocusChangeListener
            public final void onGlobalFocusChanged(View view, View view2) {
                GuhflixUi.lambda$style$1(activity, view, view2);
            }
        };
        focusListeners.put(activity, onGlobalFocusChangeListener);
        decorView.getViewTreeObserver().addOnGlobalFocusChangeListener(onGlobalFocusChangeListener);
    }

    static /* synthetic */ void lambda$style$1(Activity activity, View view, View view2) {
        String resourceEntryName;
        if (CinemaScene.motion(activity)) {
            float f = 1.0f;
            if (view != null) {
                view.animate().cancel();
                view.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0.0f).setDuration(130L).start();
            }
            if (view2 != null) {
                try {
                    resourceEntryName = activity.getResources().getResourceEntryName(view2.getId());
                } catch (Exception e) {
                    resourceEntryName = "";
                }
                if (!(resourceEntryName.equals("playerLinha") || view2.getWidth() > dp(activity, 500.0f))) {
                    f = view2 instanceof TextView ? 1.025f : 1.065f;
                }
                view2.animate().cancel();
                view2.animate().scaleX(f).scaleY(f).translationZ(dp(activity, 12.0f)).setDuration(170L).setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    public static final class Wordmark extends Drawable {
        final Paint p = new Paint(3);

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            canvas.save();
            canvas.translate(bounds.left, bounds.top);
            float fHeight = bounds.height();
            float fWidth = bounds.width();
            this.p.setTypeface(Typeface.create("sans-serif", 3));
            this.p.setTextSize(0.72f * fHeight);
            float fMeasureText = this.p.measureText("GUHFLIX");
            canvas.scale(Math.min(1.0f, fWidth / (1.06f * fMeasureText)), 1.0f);
            this.p.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, fHeight, new int[]{-1, -3750200}, (float[]) null, Shader.TileMode.CLAMP));
            float f = 0.8f * fHeight;
            canvas.drawText("GUH", 0.0f, f, this.p);
            float fMeasureText2 = this.p.measureText("GUH");
            this.p.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, fHeight, new int[]{-46512, -1767148, -6946809}, (float[]) null, Shader.TileMode.CLAMP));
            canvas.drawText("FLIX", fMeasureText2, f, this.p);
            this.p.setShader(null);
            this.p.setColor(-1239776);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(Math.max(1.0f, 0.035f * fHeight));
            Path path = new Path();
            path.moveTo(0.38f * fMeasureText, fHeight * 0.12f);
            path.cubicTo(fMeasureText * 0.93f, (-fHeight) * 0.12f, fMeasureText * 1.14f, fHeight * 0.28f, fMeasureText * 0.94f, fHeight * 0.92f);
            canvas.drawPath(path, this.p);
            this.p.setStyle(Paint.Style.FILL);
            canvas.restore();
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.p.setAlpha(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.p.setColorFilter(colorFilter);
        }
    }
}
