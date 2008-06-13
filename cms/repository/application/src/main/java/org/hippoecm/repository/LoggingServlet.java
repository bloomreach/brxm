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

package org.hippoecm.repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoggingServlet extends HttpServlet {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingServlet.class);

    Level[] levels = new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL };

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();

        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.println("<head><title>Hippo Repository Console</title>");
        writer.println("<style type=\"text/css\">");
        writer.println(" table.params {font-size:small}");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("  <h2>Hippo Repository Console</h2>");
        writer.println("  <h3>Request parameters</h3>");
        writer.println("    <table style=\"params\" summary=\"request parameters\"><tr><th>name</th><th>value</th></tr>");
        writer.println("    <tr><td>context path</td><td>: <code>" + req.getContextPath() + "</code></td></tr>");
        writer.println("    <tr><td>servlet path</td><td>: <code>" + req.getServletPath() + "</code></td></tr>");
        writer.println("    <tr><td>request uri</td><td>: <code>" + req.getRequestURI() + "</code></td></tr>");
        /*
         * for(Enumeration<String> e = req.getParameterNames(); e.hasMoreElements(); ) {
         *    String param = e.nextElement();
         *    writer.println("    <tr><td><em>"+param+ "</em></td><td>: <code>"+req.getParameter(param)+"</code></td></tr>");
         * }
         */
        writer.println("    </table>");
        writer.println("  <h3>Logging</h3>");

        writer.println("<p>Logger in use: "+log.getClass().getName()+"</p><p>\n");
        if(log.getClass().getName().equals("org.slf4j.impl.JDK14LoggerAdapter")) {
            writer.println("  <form action=\".\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\">");
            writer.println("    <table>");
            writer.println("      <tr><th>logger</th><th>level</th><th>change to</th></tr>");

            LoggerHierarchy logs = new LoggerHierarchy();
            LogManager manager = LogManager.getLogManager();
            for(Enumeration<String> namesIter = manager.getLoggerNames(); namesIter.hasMoreElements(); ) {
                String loggerName = namesIter.nextElement();
                Logger logger = manager.getLogger(loggerName);
                logs.add(logger);
            }

            logs.print(writer,"","");
            writer.println("    </table>");
            writer.println("    <input type=\"submit\" name=\"submit\" value=\"Apply\">");
            writer.println("  </form>");
        } else {
            writer.println("Since no JDK logging in use there is no further information.");
        }

        writer.println("</p>");
        writer.println("</body></html>");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if(log.getClass().getName().equals("org.slf4j.impl.JDK14LoggerAdapter")) {
            LogManager manager = LogManager.getLogManager();
            for(Enumeration<String> e = req.getParameterNames(); e.hasMoreElements(); ) {
                String param = e.nextElement();
                Logger logger = manager.getLogger(param);
                if(!req.getParameter(param).trim().equals("")) {
                    try {
                        logger.setLevel(Level.parse(req.getParameter(param)));
                    } catch(IllegalArgumentException ex) {
                        // ignore
                    }
                }
            }
        }
        doGet(req, res);
    }

    public void destroy() {
    }

    private class LoggerHierarchy {
        String name;
        Logger logger;
        Map<String, LoggerHierarchy> hierarchy;
        LoggerHierarchy() {
            name = "";
            logger = null;
            hierarchy = new TreeMap<String,LoggerHierarchy>();
        }
        LoggerHierarchy(Logger logger) {
            this.name = logger.getName();
            this.logger = logger;
            hierarchy = new TreeMap<String,LoggerHierarchy>();
        }

        LoggerHierarchy add(Logger logger) {
            if(logger == this.logger) {
                return this;
            }
            LoggerHierarchy loggerHierarchy;
            Logger parent = logger.getParent();
            if(parent != null) {
                LoggerHierarchy parentHierarchy = add(parent);
                if(!parentHierarchy.hierarchy.containsKey(logger.getName())) {
                    loggerHierarchy = new LoggerHierarchy(logger);
                    parentHierarchy.hierarchy.put(logger.getName(), loggerHierarchy);
                } else
                    loggerHierarchy = parentHierarchy.hierarchy.get(logger.getName());
            } else {
                if(!hierarchy.containsKey(logger.getName())) {
                    loggerHierarchy = new LoggerHierarchy(logger);
                    hierarchy.put(logger.getName(), loggerHierarchy);
                } else
                    loggerHierarchy = hierarchy.get(logger.getName());
            }
            return loggerHierarchy;
         }

        String print(PrintWriter writer, String contextLogger, String first) {
            String last = first;
            if(logger != null) {
                last = name;
                writer.print("  <tr><td>");
                writer.print("<tt>");
                if(contextLogger == null || contextLogger.equals("")) {
                    int skipCount = 0;
                    for(int i=0; first.length() > i && name.length() > i; i++)
                        if(first.charAt(i) != name.charAt(i))
                            break;
                        else if(first.charAt(i) == '.')
                            skipCount = i;
                    for(int i=0; i<skipCount; i++)
                        writer.print("&nbsp;");
                    writer.print(name.substring(skipCount));
                } else {
                    for(int i=0; i<contextLogger.length(); i++)
                        writer.print("&nbsp;");
                    writer.print(name.substring(contextLogger.length()));
                }
                writer.print("</tt>");

                writer.print("</td><td>");
                Level level = logger.getLevel();
                if(level != null) {
                    writer.print(level.getName());
                } else {
                    writer.print("<em>unset</em>");
                }
                writer.print("</td>");
                writer.print("<td><select name=\""+logger.getName()+"\"><option label=\"\" value=\"\"></option>");
                for(int i=0; i<levels.length; i++) {
                    writer.print("<option label=\""+levels[i].getName()+"\" value=\""+levels[i].getName()+"\"");
                    if(levels[i].equals(logger.getLevel()))
                        writer.print(" selected");
                    writer.print(">"+levels[i].getName()+"</option>");
                }
                writer.print("</select></td>");
                writer.println("</td></tr>");
            }
            for(LoggerHierarchy child : hierarchy.values()) {
                last = child.print(writer, name, last);
            }
            return last;
        }
    }
}
