package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Factory implementation for creating HTTP Response Wrappers
 * @version $Id$
 */
public class HstResponseImpl extends HttpServletResponseWrapper implements HstResponse
{
    protected HstRequestContext requestContext;
    protected HstComponentWindow componentWindow;
    protected HstResponseState responseState;
    protected String redirectLocation;
    
    public HstResponseImpl(HttpServletResponse response, HstRequestContext requestContext, HstComponentWindow componentWindow, HstResponseState responseState) {
        super(response);
        this.requestContext = requestContext;
        this.componentWindow = componentWindow;
        this.responseState = responseState;
    }
    
    public HstURL createURL(String type) {
        return this.requestContext.createURL(type, this.componentWindow.getReferenceNamespace());
    }

    public void setResponse(HttpServletResponse response) {
        super.setResponse(response);
    }
    
    public String getRedirectLocation() {
        return this.redirectLocation;
    }
    @Override
    public void addCookie(Cookie cookie)
    {
        responseState.addCookie(cookie);
    }

    @Override
    public void addDateHeader(String name, long date)
    {
        responseState.addDateHeader(name, date);
    }

    @Override
    public void addHeader(String name, String value)
    {
        responseState.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        responseState.addIntHeader(name, value);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return responseState.containsHeader(name);
    }

    @Override
    public void flushBuffer() throws IOException
    {
        responseState.flushBuffer();
    }

    @Override
    public int getBufferSize()
    {
        return responseState.getBufferSize();
    }

    @Override
    public String getCharacterEncoding()
    {
        return responseState.getCharacterEncoding();
    }

    public String getContentType()
    {
        return responseState.getContentType();
    }

    @Override
    public Locale getLocale()
    {
        return responseState.getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        ServletOutputStream os = responseState.getOutputStream();
        return os != null ? os : super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        PrintWriter pw = responseState.getWriter();
        return pw != null ? pw : super.getWriter();
    }

    @Override
    public boolean isCommitted()
    {
        return responseState.isCommitted();
    }

    @Override
    public void reset()
    {
        responseState.reset();
    }

    @Override
    public void resetBuffer()
    {
        responseState.resetBuffer();
    }

    @Override
    public void sendError(int errorCode, String errorMessage) throws IOException
    {
        responseState.sendError(errorCode, errorMessage);
    }

    @Override
    public void sendError(int errorCode) throws IOException
    {
        responseState.sendError(errorCode);
    }

    @Override
    public void sendRedirect(String redirectLocation) throws IOException
    {
        responseState.sendRedirect(redirectLocation);
    }

    @Override
    public void setBufferSize(int size)
    {
        responseState.setBufferSize(size);
    }

    public void setCharacterEncoding(String charset)
    {
        responseState.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int len)
    {
        responseState.setContentLength(len);
    }

    @Override
    public void setContentType(String type)
    {
        responseState.setContentType(type);
    }

    @Override
    public void setDateHeader(String name, long date)
    {
        responseState.setDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value)
    {
        responseState.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        responseState.setIntHeader(name, value);
    }

    @Override
    public void setLocale(Locale locale)
    {
        responseState.setLocale(locale);
    }

    @Override
    public void setStatus(int statusCode, String message)
    {
        responseState.setStatus(statusCode, message);
    }

    @Override
    public void setStatus(int statusCode)
    {
        responseState.setStatus(statusCode);
    }

    @Override
    public String encodeRedirectUrl(String url)
    {
        return encodeRedirectURL(url);
    }

    @Override
    public String encodeRedirectURL(String url)
    {
        return url;
    }

    @Override
    public String encodeUrl(String url)
    {
        return encodeURL(url);
    }

    @Override
    public String encodeURL(String url)
    {
        if (url.indexOf("://") == -1 && !url.startsWith("/"))
        {
            // The Portlet Spec only allows URL encoding for absolute or full path URIs
            // Letting this pass through thus would lead to an IllegalArgumentException been thrown.

            // TODO: figure out how this can be fixed properly which is *not* trivial as it is
            // difficult (impossible?) to detect upfront if such an url would later on need to be
            // encoded as PortletURL (in which case it should *not* be encoded upfront...

            // Also note: Tomcat does *not* encode the url when called from within a context other
            // than the originating request context (e.g. during cross-context calls) anyway...
            return url;
        }
        return super.encodeURL(url);
    }
}
