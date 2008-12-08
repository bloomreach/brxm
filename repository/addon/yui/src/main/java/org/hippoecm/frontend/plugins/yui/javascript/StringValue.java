package org.hippoecm.frontend.plugins.yui.javascript;

public class StringValue extends Value<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String SINGLE_QUOTE = "'";
    private static final String SINGLE_QUOTE_ESCAPED = "\\'";

    private boolean escaped;
    
    public StringValue(String value, boolean escaped) {
        super(value);
        this.escaped = escaped;
    }

    public String getScriptValue() {
      if(escaped) {
          return escapeString(value);
      }
      return value;
    }
    
    static String escapeString(String value) {
        //TODO: backslash should be escaped as well
        if (value != null)
            value = SINGLE_QUOTE + value.replace(SINGLE_QUOTE, SINGLE_QUOTE_ESCAPED) + SINGLE_QUOTE;
        return value;
    }

}
