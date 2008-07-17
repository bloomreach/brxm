package org.hippoecm.frontend.plugins.yui.util;

import java.util.regex.Pattern;

public class JavascriptUtil {

    public static final String SINGLE_QUOTE = "'";
    
    private static Pattern numbers = Pattern.compile("\\d*");
    private static Pattern functions = Pattern.compile("(\\w+)\\.(\\w+)");
    
    public static String serialize2JS(String value) {
        if (value == null)
            return "null";
        else if(value.equals(""))
            return SINGLE_QUOTE + SINGLE_QUOTE;
        else if (value.equalsIgnoreCase("true"))
            return "true";
        else if (value.equalsIgnoreCase("false"))
            return "false";
        else if (numbers.matcher(value).matches() || functions.matcher(value).find())
            return value;

        return SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, "\\\\" + SINGLE_QUOTE) + SINGLE_QUOTE;
    }

}
