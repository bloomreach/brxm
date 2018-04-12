/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The <CODE>HstResponse</CODE> defines the interface to assist a
 * HstComponent in creating and sending a response to the client. The HstComponent
 * container uses specialized versions of this interface when invoking a
 * HstComponent.
 * The HstComponent container creates these objects and passes them as arguments to
 * the HstComponent's <CODE>doAction, doBeforeRender</CODE> and <CODE>doBeforeServeResource</CODE> methods.
 * 
 * @version $Id$
 */
public interface HstResponse extends HttpServletResponse {
    
    /**
     * Creates a HST Render URL targeting the HstComponent.
     */
    HstURL createRenderURL();
    
    /**
     * Creates a HST Navigational Render URL
     * 
     * @param pathInfo the path to be navigated
     */
    HstURL createNavigationalURL(String pathInfo);
    
    /**
     * Creates a HST Action URL targeting the HstComponent.
     */
    HstURL createActionURL();

    /**
     * Creates a HST Resource URL targeting the current HstComponent.
     */
    HstURL createResourceURL();
    
    /**
     * Creates a HST Resource URL targeting the HstComponent indicated by referenceNamespace.
     * 
     * @param referenceNamespace
     */
    HstURL createResourceURL(String referenceNamespace);

    /**
     * Creates a HST component rendering URL targeting a specific HstComponent
     * @return a component rendering ULR
     */
    HstURL createComponentRenderingURL();

    /**
     * The value returned by this method should be prefixed or appended to elements, 
     * such as JavaScript variables or function names, to ensure they are unique 
     * in the context of the HST-managed page.
     * The namespace value must be constant for the lifetime of the HstComponentWindow. 
     */
    String getNamespace();
    
    /**
     * Sets a String parameter for the rendering phase.
     * These parameters will be accessible in all sub-sequent render calls
     * via the HstRequest.getParameter call until a request is targeted to the Hst component.
     * This method replaces all parameters with the given key.
     * The given parameter do not need to be encoded prior to calling this method. 
     * 
     * @param key key of the render parameter
     * @param value value of the render parameter 
     */
    void setRenderParameter(String key, String value); 
    
    /**
     * Sets a String parameter for the rendering phase.
     * These parameters will be accessible in all sub-sequent render calls
     * via the HstRequest.getParameter call until a request is targeted to the Hst component.
     * This method replaces all parameters with the given key.
     * The given parameter do not need to be encoded prior to calling this method. 
     * 
     * @param key key of the render parameter
     * @param values values of the render parameters
     */
    void setRenderParameter(String key, String[] values);
    
    /**
     * Sets a parameter map for the render request.
     * All previously set render parameters are cleared.
     * These parameters will be accessible in all sub-sequent renderinrg phase
     * via the HstRequest.getParameter call until a new request is targeted to the Hst component.
     * The given parameters do not need to be encoded prior to calling this method.
     * The Hst component should not modify the map any further after calling this method.
     * 
     * @param parameters
     */
    void setRenderParameters(Map<String, String[]> parameters);
    
    /**
     * Creates an element of the type specified to be used in the {@link #addPreamble(org.w3c.dom.Element)} method.
     * 
     * @param tagName the tag name of the element
     * @return DOM element with the tagName
     */
    Element createElement(String tagName);

    /**
     * Creates a comment element
     *
     * @param comment the comment text
     * @return Comment DOM element with the text as content
     */
    Comment createComment(String comment);

    /**
     * Adds an header element property to the response.
     * If keyHint argument is provided and if a header element 
     * with the provided key hint already exists, then 
     * the element will be ignored.
     * If the element is null the key is removed from the response.
     * If these header values are intended to be transmitted to the client 
     * they should be set before the response is committed.
     * 
     * @param element
     * @param keyHint
     */
    void addHeadElement(Element element, String keyHint);
    
    /**
     * Retrieves header element list.
     * This method is supposed to be invoked by the parent HstComponent
     * to render some header tag elements.
     * 
     * @return List with head element items
     */
    List<Element> getHeadElements();

    /**
     * Asks if a property exists already or not.
     * This method checks all the parent component reponses have the property.
     * 
     * @param keyHint
     */
    boolean containsHeadElement(String keyHint);

    void addProcessedHeadElement(Element headElement);

     /**
     * Sets the renderPath dynamically.
     * Normally, the renderPath is set in the configuration, but it can be
     * set dynamically in the {@link HstComponent#doBeforeRender(HstRequest, HstResponse)} method.
     * 
     * @param renderPath
     */
    void setRenderPath(String renderPath);
    
    /**
     * Sets the serveResourcePath dynamically.
     * Normally, the serveResourcePath is set in the configuration, but it can be
     * set dynamically in the {@link HstComponent#doBeforeServeResource(HstRequest, HstResponse)} method.
     * 
     * @param serveResourcePath
     */
    void setServeResourcePath(String serveResourcePath);
    
    /**
     * Flushes the buffer of child window.
     * <P>
     * <EM>Note: the child content can be flushed only once. 
     * If it is already flushed, then the next invocations will not make any effect.</EM>
     * </P>
     * 
     * @param name the name of the child window
     * @throws IOException
     */
    void flushChildContent(String name) throws IOException;

    /**
     * Flushes the child window, and writes its content to the {@link Writer}. Note that not everything that gets
     * flushed ends up in the <code>writer</code>, for example head contributions not.
     * @param name the name of the child window to flush
     * @param writer the {@link Writer} the content gets flushed to
     */
    void flushChildContent(String name, Writer writer) throws IOException;
    
