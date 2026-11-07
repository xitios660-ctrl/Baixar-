package com.guhflix.shell;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/* JADX INFO: loaded from: classes2.dex */
public final class ReferenceHome implements ViewTreeObserver.OnPreDrawListener {
    final Activity activity;
    boolean closed;
    final View host;
    boolean initialized;
    final View rows;

    private ReferenceHome(Activity activity, ViewGroup viewGroup, View view) {
        this.activity = activity;
        this.rows = view;
        this.host = activity.getWindow().getDecorView();
        viewGroup.setClipChildren(true);
        viewGroup.setClipToPadding(true);
        this.host.getViewTreeObserver().addOnPreDrawListener(this);
    }

    static View find(Activity activity, String str) {
        return activity.findViewById(activity.getResources().getIdentifier(str, "id", activity.getPackageName()));
    }

    public static ReferenceHome install(Activity activity) {
        View viewFind = find(activity, "inicioDestaque");
        View viewFind2 = find(activity, "inicioFilas");
        if (!(viewFind instanceof ViewGroup) || viewFind2 == null) {
            return null;
        }
        return new ReferenceHome(activity, (ViewGroup) viewFind, viewFind2);
    }

    View firstCard(View view) {
        if (view.isFocusable() && (view.findViewById(this.activity.getResources().getIdentifier("capaNome", "id", this.activity.getPackageName())) instanceof TextView)) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View viewFirstCard = firstCard(viewGroup.getChildAt(i));
            if (viewFirstCard != null) {
                return viewFirstCard;
            }
        }
        return null;
    }

    /* JADX INFO: renamed from: lambda$onPreDraw$0$com-guhflix-shell-ReferenceHome, reason: not valid java name */
    /* synthetic */ void m24lambda$onPreDraw$0$comguhflixshellReferenceHome(View view) {
        if (this.closed || !view.isAttachedToWindow()) {
            return;
        }
        view.requestFocus();
    }

    @Override // android.view.ViewTreeObserver.OnPreDrawListener
    public boolean onPreDraw() {
        final View viewFirstCard;
        if (!this.closed && !this.initialized && (viewFirstCard = firstCard(this.rows)) != null) {
            this.initialized = true;
            this.rows.post(new Runnable() { // from class: com.guhflix.shell.ReferenceHome$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ReferenceHome.this.m24lambda$onPreDraw$0$comguhflixshellReferenceHome(viewFirstCard);
                }
            });
        }
        return true;
    }

    public void close() {
        this.closed = true;
        if (this.host.getViewTreeObserver().isAlive()) {
            this.host.getViewTreeObserver().removeOnPreDrawListener(this);
        }
    }
}
