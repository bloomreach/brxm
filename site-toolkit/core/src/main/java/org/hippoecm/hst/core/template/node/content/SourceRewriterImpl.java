package org.hippoecm.hst.core.template.node.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;

/**
 * Object that searches for HTML links in a content string and replaces
 * the link with valid URL links.
 */
public class SourceRewriterImpl implements SourceRewriter {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Pattern HREF_PATTERN = Pattern.compile("((?:<a\\s.*?href=\"))([^:]*?)(\".*?>)");
    private static final Pattern SRC_PATTERN = Pattern.compile("((?:<img\\s.*?src=\"))([^:]*?)(\".*?>)");

    private URLPathTranslator urlPathTranslator;

    /*
     * constructor with default URLPathTranslatorImpl();
     */
    SourceRewriterImpl() {
        this.urlPathTranslator = new URLPathTranslatorImpl();
    }
    
    /*
     * constructor with custom URLPathTranslatorImpl();
     */
    SourceRewriterImpl(URLPathTranslator urlPathTranslator) {
        this.urlPathTranslator = urlPathTranslator ;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.template.node.content.SourceRewriter#replace(javax.jcr.Session, java.lang.String)
     */
    public String replace(final Session jcrSession,String content) {

        // only create if really needed
        StringBuffer sb = null;

        Matcher hrefPatt = HREF_PATTERN.matcher(content);
        hrefPatt.reset();
        while (hrefPatt.find()) {
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            String documentPath = hrefPatt.group(2);
            String url = urlPathTranslator.documentPathToURL(jcrSession, documentPath);
            hrefPatt.appendReplacement(sb, hrefPatt.group(1) + url + hrefPatt.group(3));
        }
        
        if (sb != null) {
            hrefPatt.appendTail(sb);
            content = String.valueOf(sb);
            sb = null;
        }
        
        Matcher srcPatt = SRC_PATTERN.matcher(content);
        srcPatt.reset();
        while (srcPatt.find()) {
            if (sb == null) {
                sb = new StringBuffer(content.length());
            }
            String documentPath = srcPatt.group(2);
            String url = urlPathTranslator.documentPathToURL(jcrSession, documentPath);
            srcPatt.appendReplacement(sb, srcPatt.group(1) + url + srcPatt.group(3));
        }

        if (sb == null) {
            return content;
        } else {
            srcPatt.appendTail(sb);
            return sb.toString();
        }
    }

}
