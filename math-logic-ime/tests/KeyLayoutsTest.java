package jp.example.mathime;
public class KeyLayoutsTest {
    static void check(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(KeyLayouts.PAGES.length==KeyLayouts.TABS.length,"pages");
        for (int p=0;p<KeyLayouts.PAGES.length;p++) {
            check(KeyLayouts.PAGES[p].length==12,"keys on page "+p);
            for(String[] key:KeyLayouts.PAGES[p]) {check(key.length==5,"directions");for(String s:key)check(s!=null&&!s.isEmpty(),"empty symbol");}
        }
        check(KeyLayouts.direction(0,0,20)==0,"tap");
        check(KeyLayouts.direction(-25,3,20)==1,"left");
        check(KeyLayouts.direction(3,-25,20)==2,"up");
        check(KeyLayouts.direction(25,3,20)==3,"right");
        check(KeyLayouts.direction(3,25,20)==4,"down");
        check(KeyLayouts.direction(19,19,20)==0,"dead zone");
        check(KeyLayouts.output("∀",false).equals("∀"),"unicode");
        check(KeyLayouts.output("∀",true).equals("\\forall "),"tex command delimiter");
        check(KeyLayouts.output("√",true).equals("\\sqrt{}"),"root template");
        check(KeyLayouts.output("ℝ",true).equals("\\mathbb{R}"),"number sets");
        check(KeyLayouts.value(new String[]{"a","b","c","d","e"},3,true).equals("D"),"uppercase");
        check(KeyLayouts.value(new String[]{"α","β","γ","δ","ε"},3,true).equals("δ"),"greek unchanged");
        System.out.println("KeyLayouts tests passed");
    }
}
