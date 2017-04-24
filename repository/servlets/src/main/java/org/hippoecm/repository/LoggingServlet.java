/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

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

    protected static final Logger log = LoggerFactory.getLogger(LoggingServlet.class);

    private static final String REPOS_PARAM = "repository-address";
    private static final String NODE_PARAM = "logging-check-node";
    private static final String PRIV_PARAM = "logging-check-privilege";

    private static final String DEFAULT_REPOS = "vm://";
    private static final String DEFAULT_NODE = "////content/documents";
    private static final String DEFAULT_PRIV = "hippo:admin";

    private static final String LOG_LEVEL_PARAM_NAME = "ll";

    @Deprecated
    private static final boolean isLog4j1Log = "org.slf4j.impl.Log4jLoggerAdapter".equals(log.getClass().getName());
    private static final boolean isLog4j2Log = "org.apache.logging.slf4j.Log4jLogger".equals(log.getClass().getName());
    private static final boolean isJDK14Log = "org.slf4j.impl.JDK14LoggerAdapter".equals(log.getClass().getName());

    private transient LoggerLevelManager llManager;
    private transient List<String> logLevels;

    private transient HippoRepository repository;
    private transient Session session;

    private String repositoryLocation;
    private String privilege;
    private String absPath;

    private Configuration freeMarkerConfiguration;

    public void init(ServletConfig config) throws ServletException {
        repositoryLocation = getInitParameter(config, REPOS_PARAM, DEFAULT_REPOS);
        privilege = getInitParameter(config, PRIV_PARAM, DEFAULT_PRIV);
        absPath = ensureStartSlash(getInitParameter(config, NODE_PARAM, DEFAULT_NODE));
        log.info("LoggingServlet configured with repository '" + repositoryLocation + "' check node '" + absPath
                + "' and check privilege '" + privilege + "'");

        freeMarkerConfiguration = createFreemarkerConfiguration();

        super.init(config);

        if (isJDK14Log) {
            llManager = new JDK14LoggerLevelManager();
            logLevels = llManager.getLogLevels();
        } else if (isLog4j1Log) {
            try {
                Class llManagerClass = Class.forName("org.hippoecm.repository.Log4j1LoggerLevelManager");
                llManager = (LoggerLevelManager)llManagerClass.newInstance();
                logLevels = llManager.getLogLevels();
            } catch (Exception e) {
                log.error("Error getting Log4j1LoggerLevelManager through reflection: " + e.getMessage(), e);
            }
        } else if (isLog4j2Log) {
            try {
                Class llManagerClass = Class.forName("org.hippoecm.repository.Log4j2LoggerLevelManager");
                llManager = (LoggerLevelManager)llManagerClass.newInstance();
                logLevels = llManager.getLogLevels();
            } catch (Exception e) {
                log.error("Error getting Log4j2LoggerLevelManager through reflection: " + e.getMessage(), e);
            }
        } else {
            llManager = null;
            logLevels = Collections.emptyList();
            log.error("Unable to determine logger system. Logger class in use: " + log.getClass().getName());
        }
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
            templateParams.put("logLevels", logLevels);

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
        return llManager != null ? llManager.getLoggerLevelInfosMap() : Collections.emptySortedMap();
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
        if (llManager != null) {
            llManager.setLoggerLevel(name, level);
        }
    }

    private void renderTemplatePage(final HttpServletRequest request, final HttpServletResponse response,
            Template template, final Map<String, Object> templateParams) throws IOException, ServletException, TemplateException {
        PrintWriter out = null;

        try {
            out = response.getWriter();
            Map<String, Object> context = new HashMap<>();

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

    public interface LoggerLevelManager {
        List<String> getLogLevels();
        SortedMap<String, LoggerLevelInfo> getLoggerLevelInfosMap();
        void setLoggerLevel(String name, String level);
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
