/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.demo.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Date Time Servlet to demonstrate named dispatching
 * @version $Id$
 */
public class DateTimeServlet  extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private String formatPattern;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        formatPattern = config.getInitParameter("formatPattern");
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String line = null;
        Date d = new Date();
        
        if (formatPattern == null) {
            line = d.toString();
        } else {
            line = new SimpleDateFormat(formatPattern).format(d);
        }
        
        out.print(line);
        out.flush();
    }
}
