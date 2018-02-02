/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.core.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.mock.util.IteratorEnumeration;

public abstract class MockHstRequestBase implements HstRequest {
    
    private Map<String, Object> props = new HashMap<String, Object>();
    private Map<String, Object> headers = new HashMap<String, Object>();
    private Set<String> userRoleNames = new HashSet<String>();
    private Map<String, Object> attrs = new HashMap<String, Object>();
    private List<Locale> locales = Arrays.asList(new Locale [] { Locale.getDefault() });
    private Map<String, List<String>> params = new HashMap<String, List<String>>();

    private Map<String, Object> modelsMap = new HashMap<String, Object>();
    private Map<String, Object> unmodifiableModelsMap = Collections.unmodifiableMap(modelsMap);

    public String getAuthType() {
        return (String) props.get("authType");
    }
    
    public void setAuthType(String authType) {
        props.put("authType", authType);
    }

    public String getContextPath() {
        return (String) props.get("contextPath");
    }
    
    public void setContextPath(String contextPath) {
        props.put("contextPath", contextPath);
    }

    public Cookie[] getCookies() {
        return (Cookie []) props.get("cookies");
    }
    
    public void setCookies(Cookie[] cookies) {
        props.put("cookies", cookies);
    }
    
    public long getDateHeader(String name) {
        Object v = getHeader(name);
        
        if (v != null) {
            if (v instanceof Long) {
                return ((Long) v).longValue();
            } else if (v instanceof Date) {
                return ((Date) v).getTime();
            } else if (v instanceof Calendar) {
                return ((Calendar) v).getTimeInMillis();
            } else if (v instanceof String) {
                return Long.parseLong((String) v);
            } else {
                throw new IllegalArgumentException("Not a date header: " + v);
            }
        }
        
        return 0;
    }
    
    public String getHeader(String name) {
        Object value = headers.get(name);
        
        if (value != null) {
            if (value instanceof List) {
                return ((List) value).get(0).toString();
            } else {
                return value.toString();
            }
        }
        
        return null;
    }
    
    public void setHeader(String name, Object value) {
        headers.put(name, value);
    }
    
    public void addHeader(String name, Object value) {
        if (!headers.containsKey(name)) {
            headers.put(name, value);
        } else {
            Object v = headers.get(name);
            
            if (v instanceof List) {
                ((List<Object>) v).add(value);
            } else {
                List<Object> list = new ArrayList<Object>();
                list.add(v);
                list.add(value);
                headers.put(name, list);
            }
        }
    }

    public Enumeration getHeaderNames() {
        return new IteratorEnumeration<String>(headers.keySet().iterator());
    }

    public Enumeration getHeaders(String name) {
        Object v = headers.get(name);
        
        if (v != null) {
            List<String> list = new ArrayList<String>();
            if (v instanceof List) {
                for (Object item : (List<Object>) v) {
                    list.add(item.toString());
                }
            } else {
                list.add(v.toString());
            }
            return new IteratorEnumeration<String>(list.iterator());
        }
        
        List<String> emptyList = Collections.emptyList();
        return new IteratorEnumeration<String>(emptyList.iterator());
    }

    public int getIntHeader(String name) {
        Object v = getHeader(name);
        
        if (v != null) {
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            } else if (v instanceof Integer) {
                return Integer.parseInt((String) v);
            } else {
                throw new IllegalArgumentException("Not an integer header: " + v);
            }
        }
        
