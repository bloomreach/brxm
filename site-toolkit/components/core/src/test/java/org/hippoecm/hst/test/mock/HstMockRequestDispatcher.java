package org.hippoecm.hst.test.mock;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.springframework.mock.web.MockRequestDispatcher;

public class HstMockRequestDispatcher extends MockRequestDispatcher {

    protected HttpServlet servlet;
    
    public HstMockRequestDispatcher(String url, HttpServlet servlet) {
        super(url);
        this.servlet = servlet;
    }
    
    public void include(ServletRequest request, ServletResponse response) {
        
        try {
            servlet.service(request, response);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
}
