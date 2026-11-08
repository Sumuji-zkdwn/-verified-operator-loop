package jp.example.mathime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    private EditText practiceInput;
    private final int ink=Color.rgb(31,48,59), secondary=Color.rgb(91,109,117), accent=Color.rgb(35,103,96);
    private int dp(float x) { return (int)(getResources().getDisplayMetrics().density*x+.5f); }
    private GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String s,int sp,int color) {TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setLineSpacing(dp(3),1f);return v;}
    private LinearLayout.LayoutParams p(int height,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.topMargin=dp(top);return p;}
    private TextView action(String s, boolean filled, Runnable fn) {
        TextView b=text(s,16,filled?Color.WHITE:accent); b.setGravity(Gravity.CENTER); b.setTypeface(null,Typeface.BOLD);
        b.setBackground(shape(filled?accent:Color.rgb(225,237,233),12)); b.setOnClickListener(v->fn.run()); return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(248,250,249)); getWindow().setNavigationBarColor(Color.rgb(248,250,249));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(248,250,249));
        LinearLayout content=new LinearLayout(this); content.setPadding(dp(24),dp(24),dp(24),dp(32));content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);
        // Android 15+ can enforce edge-to-edge for target SDK 35.
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getSystemWindowInsetTop();
            int bottom=insets.getSystemWindowInsetBottom();
            int keyboardBottom=0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                top=insets.getInsets(WindowInsets.Type.systemBars()).top;
                bottom=insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                keyboardBottom=insets.getInsets(WindowInsets.Type.ime()).bottom;
            }
            v.setPadding(0,top,0,Math.max(bottom,keyboardBottom));
            if (keyboardBottom>bottom && practiceInput!=null && practiceInput.hasFocus()) {
                v.post(() -> {android.graphics.Rect rect=new android.graphics.Rect(0,0,practiceInput.getWidth(),practiceInput.getHeight());practiceInput.requestRectangleOnScreen(rect,true);});
            }
            return insets;
        });
        TextView eyebrow=text("MATH FLICK",12,accent);eyebrow.setTypeface(null,Typeface.BOLD);content.addView(eyebrow,p(-2,0));
        TextView title=text("数式フリック",28,ink);title.setTypeface(null,Typeface.BOLD);content.addView(title,p(-2,6));
        content.addView(text("記号をタップ、または上下左右にフリック。\nいつものアプリで、そのまま使えます。",15,secondary),p(-2,8));
        status=text("",14,secondary);status.setPadding(dp(14),dp(12),dp(14),dp(12));status.setBackground(shape(Color.WHITE,10));content.addView(status,p(-2,18));
        content.addView(action("1   キーボードを有効にする",true,()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))),p(52,16));
        content.addView(action("2   数式フリックを選ぶ",false,()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker()),p(52,8));
        TextView practice=text("ここで練習",17,ink);practice.setTypeface(null,Typeface.BOLD);content.addView(practice,p(-2,24));
        EditText input=new EditText(this);practiceInput=input;input.setSingleLine(false);input.setMinLines(3);input.setTextSize(18);input.setPadding(dp(12),dp(10),dp(12),dp(10));input.setGravity(Gravity.TOP|Gravity.START);
        input.setHint("例：∀x ∈ ℝ  x² ≥ 0");input.setBackground(shape(Color.WHITE,10));content.addView(input,p(108,8));
        content.addView(text("タブで「基本・論理・集合・微積・αβ・英数」を切り替えます。右上の「文字／LaTeX」で出力形式を変更できます。",14,secondary),p(-2,16));
        content.addView(text("🌐 を押すと、普段の日本語キーボードに戻せます。⌫ 長押しで連続削除、◀ ▶ でカーソル移動。",14,secondary),p(-2,9));
        TextView height=text("キーの高さ",17,ink);height.setTypeface(null,Typeface.BOLD);content.addView(height,p(-2,24));
        LinearLayout choices=new LinearLayout(this); String[] labels={"小さめ","ふつう","大きめ"};int[] heights={54,62,72};
        for(int i=0;i<3;i++){final int h=heights[i];TextView b=action(labels[i],false,()->{getSharedPreferences("settings",MODE_PRIVATE).edit().putInt("height",h).apply();updateHeight(choices,heights);});LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,dp(44),1);q.setMargins(0,0,dp(5),0);choices.addView(b,q);}
        content.addView(choices,p(-2,8));updateHeight(choices,heights);
        content.addView(text("入力した文字を保存・送信しません。インターネット権限も使いません。",13,secondary),p(-2,24));
        setContentView(scroll);
    }
    private void updateHeight(LinearLayout row,int[] heights){int current=getSharedPreferences("settings",MODE_PRIVATE).getInt("height",62);for(int i=0;i<row.getChildCount();i++){TextView t=(TextView)row.getChildAt(i);boolean selected=heights[i]==current;t.setTextColor(selected?Color.WHITE:accent);t.setBackground(shape(selected?accent:Color.rgb(225,237,233),12));}}
    @Override protected void onResume(){super.onResume();if(status==null)return;InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);boolean enabled=false;for(InputMethodInfo info:imm.getEnabledInputMethodList())if(info.getPackageName().equals(getPackageName()))enabled=true;
        String selected=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);boolean active=selected!=null&&selected.startsWith(getPackageName()+"/");
        status.setText(active?"✓ 有効です。下の練習欄を押して試せます。":enabled?"✓ 有効になりました。次は 2 から選んでください。":"まず 1 から数式フリックを有効にしてください。");}
}
