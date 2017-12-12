package org.onehippo.cms7.utilities.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyFilter implements Filter{

    private static final Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    private Map<String, String> proxies;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        proxies = new HashMap<>();
        proxies.put("/hippo-projects/","http://localhost:9092/");
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String resourcePath = StringUtils.substringBefore(httpServletRequest.getPathInfo(), ";");
        if (resourcePath==null){
            chain.doFilter(request,response);
            return;
        }
        String query = httpServletRequest.getQueryString();
        final String queryParams = StringUtils.isEmpty(query) ? "" : "?" + query;

        final Optional<RequestDispatcher> requestDispatcher = proxies.entrySet().stream().filter(e -> resourcePath.startsWith(e.getKey())).map(
                e -> request.getRequestDispatcher(getUrl(e.getKey(), e.getValue(), resourcePath, queryParams))
        ).findFirst();

        if (requestDispatcher.isPresent()){
            requestDispatcher.get().forward(request,response);
        }
        else{
            chain.doFilter(request,response);
        }
    }

    private String getUrl(String to, String from, String resourcePath, String queryParams){
        return to + StringUtils.substringAfter(resourcePath,from) + queryParams;
    }

    @Override
    public void destroy() {

    }
}
