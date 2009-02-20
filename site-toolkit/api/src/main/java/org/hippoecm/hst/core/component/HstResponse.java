package org.hippoecm.hst.core.component;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

public interface HstResponse extends HttpServletResponse {
    
    /**
     * Creates a HST URL targeting the HstComponent.
     * The created URL will per default not contain any parameters of the current request.
     *  
     * @return
     */
    HstURL createURL(String type);
    
    /**
     * Creates an element of the type specified to be used in the {@link #addProperty(String, Element)} method. 
     * 
     * @param tagName the tag name of the element
     * @return DOM element with the tagName
     */
    Element createElement(String tagName);
    
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
    void addProperty(String key, Element element);
    
    /**
     * Retrieves header element property map.
     * This method is supposed to be invoked by the parent HstComponent
     * to render some header tag elements in a non-portal environment.
     * Under portal environment, this method is not supposed to be invoked
     * because the header tag elements should be written by the portal.
     * Under portal environment, the HstComponents can write some body
     * tag fragments only. If a HstComponent contributes some header
     * tag elements by invoking {@link #addProperty(String, Element)},
     * then the portal will write all the merged head tag elements finally.
     * 
     * @param key
     * @param element
     */
    Map<String, Element> getProperties();
    
}
