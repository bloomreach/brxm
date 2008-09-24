package org.hippoecm.hst.core.mapping;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlUtilities {
    
    private static final Logger log = LoggerFactory.getLogger(UrlUtilities.class);
    public static final String HTML_POSTFIX = ".html";
    
    public static String encodeUrl(String contextPath, int uriLevels, String rewrite){
        if(rewrite.startsWith("/")) {
            rewrite = rewrite.substring(1);
        }
        String[] uriParts = rewrite.split("/");
        int nrParts = uriParts.length;
        StringBuffer encodedUrl = new StringBuffer(contextPath);
        boolean replaceDup = false;
        if(nrParts > uriLevels) {
            /*
             * When the link is to a hippo document, the name coincides with the handle. 
             * If they do not contain a "." already, replace them by one part, and extend it by .html for nice urls
             */
            if(uriParts[nrParts-1].equals(uriParts[nrParts-2]) && !uriParts[nrParts-1].contains(".")) {
                replaceDup = true; 
            }
        }
        for(int i = 0; i < nrParts; i++) {
            if(replaceDup && i == nrParts-1) {
                encodedUrl.append(HTML_POSTFIX);
            } else {
                // for encoding a url, you have to decode the jcr node paths :-)
                String decodedLocalName = ISO9075Helper.decodeLocalName(uriParts[i]);
                try {
                    decodedLocalName = URLEncoder.encode(decodedLocalName, "utf-8");
                } catch (UnsupportedEncodingException e) {
                   // utf-8 is supported
                }
                encodedUrl.append("/"+decodedLocalName);
            }
        }
        return encodedUrl.toString();
    }
    
    public static String decodeUrl(String url){
        try {
            url =  URLDecoder.decode(url,"utf-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("url not utf-8 encoded.");
        }
        StringBuffer decodedUrl = new StringBuffer();
        if(url.startsWith("/")) {
            url = url.substring(1);
        }
        
        // TODO for the cms tag '2.01.00.13171' we have an issue regarding not encoded date nodes (for example 2008/09/23)
        // therefor for now, a hardcoded fix for nodes below /content/gallery and /content/assets
        boolean isBinary = false;
        if(url.startsWith("content/gallery") || url.startsWith("content/assets")) {
            isBinary = true;
        }
        
        String[] uriParts = url.split("/");
        int nrParts = uriParts.length;
        for(int i = 0 ; i < nrParts ; i++) {
            if(i == nrParts -1) {
                 /*
                  * if it ends with the html postfix and uriPart[i] != uriParts[i-1], it means we have to expand the request to
                  *  /handle/document concept
                  */  
                 if(uriParts[i].endsWith(HTML_POSTFIX) && !uriParts[i].equals(uriParts[i-1])) {
                     String lastPart = uriParts[i].substring(0,uriParts[i].length()-HTML_POSTFIX.length());
                     lastPart = ISO9075Helper.encodeLocalName(lastPart);
                     decodedUrl.append("/"+lastPart+ "/"+lastPart);
                 } else {
                     // TODO currently the last path part can contain a colon (for hippo:resources), which should not be encoded. This might need some
                     // changing when cms changes this behavior.
                     
                     String uriPart = uriParts[i];
                     if(uriPart.contains(":")) {
                         // do not encode
                     }
                     else if(isBinary && isInteger(uriPart)) {
                         // do not encode
                     } else {
                         uriPart = ISO9075Helper.encodeLocalName(uriPart);
                     }
                     decodedUrl.append("/"+uriPart);
                 }
            } else {
                String uriPart = uriParts[i];
                if(isBinary && isInteger(uriPart)) {
                    // do not encode
                } else {
                    uriPart = ISO9075Helper.encodeLocalName(uriPart);
                }
                decodedUrl.append("/"+uriPart);
            }
        }
        return decodedUrl.toString();
    }

    private static boolean isInteger(String uriPart) {
        try {
            Integer.parseInt(uriPart);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
