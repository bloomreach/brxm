package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class HttpBufferedResponse extends javax.servlet.http.HttpServletResponseWrapper
{
    private boolean usingWriter;
    private boolean usingStream;

    /** Commons logging */
    protected final static Log log = LogFactory.getLog(HttpBufferedResponse.class);

    private ServletOutputStream wrappedStream;
    private PrintWriter writer;

    public HttpBufferedResponse(HttpServletResponse servletResponse,
                                PrintWriter writer)
    {
        super(servletResponse);
        this.writer = writer;
    }

    public ServletOutputStream getOutputStream() throws IllegalStateException, IOException
    {
        if (usingWriter)
        {
            throw new IllegalStateException("getOutputStream can't be used after getWriter was invoked");
        }

        if (wrappedStream == null)
        {            
            wrappedStream = new PrintWriterServletOutputStream(writer, getResponse().getCharacterEncoding());                                                               
        }

        usingStream = true;

        return wrappedStream;
    }

    public PrintWriter getWriter() throws UnsupportedEncodingException, IllegalStateException, IOException {

        if (usingStream)
        {
            throw new IllegalStateException("getWriter can't be used after getOutputStream was invoked");
        }

        usingWriter = true;

        return writer;
    }


    public void setBufferSize(int size)
    {
        // ignore
    }

    public int getBufferSize()
    {
        return 0;
    }

    public void flushBuffer() throws IOException
    {
        writer.flush();
    }

    public boolean isCommitted()
    {
        return false;
    }

    public void reset()
    {
        // ignore right now
    }
}
