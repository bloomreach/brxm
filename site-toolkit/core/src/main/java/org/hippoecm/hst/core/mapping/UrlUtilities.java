package org.hippoecm.hst.core.mapping;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
                encodedUrl.append("/"+ISO9075Helper.decodeLocalName(uriParts[i]));
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
        String[] uriParts = url.split("/");
        int nrParts = uriParts.length;
        for(int i = 0 ; i < nrParts ; i++) {
            if(i == nrParts -1) {
                 /*
                  * if it ends with the html postfix and uriPart[i] != uriParts[i-1], it means we have to expand the request to
                  *  /handle/document concept
                  */  
                 if(uriParts[i].endsWith(HTML_POSTFIX) && !uriParts[i].equals(uriParts[i-1])) {
                     // For now, just replace " " with "_x0020_" to get jcr path
                     String lastPart = uriParts[i].substring(0,uriParts[i].length()-HTML_POSTFIX.length()).replaceAll(" ", "_x0020_");
                     decodedUrl.append("/"+lastPart+ "/"+lastPart);
                 } else {
                     decodedUrl.append("/"+uriParts[i].replaceAll(" ", "_x0020_"));
                 }
            } else {
                decodedUrl.append("/"+uriParts[i].replaceAll(" ", "_x0020_"));
            }
        }
        
        // for decoding a url, you have to encode the url to get jcr node paths
        return decodedUrl.toString();
    }
}
