package org.hippoecm.hst.test.mock;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MockJspServlet extends HttpServlet {
    
    public void init(ServletConfig config) throws ServletException {
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String myPath = (String) req.getAttribute("javax.servlet.include_url");
        System.out.println("[JSP] " + myPath);
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    public void destroy() {
    }

}
