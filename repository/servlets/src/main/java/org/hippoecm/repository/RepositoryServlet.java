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
import java.lang.annotation.Annotation;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.audit.AuditLogger;
import org.hippoecm.repository.decorating.server.ServerServicingAdapterFactory;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.GuavaHippoEventBus;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.RepositoryService;
import org.onehippo.repository.cluster.RepositoryClusterService;
import org.onehippo.repository.events.Persisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            storageLocation = config.getServletContext().getRealPath(storageLocation);

            // ServletContext#getRealPath() may return null especially when unpackWARs="false".
            if (storageLocation == null) {
                log.warn("Cannot determine the real path of the repository location, '{}'. Defaults to './{}'",
                        config.getInitParameter(REPOSITORY_DIRECTORY_PARAM),
DEFAULT_REPOSITORY_DIRECTORY_UNDER_CURRENT_WORKING_DIR);
                storageLocation = DEFAULT_REPOSITORY_DIRECTORY_UNDER_CURRENT_WORKING_DIR;
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

        hippoEventBus = new GuavaHippoEventBus() {
            @Override
            protected boolean acceptMethod(final Object listener, final Annotation[] annotations, final Class<?> parameterType) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType() == Persisted.class) {
                        return false;
                    }
                }
                return true;
            }
        };
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
            } catch (RemoteException e) {
                log.error("Error during unbinding '" + name + "': " + e.getMessage());
            } catch (NotBoundException e) {
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
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // explicitly set character encoding

        req.setCharacterEncoding("UTF-8");
        res.setContentType("text/html;charset=UTF-8");

        if (!BasicAuth.hasAuthorizationHeader(req)) {
            BasicAuth.setRequestAuthorizationHeaders(res, "Repository");
            return;
        }

        SimpleCredentials creds = BasicAuth.parseAuthorizationHeader(req);

        String path = req.getRequestURI();
        if (!path.endsWith("/")) {
            res.sendRedirect(path + "/");
            return;
        }
        if (path.startsWith(req.getContextPath())) {
            path = path.substring(req.getContextPath().length());
        }
        if (path.startsWith(req.getServletPath())) {
            path = path.substring(req.getServletPath().length());
        }
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();

        Session session = null;
        try {
            if (creds.getUserID() == null || creds.getUserID().length() == 0) {
                session = repository.login();
            } else {
                session = repository.login(creds);
            }

            if (((HippoSession)session).getUser().isSystemUser()) {
                final InetAddress address = InetAddress.getByName(req.getRemoteHost());
                if (!address.isAnyLocalAddress() && !address.isLoopbackAddress()) {
                    throw new LoginException();
                }
            }

            writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
            writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            writer.println("<head>");
            writer.println("  <title>Hippo Repository Browser</title>");
            writer.println("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
            writer.println("  <style type=\"text/css\">");
            writer.println("    h3 {margin:2px}");
            writer.println("    table.params {font-size:small}");
            writer.println("    td.header {text-align: left; vertical-align: top; padding: 10px;}");
            writer.println("    td {text-align: left}");
            writer.println("    th {text-align: left}");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<table summary=\"infotable\">");
            writer.println("  <tr>");
            writer.println("    <td class=\"header\">");
            writer.println("      <h3>Searching</h3>");
            writer.println("      <table style=\"params\" summary=\"searching\">");
            writer.println("        <form method=\"get\" action=\"\" accept-charset=\"UTF-8\">");
            writer.println("        <tr>");
            writer.println("          <th>UUID: </th>");
            writer.println("          <td>");
            writer.println("              <input name=\"uuid\" type=\"text\" size=\"150\" value=\"" + getRequestParameter(req,"uuid","") + "\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        <tr>");
            writer.println("          <td>&nbsp;</td>");
            writer.println("          <td>");
            writer.println("              <input type=\"submit\" value=\"Fetch\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        </form>");
            writer.println("        <form method=\"get\" action=\"\" accept-charset=\"UTF-8\">");
            writer.println("        <tr>");
            writer.println("          <th>XPath: </th>");
            writer.println("          <td>");
            writer.println("              <input name=\"xpath\" type=\"text\" size=\"150\" value=\"" + getRequestParameter(req,"xpath","") + "\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        <tr>");
            writer.println("          <td>Limit: </td>");
            writer.println("          <td>");
            writer.println("              <input name=\"limit\" type=\"text\" size=\"5\" value=\"" + getRequestParameter(req,"limit","1000") + "\"/>");
            writer.println("              <input type=\"submit\" value=\"Search\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        </form>");
            writer.println("        <form method=\"get\" action=\"\" accept-charset=\"UTF-8\">");
            writer.println("        <tr>");
            writer.println("          <th>SQL: </th>");
            writer.println("          <td>");
            writer.println("              <input name=\"sql\" type=\"text\" size=\"150\" value=\"" + getRequestParameter(req,"sql","") + "\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        <tr>");
            writer.println("          <td>Limit: </td>");
            writer.println("          <td>");
            writer.println("              <input name=\"limit\" type=\"text\" size=\"5\" value=\"" + getRequestParameter(req,"limit","1000") + "\"/>");
            writer.println("              <input type=\"submit\" value=\"Search\"/>");
            writer.println("          </td>");
            writer.println("        </tr>");
            writer.println("        </form>");
            writer.println("      </table>");
            writer.println("    </td>");
            writer.println("    <td class=\"header\">");
            writer.println("      <h3>Request parameters</h3>");
            writer.println("      <table style=\"params\" summary=\"request parameters\">");
            writer.println("        <tr><th>name</th><th>value</th></tr>");
            writer.println("        <tr><td>servlet path : </td><td><code>" + req.getServletPath() + "</code></td></tr>");
            writer.println("        <tr><td>request uri : </td><td><code>" + req.getRequestURI() + "</code></td></tr>");
            writer.println("        <tr><td>relative path : </td><td><code>" + path + "</code></td></tr>");
            writer.println("      </table>");
            writer.println("    </td>");
            writer.println("    <td class=\"header\">");
            writer.println("      <h3>Login information</h3>");
            writer.println("      <table style=\"params\" summary=\"login parameters\">");
            writer.println("        <tr><th>logged in as : </th><td><code>" + session.getUserID() + "</code></td></tr>");
            writer.println("      </table>");
            writer.println("    </td>");
            writer.println("  </tr>");
            writer.println("</table>");
            writer.println("  <h3>Referenced node</h3>");

            // parse path
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            path = URLDecoder.decode(path, "UTF-8");
            Node node = session.getRootNode();
            if (!"".equals(path)) {
                node = node.getNode(path);
            }

            // create breadcrumb style path
            StringBuilder breadCrumb = new StringBuilder();
            breadCrumb.append("Accessing node <code>");
            String[] elements = path.split("/");
            int count = elements.length;
            if ("".equals(path)) {
                count = 0;
            }
            for (int i = 0; i < count + 1; i++) {
                breadCrumb.append("<a href=\"./");
                for (int j = i; j < count; j++) {
                    breadCrumb.append("../");
                }
                breadCrumb.append("\">");
                if (i == 0) {
                    breadCrumb.append("/root");
                } else {
                    breadCrumb.append(StringEscapeUtils.escapeHtml(NodeNameCodec.decode(elements[i - 1])));
                }
                breadCrumb.append("/</a>");
            }
            breadCrumb.append("</code>");
            writer.println(breadCrumb);

            writer.println("    <ul>");

            // list nodes
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                String childPath = child.getName();
                if (child.getIndex() > 1) {
                    childPath = childPath + "[" + child.getIndex() + "]";
                }
                childPath = URLEncoder.encode(childPath, "UTF-8");
                writer.print("    <li type=\"circle\"><a href=\"./" + childPath + "/" + "\">");
                String displayName = StringEscapeUtils.escapeHtml(NodeNameCodec.decode(child.getName()));
                if (child.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    writer.print(displayName + " [" + child.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]");
                } else {
                    writer.print(displayName);
                }
                writer.println("</a>");
            }

            // list properties
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                Property prop = iter.nextProperty();
                writer.print("    <li type=\"disc\">");
                writer.print("[name=" + prop.getName() + "] = ");
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    writer.print("[ ");
                    for (int i = 0; i < values.length; i++) {
                        if(values[i].getType() == PropertyType.BINARY) {
                            writer.print((i > 0 ? ", " : "") + prop.getLength() + " bytes.");
                        } else {
                            writer.print((i > 0 ? ", " : "") + values[i].getString());
                        }
                    }
                    writer.println(" ]");
                } else {
                    if(prop.getType() == PropertyType.BINARY) {
                        writer.print(prop.getLength() + " bytes.");
                    } else {
                        writer.println(prop.getString());
                    }
                }
            }

            writer.println("    </ul>");


            String queryString;
            if ((queryString = req.getParameter("xpath")) != null || (queryString = req.getParameter("sql")) != null) {
                QueryManager qmgr = session.getWorkspace().getQueryManager();

                String language = (req.getParameter("xpath") != null ? Query.XPATH: Query.SQL);
                Query query;
                if (language.equals(Query.XPATH)) {
                    // we encode xpath queries to support queries like /jcr:root/7_8//*
                    // the 7 needs to be encode
                    query = qmgr.createQuery(RepoUtils.encodeXpath(queryString), language);
                } else {
                    query = qmgr.createQuery(queryString, language);
                }

                String strLimit = req.getParameter("limit");
                if (strLimit != null && !strLimit.isEmpty()) {
                    query.setLimit(Long.parseLong(strLimit));
                }
                QueryResult result = query.execute();
                HippoNodeIterator iter = (HippoNodeIterator) result.getNodes();
                
                writer.println("  <h3>Query executed</h3>");
                writer.println("  <blockquote>");
                writer.println(StringEscapeUtils.escapeHtml(queryString));
                writer.println("  </blockquote>");
                writer.println("  Number of results found: " + iter.getTotalSize());
                writer.println("  <ol>");
               
                while (iter.hasNext()) {
                    Node resultNode = iter.nextNode();
                    if (resultNode != null) {
                        writer.print("    <li>");
                        writer.print(resultNode.getPath());
                        writer.println("</li>");
                    }
                }
                writer.println("  </ol><hr/><table summary=\"searchresult\">");
                result = query.execute();
                String[] columns = result.getColumnNames();
                writer.println("  <tr>");
                for (int i = 0; i < columns.length; i++) {
                    writer.print("    <th>");
                    writer.print(columns[i]);
                    writer.println("</th>");
                }
                writer.println("  </tr>");
                for (RowIterator rowIter = result.getRows(); rowIter.hasNext();) {
                    Row resultRow = rowIter.nextRow();
                    writer.println("    <tr>");
                    if (resultRow != null) {
                        Value[] values = resultRow.getValues();
                        if (values != null) {
                            for (int i = 0; i < values.length; i++) {
                                writer.print("    <td>");
                                writer.print(values[i] != null && values[i].getType() != PropertyType.BINARY ? values[i].getString() : "");
                                writer.println("</td>");
                            }
                        }
                    }
                    writer.println("  </tr>");
                }
                writer.println("</table>");
            }
            if ((queryString = req.getParameter("map")) != null) {
                writer.println("  <h3>Repository as map</h3>");
                Map map = repository.getRepositoryMap(node);
                if(!queryString.equals("")) {
                    StringTokenizer queryElts = new StringTokenizer(queryString, ".");
                    while (queryElts.hasMoreTokens()) {
                        map = (Map)map.get(queryElts.nextToken());
                    }
                }
                writer.println("  <blockquote>");
                writer.println("    _name = " + map.get("_name")+"<br/>");
                writer.println("    _location = " + map.get("_location")+"<br/>");
                writer.println("    _path = " + map.get("_path")+"<br/>");
                //writer.println("    _parent._path = " + ((Map)map.get("_parent")).get("_path").toString()+"<br/>");
                writer.println("    _index = " + map.get("_index")+"<br/>");
                writer.println("    _size = " + map.get("_size")+"<br/>");
                for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
                    //Object e = iter.next();
                    //writer.println("X"+e.getClass()+"<br/>");
                    //if(e instanceof Map.Entry) {
                    //Map.Entry entry = (Map.Entry)e;
                    //String key = entry.getKey().toString();
                    //Object value = entry.getValue().toString();

                    String key = (String) iter.next();
                    Object value = map.get(key);
                    if (value instanceof Map) {
                        writer.println("    " + key + "._path = " + ((Map)value).get("_path")+"<br/>");
                    } else {
                        writer.println("    " + key + " = " + (value != null ? value.toString() : "null" )+"<br/>");
                    }
                    //}
                }
                writer.println("  </blockquote>");
            }
            if ((queryString = req.getParameter("uuid")) != null) {
                writer.println("  <h3>Get node by UUID</h3>");
                writer.println("  <blockquote>");
                writer.println("UUID = " + StringEscapeUtils.escapeHtml(queryString));
                writer.println("  </blockquote>");
                writer.println("  <ol>");
                writer.println("    <li>");
                try {
                    Node n = session.getNodeByIdentifier(queryString);
                    writer.println("Found node: " + n.getPath());
                } catch (ItemNotFoundException e) {
                    writer.println("No node found for uuid " + StringEscapeUtils.escapeHtml(queryString));
                } catch (RepositoryException e) {
                    writer.println(e.getMessage());
                }
                writer.println("  </li> ");
                writer.println("  </ol><hr>");

            }
            if ((queryString = req.getParameter("deref")) != null) {
                writer.println("  <h3>Getting nodes having a reference to </h3>");
                writer.println("  <blockquote>");
                writer.println("UUID = " + StringEscapeUtils.escapeHtml(queryString));
                Node n = null;
                try {
                    n = session.getNodeByIdentifier(queryString);
                    writer.println(" ( " + n.getPath() + " )");
                } catch (RepositoryException e) {
                    writer.println(e.getMessage());
                }
                writer.println("  </blockquote><hr>");
                if (n != null) {
                    PropertyIterator propIt = n.getReferences();
                    if (propIt.hasNext()) {
                        writer.println("  <table>");
                        writer.println("  <tr><th align=left>");
                        writer.println("  Node path");
                        writer.println("  </th><th align=left>" );
                        writer.println("  Property reference name" );
                        writer.println("  </th></tr>");
                        while (propIt.hasNext()) {
                            Property prop = propIt.nextProperty();
                            writer.println("  <tr><td>");
                            writer.println(prop.getParent().getPath());
                            writer.println("    </td><td>");
                            writer.println("<b>"+prop.getName()+"</b>");
                            writer.println("    </td></tr>");
                        }
                        writer.println("  </table>");

                    }else {
                        writer.println("No nodes have a reference to '" +n.getPath() + "'");
                    }
                }
            }
            writer.println("</body></html>");
        } catch (LoginException ex) {
            BasicAuth.setRequestAuthorizationHeaders(res, "Repository");
        } catch (RepositoryException ex) {
            writer.println("<p>Error while accessing the repository, exception reads as follows:");
            writer.println("<pre>" + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
            writer.println("</body></html>");
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
