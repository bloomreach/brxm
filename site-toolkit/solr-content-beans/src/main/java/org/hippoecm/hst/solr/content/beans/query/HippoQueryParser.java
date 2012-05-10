package org.hippoecm.hst.solr.content.beans.query;

/**
 * Utility class for parsing Lucene input queries to either escape or replace Lucene specific chars
 */
public class HippoQueryParser {
    
    private final static HippoQueryParser qp = new HippoQueryParser();
    
    private HippoQueryParser () {
        
    }

    public static HippoQueryParser getInstance() {
        return qp;
    }

    /**
     * Returns a String where those characters that QueryParser
     * expects to be escaped are escaped by a preceding <code>\</code>.
     */
    public String escape(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
}