        return 0;
    }

    public String getMethod() {
        return (String) props.get("method");
    }
    
    public void setMethod(String method) {
        props.put("method", method);
    }

    public String getPathInfo() {
        return (String) props.get("pathInfo");
    }
    
    public void setPathInfo(String pathInfo) {
        props.put("pathInfo", pathInfo);
    }

    public String getPathTranslated() {
        return (String) props.get("pathTranslated");
    }
    
    public void setPathTranslated(String pathTranslated) {
        props.put("pathTranslated", pathTranslated);
    }

    public String getQueryString() {
        return (String) props.get("queryString");
    }

    public void setQueryString(String queryString) {
        props.put("queryString", queryString);
    }
    
    public String getRemoteUser() {
        return (String) props.get("remoteUser");
    }

    public void setRemoteUser(String remoteUser) {
        props.put("remoteUser", remoteUser);
    }

    public String getRequestURI() {
        return (String) props.get("requestURI");
    }

    public void setRequestURI(String requestURI) {
        props.put("requestURI", requestURI);
    }

    public StringBuffer getRequestURL() {
        return (StringBuffer) props.get("requestURL");
    }

    public void setRequestURL(StringBuffer requestURL) {
        props.put("requestURL", requestURL);
    }

    public String getRequestedSessionId() {
        return (String) props.get("requestedSessionId");
    }

    public void setRequestedSessionId(String requestedSessionId) {
        props.put("requestedSessionId", requestedSessionId);
    }

    public String getServletPath() {
        return (String) props.get("servletPath");
    }

    public void setServletPath(String servletPath) {
        props.put("servletPath", servletPath);
    }

    public HttpSession getSession() {
        return (HttpSession) props.get("session");
    }
    
    public void setSession(HttpSession session) {
        props.put("session", session);
    }

    public HttpSession getSession(boolean arg0) {
        if (arg0) {
            throw new UnsupportedOperationException("Not supported yet");
        }
        return (HttpSession) props.get("session");
    }

    public Principal getUserPrincipal() {
        return (Principal) props.get("userPrincipal");
    }
    
    public void setUserPrincipal(Principal userPrincipal) {
        props.put("userPrincipal", userPrincipal);
    }

    public boolean isRequestedSessionIdFromCookie() {
        Boolean v = (Boolean) props.get("requestedSessionIdFromCookie");
        return (v != null && v.booleanValue());
    }
    
    public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
        props.put("requestedSessionIdFromCookie", Boolean.valueOf(requestedSessionIdFromCookie));
    }

    public boolean isRequestedSessionIdFromURL() {
        Boolean v = (Boolean) props.get("requestedSessionIdFromURL");
        return (v != null && v.booleanValue());
    }

    public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
        props.put("requestedSessionIdFromURL", Boolean.valueOf(requestedSessionIdFromURL));
    }
    
    public boolean isRequestedSessionIdFromUrl() {
        Boolean v = (Boolean) props.get("requestedSessionIdFromUrl");
        return (v != null && v.booleanValue());
    }

    @Override
    public boolean authenticate(final HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(final String s, final String s2) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return Collections.emptyList();
    }

    @Override
    public Part getPart(final String s) throws IOException, ServletException {
        return null;
    }

    public void setRequestedSessionIdFromUrl(boolean requestedSessionIdFromUrl) {
        props.put("requestedSessionIdFromUrl", Boolean.valueOf(requestedSessionIdFromUrl));
    }
    
    public boolean isRequestedSessionIdValid() {
        Boolean v = (Boolean) props.get("requestedSessionIdValid");
        return (v != null && v.booleanValue());
    }

    public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
        props.put("requestedSessionIdValid", Boolean.valueOf(requestedSessionIdValid));
    }
    
    public boolean isUserInRole(String roleName) {
        return userRoleNames.contains(roleName);
    }
    
    public Set<String> getUserRoleNames() {
        return userRoleNames;
    }
    
    public void setUserRoleNames(Set<String> userRoleNames) {
        this.userRoleNames = userRoleNames;
    }

    @Override
    public Object getAttribute(String name) {
        Object val = attrs.get(name);
        if (val != null) {
            return val;
        }
        return getModel(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> mergedAttrs = new HashSet<>(getModelsMap().keySet());
        mergedAttrs.addAll(attrs.keySet());
        return new IteratorEnumeration<String>(mergedAttrs.iterator());
    }

    public String getCharacterEncoding() {
        return (String) props.get("characterEncoding");
    }

    public int getContentLength() {
        Integer v = (Integer) props.get("contentLength");
        
        if (v != null) {
            return ((Integer) v).intValue();
        }
        
        return 0;
    }

    public String getContentType() {
        return (String) props.get("contentType");
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getLocalAddr() {
        return (String) props.get("localAddr");
    }
    
    public void setLocalAddr(String localAddr) {
        props.put("localAddr", localAddr);
    }

    public String getLocalName() {
        return (String) props.get("localName");
    }
    
    public void setLocalName(String localName) {
        props.put("localName", localName);
    }

    public int getLocalPort() {
        Integer v = (Integer) props.get("localPort");
        
        if (v != null) {
            return ((Integer) v).intValue();
        }
        
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    public void setLocalPort(int localPort) {
        props.put("localPort", Integer.valueOf(localPort));
    }

    public Locale getLocale() {
        return (Locale) props.get("locale");
    }
    
    public void setLocale(Locale locale) {
        props.put("locale", locale);
    }

    public Enumeration getLocales() {
        List<Locale> list = new ArrayList<Locale>();
        
        if (props.containsKey("locale")) {
            list.add((Locale) props.get("locale"));
            list.addAll(locales);
        }
        
        return new IteratorEnumeration(list.iterator());
    }
    
    public void setLocales(List<Locale> locales) {
        this.locales = locales;
    }

    public String getParameter(String name) {
        if (params.containsKey(name)) {
            List<String> values = params.get(name);
            
            if (!values.isEmpty()) {
                return values.get(0);
            }
        }
        
        return null;
    }
    
    public void addParameter(String name, String value) {
        List<String> values = params.get(name);
        
        if (values == null) {
            values = new ArrayList<String>();
            params.put(name, values);
        } 
        
        values.add(value);
    }
    
    public void removeParameter(String name) {
        params.remove(name);
    }

    public Map getParameterMap() {
        Map<String, String []> map = new HashMap<String, String []>();
        
        for (String name : params.keySet()) {
            map.put(name, params.get(name).toArray(new String[0]));
        }
        
        return map;
    }

    public Enumeration getParameterNames() {
        return new IteratorEnumeration(params.keySet().iterator());
    }

    public String[] getParameterValues(String name) {
        if (params.containsKey(name)) {
            return params.get(name).toArray(new String[0]);
        }
        
        return null;
    }

    public String getProtocol() {
        return (String) props.get("protocol");
    }
    
    public void setProtocol(String protocol) {
        props.put("protocol", protocol);
    }

    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getRealPath(String path) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getRemoteAddr() {
        return (String) props.get("remoteAddr");
    }
    
    public void setRemoteAddr(String remoteAddr) {
        props.put("remoteAddr", remoteAddr);
    }

    public String getRemoteHost() {
        return (String) props.get("remoteHost");
    }
    
    public void setRemoteHost(String remoteHost) {
        props.put("remoteHost", remoteHost);
    }

    public int getRemotePort() {
        Integer v = (Integer) props.get("remotePort");
        
        if (v != null) {
            return ((Integer) v).intValue();
        }
        
        return 0;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    public String getScheme() {
        return (String) props.get("scheme");
    }
    
    public void setScheme(String scheme) {
        props.put("scheme", scheme);
    }

    public String getServerName() {
        return (String) props.get("serverName");
    }
    
    public void setServerName(String serverName) {
        props.put("serverName", serverName);
    }

    public int getServerPort() {
        Integer v = (Integer) props.get("serverPort");
        
        if (v != null) {
            return ((Integer) v).intValue();
        }
        
        return 0;
    }

    public boolean isSecure() {
        Boolean v = (Boolean) props.get("secure");
        return (v != null && v.booleanValue());
    }

    public void removeAttribute(String name) {
        attrs.remove(name);
    }

    public void setAttribute(String name, Object value) {
        attrs.put(name, value);
    }

    public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException {
        props.put("characterEncoding", characterEncoding);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getModel(String name) {
        return (T) getModelsMap().get(name);
    }

    @Override
    public Enumeration<String> getModelNames() {
        return Collections.enumeration(getModelsMap().keySet());
    }

    @Override
    public Map<String, Object> getModelsMap() {
        return unmodifiableModelsMap;
    }

    @Override
    public Object setModel(String name, Object model) {
        return modelsMap.put(name, model);
    }

    @Override
    public void removeModel(String name) {
        modelsMap.remove(name);
    }
}