    /**
     * Returns the flushable child content window names.
     */
    List<String> getChildContentNames();
    
    /**
     * Sends a temporary redirect response to the client using the specified redirect location URL.
     * <P>
     * In either {@link HstComponent#doAction(HstRequest, HstResponse)} or {@link HstComponent#doBeforeRender(HstRequest, HstResponse)} ,
     * the invocation on this method could be effective.
     * If the invocation on this method is done in other methods,
     * the invocation will be just ignored with no operation.
     * </P>
     *
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(String)
     * @param location the redirect location URL
     */
    void sendRedirect(String location) throws IOException;
    
    /**
     * Sends an error response to the client using the specified status.
     * <P>
     * Only in {@link HstComponent#doAction(HstRequest, HstResponse)}, 
     * {@link HstComponent#doBeforeRender(HstRequest, HstResponse)} 
     * or {@link HstComponent#doBeforeServeResource(HstRequest, HstResponse)},
     * the invocation on this method will be effective.
     * If the invocation on this method is done in a view page during render phase,
     * the invocation will be just ignored with no operation.
     * </P>
     * 
     * @param sc the error status code
     * @param msg the descriptive message
     * @throws IOException If the response was committed
     */
    void sendError(int sc, String msg) throws IOException;
    
    /**
     * Sends an error response to the client using the specified status.
     * <P>
     * Only in {@link HstComponent#doAction(HstRequest, HstResponse)}, 
     * {@link HstComponent#doBeforeRender(HstRequest, HstResponse)} 
     * or {@link HstComponent#doBeforeServeResource(HstRequest, HstResponse)},
     * the invocation on this method will be effective.
     * If the invocation on this method is done in a view page during render phase,
     * the invocation will be just ignored with no operation.
     * </P>
     * 
     * @param sc the error status code
     * @throws IOException If the response was committed
     */
    void sendError(int sc) throws  IOException;
    
    /**
     * Sets the status code for this response. 
     * This method is used to set the return status code when there is no error 
     * (for example, for the status codes SC_OK or SC_MOVED_TEMPORARILY). 
     * If there is an error, and the caller wishes to invoke an error-page defined in the web application, 
     * the sendError method should be used instead.
     * <P>
     * If there are multiple HST components to invoke this method, then the last invocation will be applied.
     * </P>
     */
    void setStatus(int sc);
    
    /**
     * Forwards the page request to pathInfo.
     * <P>
     * Only in {@link HstComponent#doBeforeRender(HstRequest, HstResponse)},
     * the invocation on this method will be effective.
     * If the invocation on this method is done in a view page during render phase,
     * the invocation will be just ignored with no operation.
     * </P>
     * 
     * @param pathInfo the path info to forward. It should start with a "/" and is relative to the root of your hst sitemap
     * @throws IOException If the response was committed
     */
    void forward(String pathInfo) throws IOException;
    
    /**
     * Adds the specified cookie to the response. This method can be called
     * multiple times to set more than one cookie.
     * 
     * <P><EM>Note: Please be sure to set cookie path before adding cookie for safety
     * like the following example:</EM></P>
     * <PRE>{@code Cookie cookie = new Cookie("testcookie", "testvalue");
     * cookie.setPath("/");
     * response.addCookie(cookie);
     * }</PRE>
     * <P>
     * Sometimes, a user agent could not accept cookies
     * when the response triggers page redirection with Set-Cookie header.
     * It's possibly because it regards the state as an unverifiable transaction
     * for some reason.
     * By setting path, the cookie can be specified more as a subset of the current domain,
     * and this makes sure that the cookie accepted safely by the user agent.
     * </P>
     * 
     * @param cookie the Cookie to return to the client
     */
    void addCookie(Cookie cookie);
    
    /**
     * Sets wrapper element for the rendered content
     * @param element
     */
    void setWrapperElement(Element element);
    
    /**
     * Returns the wrapper element for the rendered content
     */
    Element getWrapperElement();

    /**
     * Add a preamble comment node, which gets rendered at the beginning of the render
     * window.
     *
     * @param comment the comment node
     */
    void addPreamble(Comment comment);


    /**
     * Preamble {@link org.w3c.dom.Node}s are written before the rest of the content of this {@link HstResponseState}. Note that
     * from this <code>element</code> <b>only</b> the element, attributes and text of the <code>element</code> are printed, and *not*
     * any descendant @link org.w3c.dom.Node}s of <code>element</code>
     * @param element the element that is a preamble
     */
    void addPreamble(Element element);

    /**
     * Return unmodifiable preamble {@link org.w3c.dom.Node}s.
     * @return unmodifiable preamble {@link org.w3c.dom.Node}s
     */
    List<Node> getPreambleNodes();

    /**
     * Add an epilogue comment, which gets rendered at the end of the render window.
     *
     * @param comment the comment node
     */
    void addEpilogue(Comment comment);

    /**
     * Return unmodifiable epilogue {@link org.w3c.dom.Node}s.
     * @return unmodifiable epilogue {@link org.w3c.dom.Node}s
     */
    List<Node> getEpilogueNodes();

    /**
     * For single /subtree component rendering mode that has {@link HstComponentInfo#isStandalone()} equal to <code>false</code>, this
     * check can be used whether some {@link HstComponent} won't get its renderer called. In other words, this is for performance optimization 
     * to short-circuit the doBeforeRender for components that won't get rendered any way.
     * @return <code>true</code> when for this {@link HstResponse} the renderer won't be invoked
     */
    boolean isRendererSkipped();

}
