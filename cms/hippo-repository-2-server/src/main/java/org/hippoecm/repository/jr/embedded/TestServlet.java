/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.repository.jr.embedded;

import java.rmi.Remote;
import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

public class TestServlet extends HttpServlet {
    String bindingAddress = null;
    String callingAddress = null;

    public TestServlet() {
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.setProperty("java.rmi.server.useCodebaseOnly", "true");
        bindingAddress = config.getInitParameter("bind-address");
        callingAddress = config.getInitParameter("call-address");
        if ((bindingAddress == null || bindingAddress.equals(""))
                && (callingAddress == null || callingAddress.equals("")))
            bindingAddress = callingAddress = "rmi://localhost:1099/test";
        if (callingAddress == null || callingAddress.equals(""))
            if (bindingAddress != null && !bindingAddress.equals(""))
                callingAddress = bindingAddress;
        try {
            if (bindingAddress != null && !bindingAddress.equals("")) {
                Context ctx = new InitialContext();
                Remote remote = new TestImpl();
                ctx.bind(bindingAddress, remote); // java.rmi.Naming.rebind("DB1", db);
                System.err.println("RMI Server " + config.getServletName() + " available on " + bindingAddress);
            }
        } catch (NamingException ex) {
            System.err.println("NamingException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RemoteException ex) {
            System.err.println("RemoteException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.startsWith(req.getContextPath()))
            path = path.substring(req.getContextPath().length());
        if (path.startsWith(req.getServletPath()))
            path = path.substring(req.getServletPath().length());
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        writer.println("<html><body>");
        writer.println("  <h1>Hippo Repository Test Servlet</h1>");
        try {
            Context ctx = new InitialContext();
            Test test = (Test) ctx.lookup(callingAddress);
            int randValue = test.test(666);
            writer.println("  Called test service using context and got value " + randValue + ".<br>");
            writer.println("  Test service is of class: " + test.getClass().getName());
        } catch (NamingException ex) {
            writer.println("  Error while retrieving service:<br><pre>" + ex.getMessage() + "</pre>");
        }
        writer.println("</body></html>");
    }

    public void destroy() {
        try {
            Context ctx = new InitialContext();
            ctx.unbind(bindingAddress);
        } catch (NamingException ex) {
            System.err.println("NamimgException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
