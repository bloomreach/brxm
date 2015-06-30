/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.audit.AuditLogger;
import org.hippoecm.repository.decorating.server.ServerServicingAdapterFactory;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.GuavaHippoEventBus;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.RepositoryService;
import org.onehippo.repository.cluster.RepositoryClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class RepositoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    /** Parameter name of the repository storage directory */
    public static final String REPOSITORY_DIRECTORY_PARAM = "repository-directory";

    /** Parameter name of the binging address */
    public static final String REPOSITORY_BINDING_PARAM = "repository-address";

    /** Parameter name of the repository config file */
    public static final String REPOSITORY_CONFIG_PARAM = "repository-config";

    public static final String START_REMOTE_SERVER = "start-remote-server";

    /** Default repository storage directory */
    public static final String DEFAULT_REPOSITORY_DIRECTORY = "WEB-INF/storage";

    /** Default repository storage directory under the current working directory in case war is running while not
 * unpacked. */
    public static final String DEFAULT_REPOSITORY_DIRECTORY_UNDER_CURRENT_WORKING_DIR = "repository_storage";

    /** Default binding address for server */
    public static final String DEFAULT_REPOSITORY_BINDING = "rmi://localhost:1099/hipporepository";

    public static final String DEFAULT_START_REMOTE_SERVER = "false";

    /** Default config file */
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    /** System property for overriding the repository config file */
    public static final String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** RMI registry to which to bind the repository. */
    private Registry registry;
    private boolean registryIsEmbedded = false;

    private static Remote rmiRepository;

    private HippoRepository repository;
    private RepositoryService repositoryService;
    private RepositoryClusterService repositoryClusterService;
    private String bindingAddress;
    private String storageLocation;
    private String repositoryConfig;
    private boolean startRemoteServer;
    private AuditLogger listener;
    private GuavaHippoEventBus hippoEventBus;

    private Configuration freeMarkerConfiguration;

    public RepositoryServlet() {
        storageLocation = null;
    }

    private void parseInitParameters(ServletConfig config) throws ServletException {
        bindingAddress = getConfigurationParameter(REPOSITORY_BINDING_PARAM, DEFAULT_REPOSITORY_BINDING);
        repositoryConfig = getConfigurationParameter(REPOSITORY_CONFIG_PARAM, DEFAULT_REPOSITORY_CONFIG);
        storageLocation = getConfigurationParameter(REPOSITORY_DIRECTORY_PARAM, DEFAULT_REPOSITORY_DIRECTORY);
        startRemoteServer = Boolean.valueOf(getConfigurationParameter(START_REMOTE_SERVER, DEFAULT_START_REMOTE_SERVER));

        // check for absolute path
        if (!storageLocation.startsWith("/") && !storageLocation.startsWith("file:")) {
            // try to parse the relative path
            final String storagePath = config.getServletContext().getRealPath("/" + storageLocation);

            // ServletContext#getRealPath() may return null especially when unpackWARs="false".
            if (storagePath == null) {
                log.warn("Cannot determine the real path of the repository storage location, '{}'. Defaults to './{}'",
                        storageLocation, DEFAULT_REPOSITORY_DIRECTORY_UNDER_CURRENT_WORKING_DIR);
                storageLocation = DEFAULT_REPOSITORY_DIRECTORY_UNDER_CURRENT_WORKING_DIR;
            } else {
                storageLocation = storagePath;
            }
        }
    }

    public String getConfigurationParameter(String parameterName, String defaultValue) {
        String result = getInitParameter(parameterName);
        if (result == null || result.equals("")) {
            result = getServletContext().getInitParameter(parameterName);
        }
        if (result == null || result.equals("")) {
            result = defaultValue;
        }
        return result;
    }

    public String getRequestParameter(HttpServletRequest request, String parameterName, String defaultValue) {
        String result = request.getParameter(parameterName);
        if (result == null || result.equals("")) {
            result = getServletContext().getInitParameter(parameterName);
        }
        if (result == null || result.equals("")) {
            result = defaultValue;
        }
        return result;
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        hippoEventBus = new GuavaHippoEventBus();
        HippoServiceRegistry.registerService(hippoEventBus, HippoEventBus.class);

        listener = new AuditLogger();
        HippoServiceRegistry.registerService(listener, HippoEventBus.class);

        parseInitParameters(config);
        System.setProperty(SYSTEM_SERVLETCONFIG_PROPERTY, repositoryConfig);

        try {
            // get the local embedded repository
            repository = HippoRepositoryFactory.getHippoRepository(storageLocation);
            HippoRepositoryFactory.setDefaultRepository(repository);

            if (startRemoteServer) {
                // the the remote repository
                RepositoryUrl url = new RepositoryUrl(bindingAddress);
                rmiRepository = new ServerServicingAdapterFactory(url).getRemoteRepository(repository.getRepository());
                System.setProperty("java.rmi.server.useCodebaseOnly", "true");

                // Get or start registry and bind the remote repository
                try {
                    registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                    registry.rebind(url.getName(), rmiRepository); // connection exception happens here
                    log.info("Using existing rmi server on " + url.getHost() + ":" + url.getPort());
                } catch (ConnectException e) {
                    registry = LocateRegistry.createRegistry(url.getPort());
                    registry.rebind(url.getName(), rmiRepository);
                    log.info("Started an RMI registry on port " + url.getPort());
                    registryIsEmbedded = true;
                }
            }

            HippoServiceRegistry.registerService(repositoryService =
                    (RepositoryService) repository.getRepository(), RepositoryService.class);
            HippoServiceRegistry.registerService(repositoryClusterService = new RepositoryClusterService() {
                @Override
                public boolean isExternalEvent(final Event event) {
                    if (!(event instanceof JackrabbitEvent)) {
                        throw new IllegalArgumentException("Event is not an instance of JackrabbitEvent");
                    }
                    return ((JackrabbitEvent) event).isExternal();
                }
            }, RepositoryClusterService.class);
        } catch (MalformedURLException ex) {
            log.error("MalformedURLException exception: " + bindingAddress, ex);
            throw new ServletException("RemoteException: " + ex.getMessage());
        } catch (RemoteException ex) {
            log.error("Generic remoting exception: " + bindingAddress, ex);
            throw new ServletException("RemoteException: " + ex.getMessage());
        } catch (RepositoryException ex) {
            log.error("Error while setting up JCR repository: ", ex);
            throw new ServletException("RepositoryException: " + ex.getMessage());
        }

        freeMarkerConfiguration = createFreemarkerConfiguration();
    }



    @Override
    public void destroy() {
        // close repository
        log.info("Closing repository.");
        if (repository != null) {
            repository.close();
            repository = null;
        }

        // done
        log.info("Repository closed.");

        if (startRemoteServer) {
            // unbinding from registry
            String name = null;
            try {
                name = new RepositoryUrl(bindingAddress).getName();
                log.info("Unbinding '"+name+"' from registry.");
                registry.unbind(name);
            } catch (RemoteException | NotBoundException e) {
                log.error("Error during unbinding '" + name + "': " + e.getMessage());
            } catch (MalformedURLException e) {
                log.error("MalformedURLException while parsing '" + bindingAddress + "': " + e.getMessage());
            }

            // unexporting from registry
            try {
                log.info("Unexporting rmi repository: " + bindingAddress);
                UnicastRemoteObject.unexportObject(rmiRepository, true);
            } catch (NoSuchObjectException e) {
                log.error("Error during rmi shutdown for address: " + bindingAddress, e);
            }

            // shutdown registry
            if (registryIsEmbedded) {
                try {
                    log.info("Closing rmiregistry: " + bindingAddress);
                    UnicastRemoteObject.unexportObject(registry, true);
                } catch (NoSuchObjectException e) {
                    log.error("Error during rmi shutdown for address: " + bindingAddress, e);
                }
            }

            // force the distributed GC to fire, otherwise in tomcat with embedded
            // rmi registry the process won't end
            // this procedure is necessary specifically for Tomcat
            log.info("Repository terminated, waiting for garbage to clear");
            Thread garbageClearThread = new Thread("garbage clearer") {
                public void run() {
                    for(int i=0; i < 5; i++) {
                        try {
                            Thread.sleep(3000);
                            System.gc();
                        } catch(InterruptedException ignored) {
                        }
                    }
                }
            };
            garbageClearThread.setDaemon(true);
            garbageClearThread.start();

        }

        if (repositoryService != null) {
            HippoServiceRegistry.unregisterService(repositoryService, RepositoryService.class);
        }
        if (repositoryClusterService != null) {
            HippoServiceRegistry.unregisterService(repositoryClusterService, RepositoryClusterService.class);
        }
        HippoServiceRegistry.unregisterService(listener, HippoEventBus.class);
        HippoServiceRegistry.unregisterService(hippoEventBus, HippoEventBus.class);
        hippoEventBus.destroy();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String currentNodePath = req.getRequestURI();

        if (!currentNodePath.endsWith("/")) {
            res.sendRedirect(currentNodePath + "/");
            return;
        }

        // explicitly set character encoding
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        if(req.getParameter("logout") != null){
            FormAuth.logout(req);
            // redirect to current URL without ?logout
            res.sendRedirect("./");
            return;
        }

        Session jcrSession = FormAuth.authorize(req, repository);
        if(jcrSession == null) {
            FormAuth.showLoginPage(req, res, "");
            return;
        }

        if (currentNodePath.startsWith(req.getContextPath())) {
            currentNodePath = currentNodePath.substring(req.getContextPath().length());
        }

        if (currentNodePath.startsWith(req.getServletPath())) {
            currentNodePath = currentNodePath.substring(req.getServletPath().length());
        }

        Map<String, Object> templateParams = new HashMap<String, Object>();

        try {

            templateParams.put("jcrSession", jcrSession);

            // parse path
            while (currentNodePath.startsWith("/")) {
                currentNodePath = currentNodePath.substring(1);
            }

            templateParams.put("rootRelativePath", StringUtils.isEmpty(currentNodePath) ? "" :
                    StringUtils.join(new String[currentNodePath.split("/").length + 1], "../"));

            currentNodePath = URLDecoder.decode(currentNodePath, "UTF-8");
            Node rootNode = jcrSession.getRootNode();
            Node currentNode = rootNode;
            if (!"".equals(currentNodePath)) {
                currentNode = currentNode.getNode(currentNodePath);
            }

            templateParams.put("rootNode", rootNode);
            templateParams.put("currentNode", currentNode);

            if (currentNode.isSame(rootNode)) {
                templateParams.put("ancestorNodes", Collections.emptyList());
            } else {
                List<Node> ancestorNodes = new LinkedList<Node>();
                for (Node ancestor = currentNode.getParent(); ancestor != null && !ancestor.isSame(rootNode); ancestor = ancestor.getParent()) {
                    ancestorNodes.add(0, ancestor);
                }
                templateParams.put("ancestorNodes", ancestorNodes);
            }


            String searchType = req.getParameter("search-type");


            if (searchType != null && !"uuid".equals(searchType)) {

                String limit = "1000";
                boolean isXPathQuery = false;
                String queryString = "";

                if ("text".equals(searchType)) {
                    limit = req.getParameter("text-limit");
                    isXPathQuery = true;
                    queryString = req.getParameter("text");
                    templateParams.put("originalQuery", queryString);
                    if(StringUtils.isNotBlank(queryString)) {
                        queryString = "//*[jcr:contains(* , '" + queryString + "') ]";
                    }
                    queryString = addOrderbyClause(queryString, true);

                } else if ("xpath".equals(searchType)) {
                    limit = req.getParameter("xpath-limit");
                    isXPathQuery = true;
                    queryString = req.getParameter("xpath").trim();
                    templateParams.put("originalQuery", queryString);
                    queryString = addOrderbyClause(queryString, true);

                } else if ("sql".equals(searchType)) {
                    limit = req.getParameter("sql-limit");
                    isXPathQuery = false;
                    queryString = req.getParameter("sql").trim();
                    templateParams.put("originalQuery", queryString);
                    queryString = addOrderbyClause(queryString, false);
                }

                QueryManager qmgr = jcrSession.getWorkspace().getQueryManager();
                String language = (isXPathQuery ? Query.XPATH: Query.SQL);
                Query query;

                if (isXPathQuery) {
                    // we encode xpath queries to support queries like /jcr:root/7_8//*
                    // the 7 needs to be encode
                    query = qmgr.createQuery(RepoUtils.encodeXpath(queryString), language);
                } else {
                    query = qmgr.createQuery(queryString, language);
                }

                if (limit != null && !limit.isEmpty()) {
                    query.setLimit(Long.parseLong(limit));
                }

                QueryResult queryResult = query.execute();
                templateParams.put("queryResult", queryResult);
                templateParams.put("queryResultTotalSize", ((HippoNodeIterator) queryResult.getNodes()).getTotalSize());
            }




            if (req.getParameter("map") != null) {
                Map repositoryMap = repository.getRepositoryMap(currentNode);
                String param = req.getParameter("map");
                if (!"".equals(param)) {
                    StringTokenizer st = new StringTokenizer(param, ".");
                    while (st.hasMoreTokens()) {
                        repositoryMap = (Map) repositoryMap.get(st.nextToken());
                    }
                }
                templateParams.put("repositoryMap", repositoryMap);
            }

            if ("uuid".equals(searchType)) {
                Node nodeById = jcrSession.getNodeByIdentifier(req.getParameter("uuid"));
                templateParams.put("nodeById", nodeById);
            } else if (req.getParameter("deref") != null) {
                Node nodeById = jcrSession.getNodeByIdentifier(req.getParameter("deref"));
                templateParams.put("nodeById", nodeById);
            }

        } catch (LoginException ex) {
            BasicAuth.setRequestAuthorizationHeaders(res, "Repository");
        } catch (Exception ex) {
            templateParams.put("exception", ex);
        } finally {
            try {
                if (jcrSession != null) {
                    renderTemplatePage(req, res, getRenderTemplate(req), templateParams);
                }
            } catch (Exception te) {
                log.warn("Failed to render freemarker template.", te);
            } finally {
                if (jcrSession != null) {
                    jcrSession.logout();
                }
            }
        }
    }

    /**
     * Create Freemarker template engine configuration on initiaization
     * <P>
     * By default, this method created a configuration by using {@link DefaultObjectWrapper}
     * and {@link ClassTemplateLoader}. And sets additional properties by loading
     * <code>./RepositoryServlet-ftl.properties</code> from the classpath.
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
     * By default, this loads <code>RepositoryServlet-html.ftl</code> from the classpath.
     * @param request meant for possibly extensibility for subclasses requiring to load a different template than the
     *                default embedded
     * @return
     * @throws IOException
     */
    protected Template getRenderTemplate(@SuppressWarnings("UnusedParameters") final HttpServletRequest request) throws IOException {
        final String templateName = getClass().getSimpleName() + "-html.ftl";
        return freeMarkerConfiguration.getTemplate(templateName);
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

    private String addOrderbyClause(String queryString, boolean isXPath) {
        if(!queryString.toLowerCase().contains("order by")) {
            return queryString + " order by " + (isXPath ? "@" : "") + "jcr:score";
        }
        return queryString;
    }

}
