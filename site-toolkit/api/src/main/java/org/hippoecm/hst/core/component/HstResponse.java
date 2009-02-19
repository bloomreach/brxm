package org.hippoecm.hst.core.component;

import javax.servlet.http.HttpServletResponse;

public interface HstResponse extends HttpServletResponse {
    
    /**
     * Creates a HST URL targeting the HstComponent.
     * The created URL will per default not contain any parameters of the current request.
     *  
     * @return
     */
    HstURL createURL(String type);
    
    /**
     * Adds an header element property to the response.
     * If a header element with the provided key already exists 
     * the provided element will be stored in addition to 
     * the existing element under the same key.
     * If the element is null the key is removed from the response.
     * If these header values are intended to be transmitted to the client 
     * they should be set before the response is committed.
     * 
     * @param key
     * @param element
     */
    void addHeaderProperty(String key, String headerElement);
    
}
