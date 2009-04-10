package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.hosting.VirtualHostsManager;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstVirtualHostsFilter implements Filter {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(HstVirtualHostsFilter.class);

    private final static String FILTER_DONE_KEY = "filter.done_"+HstVirtualHostsFilter.class.getName();
    private final static String REQUEST_START_TICK_KEY = "request.start_"+HstVirtualHostsFilter.class.getName();
    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        HttpServletRequest req = (HttpServletRequest)request;
        try {
            if(log.isDebugEnabled()) {request.setAttribute(REQUEST_START_TICK_KEY, System.nanoTime());}
            
            if (!HstServices.isAvailable()) {
                String msg = "The HST Container Services are not initialized yet.";
                log.error(msg);
                response.getWriter().println(msg);
                response.flushBuffer();
                return;
            }
            
            if (request.getAttribute(FILTER_DONE_KEY) == null) {
                request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);
                
                String currentRequestURI = req.getRequestURI().substring(req.getContextPath().length());
                
                
                VirtualHostsManager virtualHostManager = HstServices.getComponentManager().getComponent(VirtualHostsManager.class.getName());
            
                //virtualHostManager.getVirtualHosts().findVirtualHost(req.getServerName(), req.getRequestURI());
                VirtualHost virtualHost = virtualHostManager.getVirtualHosts().findVirtualHost(req.getServerName());
                
                if(virtualHost != null) {
                    /*
                     * put the matched VirtualHost temporarily on the request, as we do not yet have a HstRequestContext. When the
                     * HstRequestContext is created, and there is a VirtualHost on the request, we put it on the HstRequestContext.
                     */  
                    request.setAttribute(VirtualHost.class.getName(), virtualHost);
                    if(false) {
                    	// TODO does not yet work, hence if(false)
                        filterConfig.getServletContext().getRequestDispatcher("/preview"+currentRequestURI).forward(request, response);
                    } else {
                        chain.doFilter(request, response);
                    }
                } else {
                    chain.doFilter(request, response);
                }
                
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            if(log.isDebugEnabled()) {
                long starttick = request.getAttribute(REQUEST_START_TICK_KEY) == null ? 0 : (Long)request.getAttribute(REQUEST_START_TICK_KEY);
                if(starttick != 0) {
                    log.debug( "Handling request took --({})-- ms for '{}'.", (System.nanoTime() - starttick)/1000000, req.getRequestURI());
                }
            }
        }
    }

    public void destroy() {

    }

}
