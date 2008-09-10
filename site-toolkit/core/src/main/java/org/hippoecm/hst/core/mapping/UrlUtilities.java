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
        // for encoding a url, you have to decode the jcr node paths :-)
        if(rewrite.startsWith("/")) {
            rewrite = rewrite.substring(1);
        }
        String[] uriParts = rewrite.split("/");
        int nrParts = uriParts.length;
        StringBuffer encodedUrl = new StringBuffer(contextPath);
        boolean replaceDup = false;
        if(nrParts > uriLevels) {
            if(uriParts[nrParts-1].equals(uriParts[nrParts-2])) {
                replaceDup = true; 
            }
        }
        for(int i = 0; i < nrParts; i++) {
            if(replaceDup && i == nrParts-1) {
                encodedUrl.append(HTML_POSTFIX);
            } else {
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
                 if(uriParts[i].endsWith(HTML_POSTFIX)) {
                     String lastPart = uriParts[i].substring(0,uriParts[i].length()-HTML_POSTFIX.length());
                     decodedUrl.append("/"+ISO9075Helper.encodeLocalName(lastPart)+ "/"+ISO9075Helper.encodeLocalName(lastPart));
                 } else {
                     decodedUrl.append("/"+ISO9075Helper.encodeLocalName(uriParts[i]));
                 }
            } else {
                decodedUrl.append("/"+ISO9075Helper.encodeLocalName(uriParts[i]));
            }
        }
        
        // for decoding a url, you have to encode the url to get jcr node paths
        return decodedUrl.toString();
    }
}
