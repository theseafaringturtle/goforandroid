package derp.goforandroid;

/**
 * Created by User on 07/04/2017.
 */

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prettify.PrettifyParser;
import syntaxhighlight.ParseResult;
import syntaxhighlight.Parser;

public class PrettifyHighlighter {//http://stackoverflow.com/q/22124731
    Map<String, String> COLORS = buildColorsMap();
    String FONT_PATTERN = "<font color=\"#%s\">%s</font>";
    Parser parser = new PrettifyParser();
    static String syntaxHighlighting = "None";

    public String getHtmlHighlight(String fileExtension, String sourceCode) {
        StringBuilder highlighted = new StringBuilder();
        List<ParseResult> results = parser.parse(fileExtension, sourceCode);
        for (ParseResult result : results) {
            String type = result.getStyleKeys().get(0);
            String content = sourceCode.substring(result.getOffset(), result.getOffset() + result.getLength());
            highlighted.append(String.format(FONT_PATTERN, getColor(type), content));
        }
        return highlighted.toString();
    }
    public void highlight(Editable s, String code){
        //s.clearSpans(); http://stackoverflow.com/a/10273449
        ForegroundColorSpan[] toRemoveSpans = s.getSpans(0, s.toString().length(), ForegroundColorSpan.class);
        for (int i = 0; i < toRemoveSpans.length; i++)
            s.removeSpan(toRemoveSpans[i]);
        String hl = "go";
        if(syntaxHighlighting.equals("None")){
            return;
        }
        else if(syntaxHighlighting.equals("Extended")){
            hl="goextra";
        }
        List<ParseResult> results = parser.parse(hl, code);
        for (ParseResult result : results) {
            String type = result.getStyleKeys().get(0);
            s.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#"+getColor(type))),
                    result.getOffset(),
                    result.getOffset() + result.getLength(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    /*
    for (int i = str.indexOf(k); i >= 0; i = str.indexOf(k, i + 1)) {
                            s.setSpan(
                                    new ForegroundColorSpan(Color.MAGENTA),
                                    i,
                                    i + k.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
     */

    private String getColor(String type) {
        return COLORS.containsKey(type) ? COLORS.get(type) : COLORS.get("pln");
    }

    private static Map<String, String> buildColorsMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("typ", "FF4C4C");
        map.put("kwd", "FF4E1D");
        map.put("lit", "F2DD3D");
        map.put("com", "999999");
        map.put("str", "FF4C4C");
        map.put("pun", "EDA916");
        map.put("pln", "0E2979");
        return map;
    }
}
