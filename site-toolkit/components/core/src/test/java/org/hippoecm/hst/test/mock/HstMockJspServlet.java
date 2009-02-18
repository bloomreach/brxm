package org.hippoecm.hst.test.mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

public class HstMockJspServlet extends HttpServlet {
    
    public void init(ServletConfig config) throws ServletException {
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String myPath = (String) req.getAttribute("javax.servlet.include_url");
        
        StringBuilder content = new StringBuilder().append("JSP output from the page: " + myPath);
        
        System.out.println(content);
        
        PrintWriter out = res.getWriter();
        out.println(content);
        out.flush();

        // add children here.
        if (req instanceof HstRequest) {
            HstComponentWindow myWindow = ((HstRequest) req).getComponentWindow();
            Map<String, HstComponentWindow> childWindowMap = myWindow.getChildWindowMap();
            
            if (childWindowMap != null) {
                for (Map.Entry<String, HstComponentWindow> entry : childWindowMap.entrySet()) {
                    HstComponentWindow childWindow = entry.getValue();
                    childWindow.flushContent();
                }
            }
        }

    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    public void destroy() {
    }

}
