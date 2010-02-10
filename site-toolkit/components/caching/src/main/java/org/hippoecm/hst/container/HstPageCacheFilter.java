/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.container;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * TODO this is a work in progress HST Page Cache filter, which is an HST optimization for high traffic / large concurrency environments, where
 * throughput must be extremely large. A cached valid page won't hit the hst2 application at all, and will be served from cache. Easily thousands to tens of
 * of thousands page request per second can be handled by this filter. 
 * 
 * in the web.xml you can use this filter with 
 * 
 * <dispatchet>FORWARD</dispatcher> 
 * 
 * as the request will be forwarded from the HstVirtualHostsFilter
 * 
 * TODO 
 * <UL>
 *    <LI> Configure the cache characteristics in the repository / web.xml / hst config properties </LI>
 *    <LI> Add correct headers to response</LI>
 *    <LI> Repository invalidation when a change happens</LI>
 *    <LI> Seperate caches for preview / live </LI>
 *    <LI> Allow for exclusions (for example everything below /bar)</LI>
 *    <LI> Do not cache actions (can be set on the HstResponse to not cache)</LI>
 *    <LI> Send a 304 for not changed pages, and handle a ctrl-refresh</LI>
 *    <LI> Make concurrent calls for the exact same page wait until the first request returns, and serve the other requests from cache</LI>
 *    <LI> Investigate a clustered cache where all HST instances share their cache</LI>
 * </UL>
 */
public class HstPageCacheFilter implements Filter {

    private static final long serialVersionUID = 1L;

    private CacheManager cacheManager;
    private Cache memCache;
    
    public void init(FilterConfig filterConfig) throws ServletException {
        cacheManager = CacheManager.create();
        memCache = new Cache("pageCache", 5000, false, false, 3600*24 , 3600*24);
        cacheManager.addCache(memCache);
    }

   
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        HstResponseWrapper responseWrapper = null;
        
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        
        String pageKey = getPageKey(req);
        
        Element element = memCache.get(pageKey);
      
        if(element != null) {
            Serializable value = element.getValue();
            if(value instanceof char[]) {
                res.getWriter().write(((char[])value));
                return;
            }
            if(value instanceof String) {
                res.getWriter().write(((String)value));
                return;
            }
        } 
        
        try {
            responseWrapper = new HstResponseWrapper((HttpServletResponse)res);
            chain.doFilter(request, responseWrapper);
         } finally {
            if(responseWrapper != null && pageKey != null) {
                Element output = new Element(pageKey, responseWrapper.charWriter.toCharArray());
                memCache.put(output);   
            }
        }
    }

    protected String getPageKey(HttpServletRequest req) {
        StringBuilder keyBuilder = new StringBuilder(req.getRequestURI() );
        @SuppressWarnings("unchecked")
        Enumeration<String> params =  req.getParameterNames();
        while(params.hasMoreElements()) {
            Object param = params.nextElement();
            if(param instanceof String) {
                keyBuilder.append((String)param).append('\uFFFF').append(req.getParameter((String)param)).append('\uFFFE');
            }
        }
       return keyBuilder.toString();
    }

    public void destroy() {
        if(memCache != null) {
            memCache.dispose();
        }
        cacheManager.clearAll();
        cacheManager.removalAll();
    }
    
    class HstResponseWrapper extends HttpServletResponseWrapper {

        protected CharArrayWriter charWriter = new CharArrayWriter();
        
        public HstResponseWrapper(HttpServletResponse response) {
            super(response);
        }
        
        public PrintWriter getWriter() throws IOException {
            PrintWriter printWriter = getResponse().getWriter();
            return new RecordingPrintWriter(printWriter, charWriter);
        }
        
    }
    
    class RecordingPrintWriter extends PrintWriter {

        private CharArrayWriter charWriter;

        public RecordingPrintWriter(PrintWriter printWriter, CharArrayWriter charWriter) {
            super(printWriter);
            this.charWriter = charWriter;
        }

        @Override
        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            PrintWriter recorderWriter = new PrintWriter(charWriter);
            recorderWriter.write(buf, off, len);
            recorderWriter.flush();
        }

        @Override
        public void write(int c) {
            super.write(c);
            charWriter.write(c);

        }

        @Override
        public void write(String s, int off, int len) {
            super.write(s, off, len);
            PrintWriter recorderWriter = new PrintWriter(charWriter);
            recorderWriter.write(s, off, len);
            recorderWriter.flush();
        }

    }

}
