package jp.example.mathime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A self-contained IME. No entered content is persisted or transmitted. */
public final class MathInputService extends InputMethodService {
    private static final int BG = Color.rgb(233,238,240), INK = Color.rgb(31,48,59);
    private static final int MUTED = Color.rgb(97,115,125), ACCENT = Color.rgb(35,103,96);
    private SharedPreferences prefs;
    private LinearLayout root, tabs, grid;
    private TextView tip, mode, caps;
    private int page = 0;
    private boolean tex = false, upper = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int fill, int border, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius));
        if (border != 0) d.setStroke(dp(1), border); return d;
    }
    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(color); t.setTextSize(sp);
        t.setGravity(Gravity.CENTER); t.setIncludeFontPadding(false); return t;
    }
    private LinearLayout.LayoutParams marginParams(int w, int h, float weight, int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h,weight); p.setMargins(dp(m),dp(m),dp(m),dp(m)); return p;
    }
    @Override public View onCreateInputView() {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        tex = prefs.getBoolean("tex", false);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(4),dp(2),dp(4),dp(3)); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int bottom=android.os.Build.VERSION.SDK_INT>=30 ? insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom : insets.getSystemWindowInsetBottom();
            v.setPadding(dp(4),dp(2),dp(4),dp(3)+bottom); return insets;
        });
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        tip = text("タップ＝中央 ・ フリック＝周囲", 12, MUTED); tip.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); tip.setPadding(dp(7),0,0,0);
        bar.addView(tip, new LinearLayout.LayoutParams(0,dp(32),1));
        mode = text("", 12, ACCENT); mode.setTypeface(null,Typeface.BOLD); mode.setBackground(shape(Color.WHITE,0,8));
        mode.setContentDescription("文字と LaTeX の出力を切り替える"); mode.setOnClickListener(v -> {tex=!tex; prefs.edit().putBoolean("tex",tex).apply(); updateMode();});
        bar.addView(mode, marginParams(dp(90),dp(29),0,1)); root.addView(bar);
        tabs = new LinearLayout(this); root.addView(tabs, new LinearLayout.LayoutParams(-1,dp(43)));
        grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL); root.addView(grid);
        LinearLayout bottom = new LinearLayout(this);
        bottom.addView(button("🌐", "キーボードを切り替え", () -> showPicker()), marginParams(0,dp(50),.9f,2));
        caps = button("⇧", "英字の大文字と小文字を切り替え", () -> {upper=!upper; rebuild();}); bottom.addView(caps, marginParams(0,dp(50),.8f,2));
        bottom.addView(button("空白", "空白を入力", () -> commit(" ")), marginParams(0,dp(50),2.2f,2));
        bottom.addView(button("閉じる", "キーボードを閉じる", () -> requestHideSelf(0)), marginParams(0,dp(50),1.1f,2));
        root.addView(bottom); updateMode(); rebuild(); return root;
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info,restarting);
        if (prefs == null) prefs=getSharedPreferences("settings",MODE_PRIVATE);
        if (root != null) rebuild();
    }
    private void updateMode() { if(mode != null) mode.setText(tex ? "LaTeX" : "文字"); }
    private TextView button(String label, String description, Runnable action) {
        TextView v=text(label, label.length()>3?13:18, INK); v.setBackground(shape(Color.rgb(216,226,229),0,8));
        v.setContentDescription(description); v.setClickable(true); v.setOnClickListener(w->{w.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); action.run();}); return v;
    }
    private void rebuild() {
        if(tabs==null || grid==null) return;
        tabs.removeAllViews();
        for(int i=0;i<KeyLayouts.TABS.length;i++) {
            final int index=i; TextView t=text(KeyLayouts.TABS[i],13,i==page?Color.WHITE:INK);
            t.setTypeface(null,i==page?Typeface.BOLD:Typeface.NORMAL); t.setBackground(shape(i==page?ACCENT:Color.TRANSPARENT,0,7));
            t.setContentDescription(KeyLayouts.TABS[i]+" の記号"); t.setOnClickListener(v->{page=index; rebuild();});
            tabs.addView(t,marginParams(0,dp(37),1,1));
        }
        caps.setText(upper?"⇧ ON":"⇧"); caps.setTextColor(upper?ACCENT:INK);
        grid.removeAllViews(); int keyHeight=prefs.getInt("height",62); if(keyHeight<52||keyHeight>74) keyHeight=62;
        String[] controls={"⌫","◀","▶","↵"};
        for(int row=0; row<4; row++) {
            LinearLayout line=new LinearLayout(this);
            for(int col=0;col<3;col++) {
                String[] data=KeyLayouts.PAGES[page][row*3+col]; FlickKey key=new FlickKey(this,data);
                line.addView(key,marginParams(0,dp(keyHeight),1,2));
            }
            final int action=row;
            TextView control=button(controls[row],new String[]{"一文字削除","カーソルを左へ","カーソルを右へ","改行または確定"}[row],()->control(action));
            if(row==0) installDeleteRepeat(control);
            line.addView(control,marginParams(0,dp(keyHeight),.72f,2)); grid.addView(line);
        }
    }
    private void installDeleteRepeat(TextView control) {
        control.setOnTouchListener(new View.OnTouchListener() {
            boolean repeated=false;
            final Runnable repeat=new Runnable() { public void run(){ repeated=true; delete(); handler.postDelayed(this,70); } };
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: repeated=false; handler.postDelayed(repeat,420); return false;
                    case MotionEvent.ACTION_UP: handler.removeCallbacks(repeat); if(repeated) {v.setPressed(false); return true;} return false;
                    case MotionEvent.ACTION_CANCEL: handler.removeCallbacks(repeat); return false;
                } return false;
            }
        });
    }
    private void showPicker() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(); }
    private void commit(String text) {
        InputConnection ic=getCurrentInputConnection(); if(ic==null) return;
        ic.commitText(text,1);
        // In TeX square-root templates place the cursor between the braces.
        if(tex && text.endsWith("{}")) cursor(KeyEvent.KEYCODE_DPAD_LEFT);
    }
    private void choose(String symbol) { commit(KeyLayouts.output(symbol,tex)); }
    private void cursor(int code) {
        InputConnection ic=getCurrentInputConnection(); if(ic==null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code)); ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));
    }
    private void delete() {
        InputConnection ic=getCurrentInputConnection(); if(ic==null) return;
        CharSequence selected=ic.getSelectedText(0);
        if(selected!=null && selected.length()>0) {ic.commitText("",1); return;}
        if(!ic.deleteSurroundingTextInCodePoints(1,0)) cursor(KeyEvent.KEYCODE_DEL);
    }
    private void control(int action) {
        if(action==0) delete();
        else if(action==1) cursor(KeyEvent.KEYCODE_DPAD_LEFT);
        else if(action==2) cursor(KeyEvent.KEYCODE_DPAD_RIGHT);
        else if(!sendDefaultEditorAction(true)) commit("\n");
    }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }

    private final class FlickKey extends View {
        final String[] options; final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); float downX,downY; int direction;
        FlickKey(Context c,String[] data) {
            super(c); options=data; setClickable(true); setFocusable(true); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            String[] dirs={"タップ", "左", "上", "右", "下"}; StringBuilder a=new StringBuilder();
            for(int i=0;i<5;i++) a.append(dirs[i]).append(' ').append(KeyLayouts.value(options,i,upper)).append(i==4?"":"、");
            setContentDescription(a.toString());
        }
        private void label(Canvas c,String label,float x,float baseline,int size,int color,boolean bold) {
            paint.setColor(color); paint.setTextSize(dp(size)); paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(label,x,baseline,paint);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(),h=getHeight();
            paint.setColor(isPressed()?Color.rgb(212,237,229):Color.WHITE); c.drawRoundRect(new RectF(0,0,w,h),dp(9),dp(9),paint);
            float cx=w/2f, cy=h/2f;
            label(c,KeyLayouts.value(options,0,upper),cx,cy+dp(7),22,INK,true);
            label(c,KeyLayouts.value(options,2,upper),cx,dp(12),10,MUTED,false);
            label(c,KeyLayouts.value(options,4,upper),cx,h-dp(3),10,MUTED,false);
            label(c,KeyLayouts.value(options,1,upper),dp(13),cy+dp(3),10,MUTED,false);
            label(c,KeyLayouts.value(options,3,upper),w-dp(13),cy+dp(3),10,MUTED,false);
        }
        private void updateTip() {
            String value=KeyLayouts.value(options,direction,upper);
            tip.setText((direction==0?"タップ":new String[]{"","←","↑","→","↓"}[direction])+"  "+value);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: downX=event.getX(); downY=event.getY(); direction=0; setPressed(true); updateTip(); return true;
                case MotionEvent.ACTION_MOVE: direction=KeyLayouts.direction(event.getX()-downX,event.getY()-downY,dp(20)); updateTip(); return true;
                case MotionEvent.ACTION_UP: direction=KeyLayouts.direction(event.getX()-downX,event.getY()-downY,dp(20)); setPressed(false); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if(direction==0) performClick(); else choose(KeyLayouts.value(options,direction,upper));
                    tip.setText("タップ＝中央 ・ フリック＝周囲"); return true;
                case MotionEvent.ACTION_CANCEL: setPressed(false); tip.setText("タップ＝中央 ・ フリック＝周囲"); return true;
            } return true;
        }
        @Override public boolean performClick() { super.performClick(); choose(KeyLayouts.value(options,0,upper)); return true; }
    }
}
