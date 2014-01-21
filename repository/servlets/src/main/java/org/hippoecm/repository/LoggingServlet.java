/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(LoggingServlet.class);

    private static final String REPOS_PARAM = "repository-address";
    private static final String NODE_PARAM = "logging-check-node";
    private static final String PRIV_PARAM = "logging-check-privilege";

    private static final String DEFAULT_REPOS = "vm://";
    private static final String DEFAULT_NODE = "////content/documents";
    private static final String DEFAULT_PRIV = "hippo:admin";

    private static final boolean isLog4jLog = "org.slf4j.impl.Log4jLoggerAdapter".equals(log.getClass().getName());
    private static final boolean isJDK14Log = "org.slf4j.impl.JDK14LoggerAdapter".equals(log.getClass().getName());

    private transient HippoRepository repository;
    private transient Session session;

    private String repositoryLocation;
    private String privilege;
    private String absPath;

    private static final Level[] levels = new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO,
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL };

    private static final String[] log4jLevels = new String[] { "OFF", "SEVERE", "ERROR", "WARN", "INFO", "DEBUG",
            "TRACE", "ALL" };

    private static final String[] jdk14Levels = new String[] { "OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE",
            "FINER", "FINEST", "ALL" };

    public void init(ServletConfig config) throws ServletException {
        repositoryLocation = getInitParameter(config, REPOS_PARAM, DEFAULT_REPOS);
        privilege = getInitParameter(config, PRIV_PARAM, DEFAULT_PRIV);
        absPath = ensureStartSlash(getInitParameter(config, NODE_PARAM, DEFAULT_NODE));
        log.info("LoggingServlet configured with repository '" + repositoryLocation + "' check node '" + absPath
                + "' and check privilege '" + privilege + "'");
        super.init(config);
    }

    private String getInitParameter(ServletConfig config, String paramName, String defaultValue) {
        String initValue = config.getInitParameter(paramName);
        if (initValue != null && initValue.length() != 0) {
            return initValue;
        } else {
            return defaultValue;
        }
    }

    private String ensureStartSlash(String path) {
        int c = 0;
        while (path.charAt(c) == '/') {
            c++;
        }
        return "/" + path.substring(c);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // explicitly set character encoding
        req.setCharacterEncoding("UTF-8");
        res.setContentType("text/html;charset=UTF-8");

        if (!isAuthorized(req)) {
            BasicAuth.setRequestAuthorizationHeaders(res, "LoggingServlet");
            return;
        }
        
        String path = req.getRequestURI();
        if (!path.endsWith("/")) res.sendRedirect(path + "/");

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
        writer
                .println("    <table style=\"params\" summary=\"request parameters\"><tr><th>name</th><th>value</th></tr>");
        writer.println("    <tr><td>context path</td><td>: <code>" + req.getContextPath() + "</code></td></tr>");
        writer.println("    <tr><td>servlet path</td><td>: <code>" + req.getServletPath() + "</code></td></tr>");
        writer.println("    <tr><td>request uri</td><td>: <code>" + req.getRequestURI() + "</code></td></tr>");
        writer.println("    </table>");
        writer.println("  <h3>Logging</h3>");

        writer.println("<p>Logger in use: " + log.getClass().getName() + "</p><p>\n");

        String lastLogger = req.getParameter("logger");
        String lastLevel = req.getParameter("level");
        SortedMap<String, String> loggerLevelMap = getLoggerLevelMap();
        printLoggerLevels(writer, loggerLevelMap);

        writer.println("</p>");
        writer.println("</body></html>");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!isAuthorized(req)) {
            BasicAuth.setRequestAuthorizationHeaders(res, "LoggingServlet");
            return;
        }
        String loggerName = req.getParameter("logger");
        String loggerLevel = req.getParameter("level");
        setLoggerLevel(loggerName, loggerLevel);
        doGet(req, res);
    }

    private boolean isAuthorized(HttpServletRequest req) {
        if (!BasicAuth.hasAuthorizationHeader(req)) {
            return false;
        }
        SimpleCredentials creds = BasicAuth.parseAuthorizationHeader(req);
        try {
            return hasRepositoryPrivs(creds);
        } catch (LoginException e) {
            log.warn("Invalid login from: " + req.getRemoteAddr() + " for " + req.getRequestURI());
        } catch (AccessControlException e) {
            log.warn("Unauthorized attempt from: " + req.getRemoteAddr() + " for " + req.getRequestURI());
        } catch (RepositoryException e) {
            log.warn("Error while checking privileges: " + e.getMessage());
            log.debug("Error:", e);
        }
        return false;
    }

    private synchronized boolean hasRepositoryPrivs(SimpleCredentials creds) throws RepositoryException {
        try {
            obtainRepository();
            obtainSession(creds);
            return hasPrivs();
        } finally {
            closeSession();
        }
    }

    private void obtainRepository() throws RepositoryException {
        repository = HippoRepositoryFactory.getHippoRepository(repositoryLocation);
    }

    private void obtainSession(SimpleCredentials creds) throws RepositoryException {
        session = repository.login(creds);
    }

    private boolean hasPrivs() throws RepositoryException {
        if (absPath.length() == 0) {
            session.checkPermission("/", privilege);
        } else {
            session.checkPermission(absPath, privilege);
        }
        return true;
    }

    private void closeSession() {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    private void printLoggerLevels(PrintWriter writer, Map<String, String> loggerLevelMap) {
        writer.println("    <table>");
        writer.println("      <tr><th>logger</th><th>level</th><th>change</th></tr>");
        for (Map.Entry<String, String> logMap : loggerLevelMap.entrySet()) {
            writer.print("      <tr><td><tt>");
            String escapedName = StringEscapeUtils.escapeHtml(logMap.getKey());
            writer.print("<a id=\""+escapedName+"\">");
            writer.print(escapedName);
            writer.print("</a>");
            writer.print("</tt></td><td>");
            writer.print(logMap.getValue());
            writer.print("</td><td>");
            printChangeLevelForm(writer, logMap.getKey());
            writer.println("</td></tr>");
        }
        writer.println("    </table>");
    }

    private void printChangeLevelForm(final PrintWriter writer, final String loggerName) {
        String escapedName = StringEscapeUtils.escapeHtml(loggerName);
        writer.println("<form action=\"#"+escapedName+"\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\">");
        writer.println("<input type=\"hidden\" name=\"logger\" value=\""+escapedName+"\">");
        writer.println("level: <select name=\"level\">");
        if (isJDK14Log) {
            for (int i = 0; i < jdk14Levels.length; i++) {
                writer.println("<option label=\"" + jdk14Levels[i] + "\" value=\"" + jdk14Levels[i] + "\">"
                        + jdk14Levels[i] + "</option>");
            }
        } else if (isLog4jLog) {
            for (int i = 0; i < log4jLevels.length; i++) {
                writer.println("<option label=\"" + log4jLevels[i] + "\" value=\"" + log4jLevels[i] + "\">"
                        + log4jLevels[i] + "</option>");
            }
        }
        writer.println("</select>");
        writer.println("<input type=\"submit\" name=\"submit\" value=\"Apply\">");
        writer.println("  </form>");
    }

    public void destroy() {
    }

    private class LoggerHierarchy {
        String name;
        java.util.logging.Logger logger;
        Map<String, LoggerHierarchy> hierarchy;

        LoggerHierarchy() {
            name = "";
            logger = null;
            hierarchy = new TreeMap<String, LoggerHierarchy>();
        }

        LoggerHierarchy(java.util.logging.Logger logger) {
            this.name = logger.getName();
            this.logger = logger;
            hierarchy = new TreeMap<String, LoggerHierarchy>();
        }

        LoggerHierarchy add(java.util.logging.Logger logger) {
            if (logger == this.logger) {
                return this;
            }
            LoggerHierarchy loggerHierarchy;
            java.util.logging.Logger parent = logger.getParent();
            if (parent != null) {
                LoggerHierarchy parentHierarchy = add(parent);
                if (!parentHierarchy.hierarchy.containsKey(logger.getName())) {
                    loggerHierarchy = new LoggerHierarchy(logger);
                    parentHierarchy.hierarchy.put(logger.getName(), loggerHierarchy);
                } else
                    loggerHierarchy = parentHierarchy.hierarchy.get(logger.getName());
            } else {
                if (!hierarchy.containsKey(logger.getName())) {
                    loggerHierarchy = new LoggerHierarchy(logger);
                    hierarchy.put(logger.getName(), loggerHierarchy);
                } else
                    loggerHierarchy = hierarchy.get(logger.getName());
            }
            return loggerHierarchy;
        }

        String print(PrintWriter writer, String contextLogger, String first) {
            String last = first;
            if (logger != null) {
                last = name;
                writer.print("  <tr><td>");
                writer.print("<tt>");
                if (contextLogger == null || contextLogger.equals("")) {
                    int skipCount = 0;
                    for (int i = 0; first.length() > i && name.length() > i; i++)
                        if (first.charAt(i) != name.charAt(i))
                            break;
                        else if (first.charAt(i) == '.')
                            skipCount = i;
                    for (int i = 0; i < skipCount; i++)
                        writer.print("&nbsp;");
                    writer.print(name.substring(skipCount));
                } else {
                    for (int i = 0; i < contextLogger.length(); i++)
                        writer.print("&nbsp;");
                    writer.print(name.substring(contextLogger.length()));
                }
                writer.print("</tt>");

                writer.print("</td><td>");
                Level level = logger.getLevel();
                if (level != null) {
                    writer.print(level.getName());
                } else {
                    writer.print("<em>unset</em>");
                }
                writer.print("</td>");
                writer.print("<td><select name=\"" + logger.getName() + "\"><option label=\"\" value=\"\"></option>");
                for (int i = 0; i < levels.length; i++) {
                    writer.print("<option label=\"" + levels[i].getName() + "\" value=\"" + levels[i].getName() + "\"");
                    if (levels[i].equals(logger.getLevel()))
                        writer.print(" selected");
                    writer.print(">" + levels[i].getName() + "</option>");
                }
                writer.print("</select></td>");
                writer.println("</td></tr>");
            }
            for (LoggerHierarchy child : hierarchy.values()) {
                last = child.print(writer, name, last);
            }
            return last;
        }
    }

    /**
     * Get a sorted map with sets "logger name" => "logger level"
     * @return SortedMap<String, String>
     */
    private SortedMap<String, String> getLoggerLevelMap() {
        SortedMap<String, String> logLevelMap = new TreeMap<String, String>();

        if (isJDK14Log) {
            java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
            for (Enumeration<String> namesIter = manager.getLoggerNames(); namesIter.hasMoreElements();) {
                String loggerName = namesIter.nextElement();
                java.util.logging.Logger logger = manager.getLogger(loggerName);
                java.util.logging.Level level = logger.getLevel();
                java.util.logging.Level effectiveLevel = level;

                // try to find effective level
                if (level == null) {
                    for (java.util.logging.Logger l = logger; l != null; l = l.getParent()) {
                        if (l.getLevel() != null) {
                            effectiveLevel = l.getLevel();
                            break;
                        }
                    }
                }
                String loggerLevel = (level == null) ? effectiveLevel.toString() + " (unset)" : level.toString();
                logLevelMap.put(loggerName, loggerLevel);
            }
        } else if (isLog4jLog) {
            try {
                // Log4j Classes
                Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
                Class<?> managerClass = Class.forName("org.apache.log4j.LogManager");

                // Log4j Methods
                Method getName = loggerClass.getMethod("getName", null);
                Method getLevel = loggerClass.getMethod("getLevel", null);
                Method getEffectiveLevel = loggerClass.getMethod("getEffectiveLevel", null);
                Method getLoggers = managerClass.getMethod("getCurrentLoggers", null);

                // get and sort loggers and log levels
                Enumeration loggers = (Enumeration) getLoggers.invoke(null, null);
                while (loggers.hasMoreElements()) {
                    try {
                        Object logger = loggers.nextElement();
                        String loggerName = (String) getName.invoke(logger, null);
                        Object level = getLevel.invoke(logger, null);
                        Object effectiveLevel = getEffectiveLevel.invoke(logger, null);
                        String loggerLevel = (level == null) ? effectiveLevel.toString() + " (unset)" : level
                                .toString();
                        logLevelMap.put(loggerName, loggerLevel);
                    } catch (Exception e) {
                        log.error("Error getting logger name and level : " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error getting log4j through reflection: " + e.getMessage(), e);
            }
        }
        return logLevelMap;
    }

    /**
     * Set the logger's log level through reflection
     * @param name String the name of the logger to change
     * @param level String the new log level
     */
    private void setLoggerLevel(String name, String level) {
        if (name == null || name.length() == 0) {
            log.warn("Invalid empty name. Not settting log level");
            return;
        }
        if (isJDK14Log) {
            java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
            java.util.logging.Logger logger = logManager.getLogger(name);
            if (logger != null) {
                logger.setLevel(Level.parse(level));
            } else {
                log.warn("Logger not found : " + name);
            }
        } else if (isLog4jLog) {
            try {
                log.warn("Setting logger " + name + " to level " + level);

                // basic log4j reflection
                Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
                Class<?> levelClass = Class.forName("org.apache.log4j.Level");
                Class<?> logManagerClass = Class.forName("org.apache.log4j.LogManager");
                Method setLevel = loggerClass.getMethod("setLevel", levelClass);

                // get the logger
                Object logger = logManagerClass.getMethod("getLogger", String.class).invoke(null, name);

                // get the static level object field, e.g. Level.INFO
                Field levelField;
                levelField = levelClass.getField(level);
                Object levelObj = levelField.get(null);

                // set the level
                setLevel.invoke(logger, levelObj);
            } catch (NoSuchFieldException e) {
                log.warn("Unable to find Level." + level + " , not adjusting logger " + name);
            } catch (Exception e) {
                log.error("Unable to set logger " + name + " + to level " + level, e);
            }
        } else {
            log.warn("Unable to determine logger");
        }
    }
}
