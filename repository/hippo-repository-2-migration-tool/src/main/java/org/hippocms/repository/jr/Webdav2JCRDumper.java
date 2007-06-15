/*
 * Copyright 2007 Hippo (www.hippo.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;

import nl.hippo.webdav.batchprocessor.Configuration;
import nl.hippo.webdav.batchprocessor.OperationOnDeletedNodeException;
import nl.hippo.webdav.batchprocessor.Plugin;
import nl.hippo.webdav.batchprocessor.PluginConfiguration;
import nl.hippo.webdav.batchprocessor.ProcessingException;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessor;
import nl.hippo.webdav.batchprocessor.WebdavBatchProcessorException;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;

public class Webdav2JCRDumper implements Plugin {

    // ---------------------------------------------------- Constants
    private static final String JCR_RMI_HOST = "jcr.rmi.host";
    private static final String JCR_RMI_PORT = "jcr.rmi.port";

    private static final String JCR_REPOSITORY = "jcr.repository";
    private static final String JCR_WORKSPACE = "jcr.workspace";
    private static final String JCR_USER = "jcr.rmi.user";
    private static final String JCR_PASS = "jcr.rmi.pass";
    private static final String JCR_PATH = "jcr.path";

    private static final String DAV_NAMESPACE = "DAV:";
    private static final String HIPPO_NAMESPACE = "http://hippo.nl/cms/1.0";

    // subnode of the rootnode that contains the authors
    private static final String AUTHOR_NODE = "authors";
    private static final String AUTHOR_NODETYPE = "hippo:author";
    private static final String AUTHOR_ID_PROPERTY = "authorID";

    // subnode of the rootnode that contains the sections
    private static final String SECTION_NODE = "sections";
    private static final String SECTION_NODETYPE = "hippo:section";
    private static final String SECTION_ID_PROPERTY = "sectionID";

    // subnode of the rootnode that contains the categories
    private static final String CATEGORY_NODE = "catagories";
    private static final String CATEGORY_NODETYPE = "hippo:catagory";
    private static final String CATEGORY_ID_PROPERTY = "catagoryId";

    // subnode of the rootnode that contains the magazines
    private static final String MAGAZINE_NODE = "magazines";
    private static final String MAGAZINE_NODETYPE = "hippo:magazine";
    private static final String MAGAZINE_ID_PROPERTY = "magazineId";

    /* DAV::creationate = 2006-12-18T14:24:53Z */
    private static final SimpleDateFormat CREATIONDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /* hippo::publicationDate = 20040124 */
    private static final SimpleDateFormat PUBLICATIONDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /* hippo::documentdate = 20040124 */
    private static final SimpleDateFormat DOCUMENTDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    // ---------------------------------------------------- Instance variables
    // rmi
    private String rmiHost;
    private String rmiPort;

    // jcr
    private String jcrRepository;
    private String jcrWorkspace;
    private String jcrUser;
    private String jcrPass;
    private String jcrPath;

    private Session session;
    private ValueFactory valueFactory;
    private String webdavRootUri;

    // backup & embedded server
    //private String dumpFile;
    //private String workingDir;
    //private Server server;

    public void configure(WebdavBatchProcessor processor, Configuration config, PluginConfiguration pluginConfig) {

        // Fetch properties
        webdavRootUri = config.getRootUri();
        //workingDir = pluginConfig.getValue("jcr.workingdir");
        //dumpFile = pluginConfig.getValue("dumpfile");

        // rmi connection
        rmiHost = pluginConfig.getValue(JCR_RMI_HOST);
        rmiPort = pluginConfig.getValue(JCR_RMI_PORT);

        // jcr settings
        jcrRepository = pluginConfig.getValue(JCR_REPOSITORY);
        jcrWorkspace = pluginConfig.getValue(JCR_WORKSPACE);
        jcrUser = pluginConfig.getValue(JCR_USER);
        jcrPass = pluginConfig.getValue(JCR_PASS);
        jcrPath = pluginConfig.getValue(JCR_PATH);

        // remove trainling slash
        if (jcrPath.endsWith("/")) {
            jcrPath = jcrPath.substring(0, jcrPath.length() - 1);
        }

        // show config
        printConfig(config);

        // test and setup connection and login       
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository;
        try {
            // get the repository
            repository = factory.getRepository(getRmiUrl());

            // login and get session
            session = repository.login(new SimpleCredentials(jcrUser, jcrPass.toCharArray()));

            // todo, add option to set and create workspace
            Workspace ws = session.getWorkspace();
            if (!ws.getName().equals(jcrWorkspace)) {
                throw new RuntimeException("Workspaces not equal. Found: " + ws.getName() + " requested: "
                        + jcrWorkspace);
            }

            // test and create target path
            checkPath();

            valueFactory = session.getValueFactory();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void process(nl.hippo.webdav.batchprocessor.Node webdavNode) throws WebdavBatchProcessorException {
        String jcrParentPath = jcrPath + parentPath(webdavNode.getUri());
        String nodeName = nodeName(webdavNode.getUri());

        if (webdavNode.getUri().equals(webdavRootUri)) {
            return;
        }

        try {
            //System.out.println("Converting WebDAV node: " + webdavNode.getUri() + " => " + jcrParentPath + "/" + nodeName);
            System.out.append('.');

            javax.jcr.Node parent = (javax.jcr.Node) session.getItem(jcrParentPath);

            // Add content property
            if (!webdavNode.isCollection()) {

                Property typeProp = webdavNode.getProperty(HIPPO_NAMESPACE, "type");

                //if (typeProp != null && typeProp.getPropertyAsString().equals("newsarticle")) {
                    convertNewsArticleToJCR(webdavNode, nodeName, parent);
                //} else {
                //    convertNodeToJCR(webdavNode, nodeName, parent);
                //}

            } else {
                // create 'collection' nodes if they don't exist
                if (!parent.hasNode(nodeName)) {
                    parent.addNode(nodeName);
                }
            }
            session.save();

        } catch (PathNotFoundException e) {
            System.err.println("");
            System.err.println("JCR Path does not exist: " + e.getMessage());
            System.err.println("Unable to create: " + nodeName);
            throw new RuntimeException("Path does not exist: " + e.getMessage());
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void convertNodeToJCR(nl.hippo.webdav.batchprocessor.Node webdavNode, String nodeName, javax.jcr.Node parent)
            throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException,
            LockException, RepositoryException, NoSuchNodeTypeException, ValueFormatException, ProcessingException,
            OperationOnDeletedNodeException, NumberFormatException, IOException {

        // Overwrite existing nodes
        if (parent.hasNode(nodeName)) {
            parent.getNode(nodeName).remove();
        }

        // Create the new JCR node
        javax.jcr.Node current = parent.addNode(nodeName);

        current.addMixin("mix:referenceable");
        current.setProperty("published", false);

        String contentLengthAsString = webdavNode.getProperty(DAV_NAMESPACE, "getcontentlength").getPropertyAsString();
        int contentLength = Integer.parseInt(contentLengthAsString);

        if (contentLength > 0) {
            byte[] content = webdavNode.getContents();
            current.setProperty("content", valueFactory.createValue(new ByteArrayInputStream(content)));
        }

        // Add metadata properties
        Iterator webdavPropertyNames = webdavNode.propertyNamesIterator();
        while (webdavPropertyNames.hasNext()) {
            PropertyName webdavPropertyName = (PropertyName) webdavPropertyNames.next();
            String webdavPropertyNamespace = webdavPropertyName.getNamespaceURI();
            if (webdavPropertyNamespace.equals(HIPPO_NAMESPACE)) {
                String name = webdavPropertyName.getLocalName();
                Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);

                if (name.equals("publicationDate")) {
                    current.setProperty("published", true);
                    current.setProperty("publicationdate", getCalendarFromProperty(webdavProperty, PUBLICATIONDATE_FORMAT));

                } else if (name.equals("documentdate")) {
                    Calendar c = getCalendarFromProperty(webdavProperty, DOCUMENTDATE_FORMAT);
                    current.setProperty("documentdate", c);
                    current.setProperty("year", c.get(Calendar.YEAR));
                    current.setProperty("month", 1 + c.get(Calendar.MONTH));
                    current.setProperty("day", c.get(Calendar.DAY_OF_MONTH));
                } else {
                    Value value = valueFactory.createValue(webdavProperty.getPropertyAsString());
                    current.setProperty(name, value);
                }
            } else {
                /*
                 String name = webdavPropertyName.getLocalName();
                 if (name.equals("creationdate")) {
                 Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);
                 Date d = new Date();
                 try
                 {
                 d = CREATIONDATE_FORMAT.parse(webdavProperty.getPropertyAsString());
                 Calendar c = Calendar.getInstance();
                 c.setTime(d);
                 current.setProperty("year", c.get(Calendar.YEAR));
                 current.setProperty("month", 1 + c.get(Calendar.MONTH));
                 current.setProperty("day", c.get(Calendar.DAY_OF_MONTH));
                 } catch (java.text.ParseException e) {
                 }
                 
                 }
                 */

            }
        }
    }

    private void convertNewsArticleToJCR(nl.hippo.webdav.batchprocessor.Node webdavNode, String nodeName,
            javax.jcr.Node parent) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException, NoSuchNodeTypeException,
            ValueFormatException, ProcessingException, OperationOnDeletedNodeException, NumberFormatException,
            IOException {

        // Overwrite existing nodes
        if (parent.hasNode(nodeName)) {
            parent.getNode(nodeName).remove();
        }
        
        long id = 1 + getMaxId("hippo:newsArticle");
        
        // Create the new JCR node
        javax.jcr.Node newsArticle = parent.addNode(nodeName,"hippo:newsArticle");

        newsArticle.setProperty("hippo:id", id);
        
        //newsArticle.addMixin("mix:referenceable");
        //newsArticle.setProperty("published", false);

        
        // Body
        String contentLengthAsString = webdavNode.getProperty(DAV_NAMESPACE, "getcontentlength").getPropertyAsString();
        int contentLength = Integer.parseInt(contentLengthAsString);
        if (contentLength > 0) {
            // Create the new JCR body node
            javax.jcr.Node body = newsArticle.addNode("body", "hippo:body");
            
            byte[] content = webdavNode.getContents();
            body.setProperty("page", valueFactory.createValue(new ByteArrayInputStream(content)));

            Property captionProp = webdavNode.getProperty(HIPPO_NAMESPACE, "caption");
            body.setProperty("title", captionProp.getPropertyAsString());

            // TODO: get summary from content
            // TODO: get locale?
        }

        
        Property prop;
        
        // Author 
        prop = webdavNode.getProperty(HIPPO_NAMESPACE, "author");
        id = getIdOrCreate(prop.getPropertyAsString(), AUTHOR_NODETYPE, AUTHOR_NODE);
        newsArticle.setProperty(AUTHOR_ID_PROPERTY, id);
        
        // Section
        prop = webdavNode.getProperty(HIPPO_NAMESPACE, "section");
        id = getIdOrCreate(prop.getPropertyAsString(), SECTION_NODETYPE, SECTION_NODE);
        newsArticle.setProperty(SECTION_ID_PROPERTY, id);

        // Magazine
        prop = webdavNode.getProperty(HIPPO_NAMESPACE, "source");
        id = getIdOrCreate(prop.getPropertyAsString(), MAGAZINE_NODETYPE, MAGAZINE_NODE);
        newsArticle.setProperty(MAGAZINE_ID_PROPERTY, id);
        

        // Newsdate
        prop = webdavNode.getProperty(HIPPO_NAMESPACE, "documentdate");
        if (prop != null) {
            newsArticle.setProperty("hippo:newsDate", getCalendarFromProperty(prop, DOCUMENTDATE_FORMAT));
        }
        
        // Publication
        prop = webdavNode.getProperty(HIPPO_NAMESPACE, "publicationdate");
        if (prop != null) {
            newsArticle.setProperty("hippo:published", true);
            newsArticle.setProperty("hippo:publicationDate", getCalendarFromProperty(prop, PUBLICATIONDATE_FORMAT));
        }

        
        // TODO: Category
        // TODO: Imageset
        // TODO: Links
        // TODO: Relations
        
        

    }

    public boolean requiresNodeOrderPreservation() {
        return true;
    }

    public void postprocess() {
        session.logout();
    }

    private String parentPath(String uri) {
        String result = uri.substring(webdavRootUri.length());

        int i = result.lastIndexOf("/");
        result = i == -1 ? result : result.substring(0, i);

        return result;
    }

    private String nodeName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    private String getRmiUrl() {
        return "rmi://" + rmiHost + ":" + rmiPort + "/" + jcrRepository;
    }

    private void checkPath() {
        try {
            javax.jcr.Node node = (javax.jcr.Node) session.getRootNode();
            String currentPath = "";

            StringTokenizer st = new StringTokenizer(jcrPath, "/");

            while (st.hasMoreTokens()) {

                String nodeName = st.nextToken();

                if (nodeName == null || "".endsWith(nodeName)) {
                    continue;
                }
                System.out.println("Checking for: " + currentPath + "/" + nodeName);

                // add node if it doesn't exist
                if (!node.hasNode(nodeName)) {
                    node.addNode(nodeName);
                    System.out.println("Added node for jcrPath: " + currentPath);
                }
                currentPath += "/" + nodeName;

                // shift to child node 
                node = node.getNode(nodeName);
            }
            session.save();

        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Calendar getCalendarFromProperty(Property webdavProperty, SimpleDateFormat dateFormat) {
        Date d;
        try {
            d = dateFormat.parse(webdavProperty.getPropertyAsString());
        } catch (java.text.ParseException e) {
            // use now if the date can't be parsed
            d = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }
    
    private long getIdOrCreate(String name, String nodeType, String baseNode) throws RepositoryException {
        long id = getId(name, nodeType);
        // id has contraint >0 
        if (id < 0) {
            id = createIdNode(name, nodeType, baseNode);
        }
        return id;
    }

    private long getId(String name, String nodeType) {
        long id = -1;
        String sql = "SELECT hippo:id FROM " + nodeType + " WHERE  hippo:name = '" + name + "'";
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            QueryResult result = q.execute();
            RowIterator it = result.getRows();
            if (it.hasNext()) {
                Value idValue = it.nextRow().getValue("hippo:id");
                if (idValue != null ) {
                    id = idValue.getLong();
                }
            }
        } catch (RepositoryException e) {
            System.err.println(e);
        }

        return id;
    }
    
    private long createIdNode(String name, String nodeType, String baseNode) throws RepositoryException {
        long id = 1 + getMaxId(nodeType);

        checkAndCreateFolderNode(baseNode);
        javax.jcr.Node parent = session.getRootNode().getNode(baseNode);
        javax.jcr.Node author = parent.addNode(name, nodeType);

        author.setProperty("hippo:id", id);
        author.setProperty("hippo:name", name);

        return id;
        
    }
    
    private void checkAndCreateFolderNode(String nodeName) throws RepositoryException {
        if (!session.getRootNode().hasNode(nodeName)) {
            session.getRootNode().addNode(nodeName, "nt:folder");
        }
    }
    
    private long getMaxId(String nodeType) {
        long id = 0;
        
        String sql = "SELECT hippo:id FROM " + nodeType + " ORDER BY hippo:id DESC";
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            QueryResult result = q.execute();
            RowIterator it = result.getRows();
            if (it.hasNext()) {
                Value idValue = it.nextRow().getValue("hippo:id");
                if (idValue != null ) {
                    id = idValue.getLong();
                }
            }
        } catch (RepositoryException e) {
            System.err.println(e);
        }
        return id;
    }
    
    private void printConfig(Configuration config) {
        System.out.println("******************************************************");
        System.out.println("******************************************************");
        System.out.println("Exporting from WebDAV: ");
        System.out.println("  Url         : http://" + config.getLocationHost() + ":" + config.getLocationPort());
        System.out.println("  Repository  : " + config.getLocationRootPath());
        System.out.println("  Path        : " + config.getFullPath());
        System.out.println("Importing in JCR:");
        System.out.println("  RMI Url     : " + getRmiUrl());
        System.out.println("  Repository  : " + jcrRepository);
        System.out.println("  Workspace   : " + jcrWorkspace);
        System.out.println("  Path        : " + jcrPath);
        System.out.println("******************************************************");
        System.out.println("******************************************************");
    }
}
