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
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class LoggingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(LoggingServlet.class);

    private static final String REPOS_PARAM = "repository-address";
    private static final String NODE_PARAM = "logging-check-node";
    private static final String PRIV_PARAM = "logging-check-privilege";

    private static final String DEFAULT_REPOS = "vm://";
    private static final String DEFAULT_NODE = "////content/documents";
    private static final String DEFAULT_PRIV = "hippo:admin";

    private static final String LOG_LEVEL_PARAM_NAME = "ll";

    private static final boolean isLog4jLog = "org.slf4j.impl.Log4jLoggerAdapter".equals(log.getClass().getName());
    private static final boolean isJDK14Log = "org.slf4j.impl.JDK14LoggerAdapter".equals(log.getClass().getName());

    private transient HippoRepository repository;
    private transient Session session;

    private String repositoryLocation;
    private String privilege;
    private String absPath;

    private static final String[] log4jLevels = new String[] { "OFF", "SEVERE", "ERROR", "WARN", "INFO", "DEBUG",
            "TRACE", "ALL" };

    private static final String[] jdk14Levels = new String[] { "OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE",
            "FINER", "FINEST", "ALL" };

    private Configuration freeMarkerConfiguration;

    public void init(ServletConfig config) throws ServletException {
        repositoryLocation = getInitParameter(config, REPOS_PARAM, DEFAULT_REPOS);
        privilege = getInitParameter(config, PRIV_PARAM, DEFAULT_PRIV);
        absPath = ensureStartSlash(getInitParameter(config, NODE_PARAM, DEFAULT_NODE));
        log.info("LoggingServlet configured with repository '" + repositoryLocation + "' check node '" + absPath
                + "' and check privilege '" + privilege + "'");

        freeMarkerConfiguration = createFreemarkerConfiguration();

        super.init(config);
    }

    /**
     * Create Freemarker template engine configuration on initiaization
     * <P>
     * By default, this method created a configuration by using {@link DefaultObjectWrapper}
     * and {@link ClassTemplateLoader}. And sets additional properties by loading
     * <code>./LoggingServlet-ftl.properties</code> from the classpath.
     * </P>
     * @return
     */
    protected Configuration createFreemarkerConfiguration() {
        Configuration cfg = new Configuration();

        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), ""));

        InputStream propsInput = null;
        final String propsResName = getClass().getSimpleName() + "-ftl.properties";

        try {
            propsInput = getClass().getResourceAsStream(propsResName);
            cfg.setSettings(propsInput);
        } catch (Exception e) {
            log.warn("Failed to load Freemarker configuration properties.", e);
        } finally {
            IOUtils.closeQuietly(propsInput);
        }

        return cfg;
    }

    /**
     * Create Freemarker template to render result.
     * By default, this loads <code>LoggingServlet-html.ftl</code> from the classpath.
     * @param request
     * @return
     * @throws IOException
     */
    protected Template getRenderTemplate(final HttpServletRequest request) throws IOException {
        final String templateName = getClass().getSimpleName() + "-html.ftl";
        return freeMarkerConfiguration.getTemplate(templateName);
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
        res.setCharacterEncoding("UTF-8");

        if (!isAuthorized(req)) {
            BasicAuth.setRequestAuthorizationHeaders(res, "LoggingServlet");
            return;
        }

        String requestURI = req.getRequestURI();

        if (!requestURI.endsWith("/")) {
            res.sendRedirect(requestURI + "/");
            return;
        }

        Map<String, Object> templateParams = new HashMap<String, Object>();

        try {
            if (isJDK14Log) {
                templateParams.put("logLevels", Arrays.asList(jdk14Levels));
            } else if (isLog4jLog) {
                templateParams.put("logLevels", Arrays.asList(log4jLevels));
            }

            templateParams.put("loggerInUse", log);
            Map<String, LoggerLevelInfo> loggerLevelInfosMap = getLoggerLevelInfosMap();
            templateParams.put("loggerLevelInfosMap", loggerLevelInfosMap);
        } catch (Exception ex) {
            templateParams.put("exception", ex);
        } finally {
            try {
                renderTemplatePage(req, res, getRenderTemplate(req), templateParams);
            } catch (Exception te) {
                log.warn("Failed to render freemarker template.", te);
            }
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!isAuthorized(req)) {
            BasicAuth.setRequestAuthorizationHeaders(res, "LoggingServlet");
            return;
        }

        String [] logLevelParams = req.getParameterValues(LOG_LEVEL_PARAM_NAME);

        if (logLevelParams != null) {
            Map<String, LoggerLevelInfo> loggerLevelInfosMap = getLoggerLevelInfosMap();

            String loggerName = null;
            String loggerLevel = null;

            for (String logLevelParam : logLevelParams) {
                loggerName = StringUtils.substringBefore(logLevelParam, ":");
                loggerLevel = StringUtils.substringAfter(logLevelParam, ":");

                if (StringUtils.isNotEmpty(loggerName) && StringUtils.isNotEmpty(loggerLevel)) {
                    LoggerLevelInfo loggerLevelInfo = loggerLevelInfosMap.get(loggerName);

                    if (loggerLevelInfo != null && !StringUtils.equals(loggerLevel, loggerLevelInfo.getEffectiveLogLevel())) {
                        setLoggerLevel(loggerName, loggerLevel);
                    }
                }
            }
        }

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

    public void destroy() {
    }

    /**
     * Get a sorted map with sets "logger name" => <code>LoggerLevelInfo</code>
     * @return SortedMap<String, LoggerLevelInfo>
     */
    private SortedMap<String, LoggerLevelInfo> getLoggerLevelInfosMap() {
        SortedMap<String, LoggerLevelInfo> loggerLevelInfosMap = new TreeMap<String, LoggerLevelInfo>();

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

                if (level != null) {
                    loggerLevelInfosMap.put(loggerName, new LoggerLevelInfo(loggerName, level.toString()));
                } else {
                    loggerLevelInfosMap.put(loggerName, new LoggerLevelInfo(loggerName, null, effectiveLevel.toString()));
                }
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

                        if (level != null) {
                            loggerLevelInfosMap.put(loggerName, new LoggerLevelInfo(loggerName, level.toString()));
                        } else {
                            loggerLevelInfosMap.put(loggerName, new LoggerLevelInfo(loggerName, null, effectiveLevel.toString()));
                        }
                    } catch (Exception e) {
                        log.error("Error getting logger name and level : " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error getting log4j through reflection: " + e.getMessage(), e);
            }
        }

        return loggerLevelInfosMap;
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

    private void renderTemplatePage(final HttpServletRequest request, final HttpServletResponse response,
            Template template, final Map<String, Object> templateParams) throws IOException, ServletException, TemplateException {
        PrintWriter out = null;

        try {
            out = response.getWriter();
            Map<String, Object> context = new HashMap<String, Object>();

            if (templateParams != null && !templateParams.isEmpty()) {
                for (Map.Entry<String, Object> entry : templateParams.entrySet()) {
                    context.put(entry.getKey(), entry.getValue());
                }
            }

            context.put("request", request);
            context.put("response", response);

            template.process(context, out);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static class LoggerLevelInfo {

        private String loggerName;
        private String logLevel;
        private String effectiveLogLevel;

        public LoggerLevelInfo(String loggerName, String logLevel) {
            this(loggerName, logLevel, logLevel);
        }

        public LoggerLevelInfo(String loggerName, String logLevel, String effectiveLogLevel) {
            this.loggerName = loggerName;
            this.logLevel = logLevel;
            this.effectiveLogLevel = effectiveLogLevel;
        }

        public String getLoggerName() {
            return loggerName;
        }

        public void setLoggerName(String loggerName) {
            this.loggerName = loggerName;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public String getEffectiveLogLevel() {
            return effectiveLogLevel;
        }

        public void setEffectiveLogLevel(String effectiveLogLevel) {
            this.effectiveLogLevel = effectiveLogLevel;
        }
    }
}
