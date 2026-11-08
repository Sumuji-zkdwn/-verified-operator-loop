package jp.example.mathime;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** The order is tap, left, up, right, down. Pure Java so it can be tested without Android. */
public final class KeyLayouts {
    public static final String[] TABS = {"基本", "論理", "集合", "微積", "αβ", "英数"};
    public static final String[][][] PAGES = {
        { {"=","≠","≈","≡",":="}, {"+","±","⊕","∓","∑"}, {"−","–","⊖","∓","-"},
          {"×","·","⊗","*","∘"}, {"÷","/","∕","%","∶"}, {"√","∛","∞","∜","∝"},
          {"(","[","{","⟨","|"}, {")","]","}","⟩","‖"}, {"/","\\","_","^","∣"},
          {"x","y","z","t","n"}, {"^","²","³","⁻¹","ₙ"}, {"∞","0","1","2","π"} },
        { {"∀","∃","∄","∴","∵"}, {"⇒","→","⟹","⇐","↦"}, {"⇔","↔","⟺","≡","≔"},
          {"¬","⊥","⊤","≠","∅"}, {"∧","&","⊓","⋀","∩"}, {"∨","|","⊔","⋁","∪"},
          {"∃","∀","∃!","∄","∈"}, {"⊢","⊣","⊨","⊬","⊩"}, {"⊨","⊭","⊢","⊥","⊤"},
          {"∴","∵","∎","□","◇"}, {"P","Q","R","S","T"}, {"=","≠","⇒","⇔","¬"} },
        { {"∈","∉","∋","∌","∅"}, {"⊂","⊄","⊆","⊊","⊃"}, {"⊆","⊈","⊊","⊇","⊋"},
          {"∪","⋃","⊔","+","⊎"}, {"∩","⋂","⊓","×","∖"}, {"∅","∖","∁","℘","∣"},
          {"ℕ","ℤ","ℚ","ℝ","ℂ"}, {"ℤ","ℕ","ℚ","ℝ","ℂ"}, {"ℝ","ℚ","ℂ","ℝⁿ","ℤ"},
          {"{","}","[","]","∅"}, {"→","↦","←","↔","∘"}, {"×","∩","∪","∖","⊗"} },
        { {"∂","∇","Δ","∂²","d"}, {"∫","∬","∭","∮","∞"}, {"∑","∏","⋃","⋂","∞"},
          {"lim","sup","inf","max","min"}, {"→","←","↦","⇒","↔"}, {"∞","−∞","±∞","0","1"},
          {"d","dx","dy","dt","d²"}, {"f","g","h","F","G"}, {"′","″","‴","^","∂"},
          {"≈","∼","≃","≅","≍"}, {"≤","<","≥",">","≠"}, {"π","e","i","ℏ","°"} },
        { {"α","β","γ","δ","ε"}, {"β","α","ϐ","Β","γ"}, {"γ","Γ","δ","Δ","ε"},
          {"δ","Δ","ε","ζ","η"}, {"ε","ϵ","ζ","η","θ"}, {"θ","ϑ","Θ","κ","η"},
          {"λ","Λ","μ","ν","ξ"}, {"μ","ν","Μ","η","ξ"}, {"π","Π","ρ","σ","τ"},
          {"σ","Σ","ς","τ","ρ"}, {"φ","ϕ","Φ","χ","ψ"}, {"ω","Ω","ψ","Ψ","ζ"} },
        { {"a","b","c","d","e"}, {"f","g","h","i","j"}, {"k","l","m","n","o"},
          {"p","q","r","s","t"}, {"u","v","w","x","y"}, {"x","y","z","t","n"},
          {"1","2","3","4","5"}, {"6","7","8","9","0"}, {".",",",":",";","_"},
          {"-","+","=","/","*"}, {"(",")","[","]","'"}, {"?","!","@","#","%"} }
    };
    private static final Map<String,String> TEX = new HashMap<>();
    static {
        String[][] values = {
            {"≠","\\ne"},{"≈","\\approx"},{"≡","\\equiv"},{"±","\\pm"},{"∓","\\mp"},
            {"⊕","\\oplus"},{"⊖","\\ominus"},{"×","\\times"},{"·","\\cdot"},{"⊗","\\otimes"},
            {"∘","\\circ"},{"÷","\\div"},{"∕","/"},{"∶",":"},{"√","\\sqrt{}"},{"∛","\\sqrt[3]{}"},
            {"∜","\\sqrt[4]{}"},{"∞","\\infty"},{"−∞","-\\infty"},{"±∞","\\pm\\infty"},{"−","-"},{"–","-"},{"∝","\\propto"},
            {"⟨","\\langle"},{"⟩","\\rangle"},{"‖","\\Vert"},{"∣","\\mid"},
            {"²","^2"},{"³","^3"},{"⁻¹","^{-1}"},{"ₙ","_n"},{"ℝⁿ","\\mathbb{R}^n"},
            {"∀","\\forall"},{"∃","\\exists"},{"∄","\\nexists"},{"∃!","\\exists!"},
            {"⇒","\\Rightarrow"},{"⇐","\\Leftarrow"},{"⇔","\\Leftrightarrow"},{"⟹","\\implies"},{"⟺","\\iff"},
            {"→","\\to"},{"←","\\leftarrow"},{"↔","\\leftrightarrow"},{"↦","\\mapsto"},{"≔","\\coloneqq"},
            {"¬","\\neg"},{"∧","\\land"},{"∨","\\lor"},{"⋀","\\bigwedge"},{"⋁","\\bigvee"},
            {"⊢","\\vdash"},{"⊣","\\dashv"},{"⊨","\\models"},{"⊬","\\nvdash"},{"⊩","\\Vdash"},{"⊭","\\not\\models"},
            {"∴","\\therefore"},{"∵","\\because"},{"⊥","\\bot"},{"⊤","\\top"},{"∎","\\blacksquare"},{"□","\\square"},{"◇","\\diamond"},
            {"∈","\\in"},{"∉","\\notin"},{"∋","\\ni"},{"∌","\\not\\ni"},
            {"⊂","\\subset"},{"⊄","\\not\\subset"},{"⊆","\\subseteq"},{"⊈","\\nsubseteq"},{"⊊","\\subsetneq"},
            {"⊃","\\supset"},{"⊇","\\supseteq"},{"⊋","\\supsetneq"},
            {"∪","\\cup"},{"∩","\\cap"},{"⋃","\\bigcup"},{"⋂","\\bigcap"},{"⊔","\\sqcup"},{"⊓","\\sqcap"},
            {"⊎","\\uplus"},{"∅","\\varnothing"},{"∖","\\setminus"},{"∁","\\complement"},{"℘","\\wp"},
            {"ℕ","\\mathbb{N}"},{"ℤ","\\mathbb{Z}"},{"ℚ","\\mathbb{Q}"},{"ℝ","\\mathbb{R}"},{"ℂ","\\mathbb{C}"},
            {"∂","\\partial"},{"∂²","\\partial^2"},{"∇","\\nabla"},{"Δ","\\Delta"},{"∫","\\int"},{"∬","\\iint"},
            {"∭","\\iiint"},{"∮","\\oint"},{"∑","\\sum"},{"∏","\\prod"},{"lim","\\lim"},{"sup","\\sup"},{"inf","\\inf"},{"max","\\max"},{"min","\\min"},
            {"′","'"},{"″","''"},{"‴","'''"},{"∼","\\sim"},{"≃","\\simeq"},{"≅","\\cong"},{"≍","\\asymp"},
            {"≤","\\leq"},{"≥","\\geq"},{"ℏ","\\hbar"},{"°","^\\circ"},
            {"α","\\alpha"},{"β","\\beta"},{"ϐ","\\beta"},{"Β","B"},{"γ","\\gamma"},{"Γ","\\Gamma"},
            {"δ","\\delta"},{"ε","\\epsilon"},{"ϵ","\\varepsilon"},{"ζ","\\zeta"},{"η","\\eta"},
            {"θ","\\theta"},{"ϑ","\\vartheta"},{"Θ","\\Theta"},{"κ","\\kappa"},{"λ","\\lambda"},{"Λ","\\Lambda"},
            {"μ","\\mu"},{"Μ","M"},{"ν","\\nu"},{"ξ","\\xi"},{"π","\\pi"},{"Π","\\Pi"},{"ρ","\\rho"},
            {"σ","\\sigma"},{"Σ","\\Sigma"},{"ς","\\varsigma"},{"τ","\\tau"},{"φ","\\phi"},{"ϕ","\\varphi"},
            {"Φ","\\Phi"},{"χ","\\chi"},{"ψ","\\psi"},{"Ψ","\\Psi"},{"ω","\\omega"},{"Ω","\\Omega"}
        };
        for (String[] pair : values) TEX.put(pair[0], pair[1]);
    }
    private KeyLayouts() {}
    public static int direction(float dx, float dy, float threshold) {
        if (Math.max(Math.abs(dx), Math.abs(dy)) < threshold) return 0;
        return Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? 1 : 3) : (dy < 0 ? 2 : 4);
    }
    public static String value(String[] key, int direction, boolean upper) {
        String s = key[direction];
        return upper && s.length() == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'z' ? s.toUpperCase(Locale.ROOT) : s;
    }
    public static String output(String symbol, boolean tex) {
        if (!tex) return symbol;
        String mapped = TEX.get(symbol);
        if (mapped == null) return symbol;
        // A space terminates a final TeX control word, not commands followed by punctuation/braces.
        return mapped.matches(".*\\\\[A-Za-z]+$") ? mapped + " " : mapped;
    }
}
