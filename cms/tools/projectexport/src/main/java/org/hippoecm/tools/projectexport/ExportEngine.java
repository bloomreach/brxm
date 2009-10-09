/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools.projectexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.hippoecm.tools.projectexport.Element.ContentElement;
import org.hippoecm.tools.projectexport.Element.NamespaceElement;
import org.hippoecm.tools.projectexport.Element.ProjectElement;

class ExportEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(ExportEngine.class);

    Set<String> ignorePaths = new HashSet<String>();
    
    Set<String> ignoreTrees = new HashSet<String>();
    
    Set<String> ignoreNamespaces = new HashSet<String>();
 
    Set<String> ignoreProjects = new HashSet<String>();
    
    LinkedHashSet<Element> elements = new LinkedHashSet<Element>();

    Set<String> paths = new HashSet<String>();

    /** returns a subset of the input elements set, matching the project requested */
    Set<Element> projectElements(Set<Element> elements, String project) {
        ProjectElement element = projectElement(elements, project);
        return element.elements;
    }

    Set<ProjectElement> projectElements(Set<Element> elements) {
        Set<ProjectElement> projects = new LinkedHashSet<ProjectElement>();
        for(Element element : elements)
            if(element instanceof ProjectElement)
                projects.add((ProjectElement)element);
        return projects;
    }
    
    ProjectElement projectElement(Set<Element> elements, String name) {
        for(ProjectElement project :projectElements(elements))
            if(name.equals(project.projectName))
                return project;
        return null;
    }

    /** returns a by path breath-first sorted map of ContentElements contained in the input set */
    static SortedMap<String, ContentElement> contentElements(Set<Element> elements) {
        SortedMap<String, ContentElement> list = new TreeMap<String, ContentElement>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                if(o1 == null)
                    return o2 == null ? 0 : -1;
                return o1.compareTo(o2);
            }
        });
        for(Element element : elements) {
            if(element instanceof ContentElement)
                list.put(((ContentElement)element).path, (ContentElement)element);
            else if(element instanceof ProjectElement)
                list.putAll(contentElements(((ProjectElement)element).elements));
        }
        return list;
   }

    /** returns a by dependency then name sorted map of contentElements contained in the input set */
    static List<NamespaceElement> namespaceElements(Set<Element> elements) {
        List<NamespaceElement> list = new LinkedList<NamespaceElement>();
        for(Element element : elements) {
            if(element instanceof NamespaceElement)
                list.add((NamespaceElement)element);
            else if(element instanceof ProjectElement)
                list.addAll(namespaceElements(((ProjectElement)element).elements));
        }
        // FIXME order the list
        return list;
    }

    public void base(Node root, Node projectsScratch, Node contentScratch) throws RepositoryException, IOException, NotExportableException {
        ((HippoSession)projectsScratch.getSession()).importDereferencedXML(contentScratch.getPath(), LocalHippoRepository.class.getResourceAsStream("configuration.xml"), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, ImportMergeBehavior.IMPORT_MERGE_THROW);
        elements.add(new Element.ContentElement(contentScratch.getNode("hippo:configuration")));

        Set<String> prefixes = new HashSet<String>();
        for (String prefix : root.getSession().getNamespacePrefixes()) {
            if (!prefix.equals("") && !prefix.contains("_")) {
                if (log.isDebugEnabled()) {
                    log.debug("prefix \""+prefix+"\"");
                }
                prefixes.add(prefix);
            }
        }
        for (Enumeration<URL> e = getClass().getClassLoader().getResources("hippoecm-extension.xml"); e.hasMoreElements();) {
            URL url = e.nextElement();
            if (log.isDebugEnabled()) {
                log.debug("module "+url.toString());
            }
            ProjectElement projectElement = new ProjectElement(url, projectsScratch);
            elements.add(projectElement);
        }
        projectsScratch.save();

        for (Enumeration<URL> e = getClass().getClassLoader().getResources("org/hippoecm/repository/extension.xml"); e.hasMoreElements();) {
            URL url = e.nextElement();
            if(log.isDebugEnabled()) {
                log.debug("extension "+url.toString());
            }
            InputStream project = url.openStream();
            ((HippoSession)projectsScratch.getSession()).importDereferencedXML(projectsScratch.getPath(), project, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, ImportMergeBehavior.IMPORT_MERGE_THROW);
            if(!projectsScratch.hasNode("hippo:initialize") || !projectsScratch.getNode("hippo:initialize").isNodeType("hippo:initializefolder")) {
                throw new NotExportableException("bad project description");
            }
            Matcher match = Pattern.compile("^jar:file:.*/([a-zA-z\\-]*)-[0-9A-Za-z\\-\\.]*.jar\\!.*$").matcher(url.toString());
            if (!match.matches()) {
                throw new NotExportableException("unrecognized extension location");
            }
            String name = match.group(1);
            projectsScratch.getSession().move(projectsScratch.getPath()+"/hippo:initialize", projectsScratch.getPath()+"/"+name);
            projectsScratch.save();
        }
        projectsScratch.save();
        contentScratch.save();

        QueryManager queryManager = projectsScratch.getSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM hippo:initializeitem WHERE jcr:path = '"+projectsScratch.getPath()+"/%' ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC", Query.SQL);
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            if (log.isDebugEnabled()) {
                log.debug("content "+node.getPath());
            }
            if (node != null && node.hasProperty("hippo:contentresource")) {
                String absPath = node.getProperty("hippo:contentroot").getString();
                String relPath = (absPath.startsWith("/") ? absPath.substring(1) : absPath);
                if (relPath.length() > 0 && !contentScratch.hasNode(relPath)) {
                    contentScratch.addNode(relPath);
                    contentScratch.save();
                }
                ProjectElement projectElement = projectElement(elements, node.getParent().getName());
                if(projectElement != null) {
                    URL contentURL = new URL(projectElement.url, node.getProperty("hippo:contentresource").getString());
                    ((HippoSession)contentScratch.getSession()).importDereferencedXML(contentScratch.getPath() + absPath, contentURL.openStream(),
                            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                            ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                    Node contentroot = (relPath.length() > 0 ? contentScratch.getNode(relPath) : contentScratch);
                    NodeIterator pendingChangesIter = ((HippoNode)contentroot).pendingChanges("nt:base", true);
                    if(pendingChangesIter.hasNext()) {
                        Node content = pendingChangesIter.nextNode();
                        if (pendingChangesIter.hasNext()) {
                            boolean reallyMultipleChanges = false;
                            while(pendingChangesIter.hasNext()) {
                                Node changed = pendingChangesIter.nextNode();
                                if (content.isSame(contentroot)) {
                                    content = changed;
                                } else if (!changed.isSame(contentroot)) {
                                    reallyMultipleChanges = true;
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("multiple changes: "+content.getPath()+" and "+changed.getPath()+" in "+contentroot.getPath());
                                }
                            }
                            if (reallyMultipleChanges) {
                                throw new NotExportableException("multiple changes in one xml file");
                            }
                        }
                        projectElement.elements.add(new ContentElement(node.getName(), (absPath.endsWith("/") ? absPath : absPath + "/")+content.getName(), node.getProperty("hippo:contentresource").getString(), content));
                        contentScratch.save();
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("expected some change");
                        }
                        contentScratch.refresh(false);
                    }
                } else {
                    InputStream stream = getClass().getClassLoader().getResourceAsStream("org/hippoecm/repository/" + node.getProperty("hippo:contentresource").getString());
                    ((HippoSession)contentScratch.getSession()).importDereferencedXML(contentScratch.getPath() + absPath, stream,
                            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                            ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                    contentScratch.save();
                }
            }
        }

        contentScratch.save();

        for (ProjectElement project : projectElements(elements)) {
            if (log.isDebugEnabled()) {
                log.debug("project "+project.projectName);
            }
            project.expand(project, contentScratch);
        }
        
        for (NodeIterator iter = root.getNode("hippo:namespaces").getNodes(); iter.hasNext();) {
            Node namespaceNode = iter.nextNode();
            if (namespaceNode != null && namespaceNode.isNodeType("hipposysedit:namespace")) {
                if (prefixes.contains(namespaceNode.getName())) {
                    if(!ignoreNamespaces.contains(namespaceNode.getName())) {
                        elements.add(new NamespaceElement(namespaceNode));
                        prefixes.remove(namespaceNode.getName());
                    }
                } else if(!namespaceNode.getName().equals("system")){
                    if(!ignoreNamespaces.contains(namespaceNode.getName())) {
                        throw new NotExportableException("Namespace "+namespaceNode.getName()+" was never instantiated");
                    }
                }
            }
        }
        for (ProjectElement project : projectElements(elements)) {
            for(NamespaceElement namespace : namespaceElements(project.elements)) {
                prefixes.remove(namespace.prefix);
            }
        }
        for (String prefix : prefixes) {
            if(!ignoreNamespaces.contains(prefix)) {
                elements.add(new NamespaceElement(prefix, root.getSession().getNamespaceURI(prefix)));
            }
        }
        for (ContentElement content : contentElements(elements).values()) {
            paths.add(content.getPath());
        }
        for (ProjectElement project : projectElements(elements)) {
            for (ContentElement content : contentElements(project.elements).values()) {
                paths.add(content.getPath());
            }
        }
        if (log.isDebugEnabled()) {
            for(String path : paths) {
                log.debug("path "+path);
            }
        }
    }

    static void exportPOM(String fullname, OutputStream ostream) {
        String name, group, description;
        if(fullname == null || fullname.trim().equals("")) {
            fullname = "com.company.project";
        } else {
            fullname = fullname.trim();
        }
        if(fullname.contains(".")) {
          name = fullname.substring(fullname.lastIndexOf(".")+1);
          group = fullname.substring(0, fullname.lastIndexOf("."));
        } else {
            name = group = fullname;
        }
        description = Character.toUpperCase(name.charAt(0))+name.substring(1);
        PrintWriter out = new PrintWriter(ostream);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
        out.println("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">");
        out.println("  <modelVersion>4.0.0</modelVersion>");
        out.println("");
        out.println("  <name>"+name+"</name>");
        out.println("  <description>"+description+"</description>");
        out.println("  <groupId>"+group+"</groupId>");
        out.println("  <version>1.0-SNAPSHOT</version>");
        out.println("  <artifactId>artifact</artifactId>");
        out.println("  <packaging>jar</packaging>");
        out.println("");
        out.println("  <build>");
        out.println("    <defaultGoal>package</defaultGoal>");
        out.println("  </build>");
        out.println("");
        out.println("</project>");
        out.flush();
    }

    Session session;

    public ExportEngine(Session session) throws RepositoryException, IOException, NotExportableException {
        this.session = session;
        Node root = session.getRootNode();
        Node scratch = root.getNode("hippo:configuration/hippo:temporary"); 
        while (scratch.hasNode("projects")) {
            scratch.getNode("projects").remove();
        }
        while (scratch.hasNode("content")) {
            scratch.getNode("content").remove();
        }
        scratch.save();
        try {
            ignorePaths.add("/jcr:system");
            ignorePaths.add("/hippo:configuration");
            ignorePaths.add("/hippo:configuration/hippo:frontend/cms/cms-pickers");
            ignoreTrees.add("/hippo:configuration/hippo:temporary");
            ignoreTrees.add("/hippo:log");
            ignoreTrees.add("/hippo:namespaces");
            ignoreTrees.add("/hippo:namespaces/hippo");
            ignoreTrees.add("/live");
            ignoreTrees.add("/preview");
            ignoreTrees.add("/content/documents/state");
            ignoreTrees.add("/content/documents/tags");
            ignoreNamespaces.add("fn");
            ignoreNamespaces.add("fn_old");
            ignoreNamespaces.add("xml");
            ignoreNamespaces.add("xs");
            ignoreNamespaces.add("jcr");
            ignoreNamespaces.add("mix");
            ignoreNamespaces.add("nt");
            ignoreNamespaces.add("rep");
            ignoreNamespaces.add("sv");
            ignoreNamespaces.add("hippo");
            ignoreNamespaces.add("hippogallery");
            ignoreNamespaces.add("hippolog");
            ignoreNamespaces.add("hipposched");
            ignoreNamespaces.add("hippostd");
            ignoreNamespaces.add("hipposys");
            ignoreNamespaces.add("hipposysedit");
            ignoreNamespaces.add("frontend");
            ignoreNamespaces.add("editmodel");
            ignoreNamespaces.add("editor");
            ignoreNamespaces.add("reporting");
            ignoreNamespaces.add("defaultcontent");
            ignoreNamespaces.add("hippoldap");
            ignoreProjects.add("Frontend engine");
            ignoreProjects.add("Repository modules");
            ignoreProjects.add("Hippo ECM package configuration");
            ignoreProjects.add("Hippo ECM editor repository addon");
            ignoreProjects.add("Gallery Addon");
            //ignoreProjects.add("Defaultcontent repository addon");
            ignoreProjects.add("Builtin repository addon");
            ignoreProjects.add("Faceted date repository addon");
            ignoreProjects.add("Xinha addon frontend plugin");
            ignoreProjects.add("Hippo ECM authorisation configuration");
            ignoreProjects.add("Reviewed action repository addon");
            ignoreProjects.add("Frontend plugins");
            Node projectsNode = scratch.addNode("projects");
            Node contentNode = scratch.addNode("content");
            session.save();
            base(root, projectsNode, contentNode);
            diff(root);
            
            ProjectElement newProject = new ProjectElement();
            for(Iterator<Element> elementIter = elements.iterator(); elementIter.hasNext(); ) {
                Element element = elementIter.next();
                if(element instanceof ProjectElement) {
                    if(ignoreProjects.contains(((ProjectElement)element).projectName)) {
                        elementIter.remove();
                    }
                } else if(element instanceof ContentElement) {
                    String path = ((ContentElement)element).path;
                    boolean include = !ignorePaths.contains(path);
                    if (include == true) {
                        while (path.lastIndexOf("/") > 1) {
                            path = path.substring(0, path.lastIndexOf("/"));
                            if (ignoreTrees.contains(path)) {
                                include = false;
                                break;
                            }
                        }
                    }
                    if (include) {
                        newProject.elements.add(element);
                        elementIter.remove();
                    }
                } else if(element instanceof NamespaceElement) {
                    newProject.elements.add(element);
                    elementIter.remove();
                    if(((NamespaceElement)element).uri == null) {
                        ((NamespaceElement)element).uri = session.getNamespaceURI(element.getElementName());
                    }
                    newProject.elements.add(new ContentElement(session.getRootNode().getNode("hippo:namespaces").getNode(element.getElementName())));
                }
            }
            elements.add(newProject);
        } finally {
        }
    }

    public void close() throws RepositoryException {
        Node root = session.getRootNode();
        Node scratch = root.getNode("hippo:configuration/hippo:temporary"); 
        scratch.refresh(false);
        if(scratch.hasNode("projects")) {
            scratch.getNode("projects").remove();
        }
        if(scratch.hasNode("content")) {
            scratch.getNode("content").remove();
        }
        scratch.save();
    }

    private void diff(Node root) throws RepositoryException {
        // now check all content elements for changes
        Set<String> paths = new HashSet<String>();
        paths.addAll(this.paths);
        for (ContentElement content : contentElements(elements).values()) {
            paths.add(content.getPath());
        }
        for (NodeIterator iter = root.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            if (node != null) {
                if (!paths.contains(node.getPath())) {
                    paths.add(node.getPath());
                    elements.add(new ContentElement(node));
                }
            }
        }
        JcrDiff diff = new JcrDiff(paths);
        Set<ContentElement> addedContent = new HashSet<ContentElement>();
        for (ContentElement content : contentElements(elements).values()) {
            Node previous = content.getPrevious();
            Node current = content.getCurrent();
            if(previous == null) {
                if (log.isDebugEnabled()) {
                    log.debug("compare " + content.getPath() + " new");
                }
                content.setDirty();
            } else if(current == null) {
                if (log.isDebugEnabled()) {
                    log.debug("compare " + content.getPath() + " deleted");
                }
                content.setDirty();
            } else if (!(diff.diff(content.getPrevious(), content.getCurrent()))) {
                if (log.isDebugEnabled()) {
                    log.debug("compare " + content.getPath() + " dirty");
                }
                for(Node added : diff.addedNodes) {
                    addedContent.add(new ContentElement(added));
                }
                content.setDirty();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("compare " + content.getPath() + " clean");
                }
            }
            elements.addAll(addedContent);
        }
    }

    XMLExport xmlexport = new XMLExport();

    static ContentElement buildExtension(Set<Element> elements, Node extension) throws RepositoryException, NotExportableException {
        boolean sequence = true;
        List<NamespaceElement> namespaces = namespaceElements(elements);

        if(extension.hasNode("hippo:initialize"))
            throw new NotExportableException("already selected for export");
        extension = extension.addNode("hippo:initialize", "hippo:initializefolder");

        double sequenceNumber = 10000.0;
        for (NamespaceElement element : namespaces) {
            Node item = extension.addNode(element.getElementName(), "hippo:initializeitem");
            if (sequence) {
                item.setProperty("hippo:sequence", sequenceNumber);
                sequenceNumber += 1.0;
            }
            if (element.uri != null) {
                item.setProperty("hippo:namespace", element.uri);
            }
            if (element.nodetypes != null) {
                if(element.nodetypes.size() == 1) {
                    item.setProperty("hippo:nodetypesresource", element.nodetypes.iterator().next().getElementName() + ".cnd");
                }
                if (element.nodetypes.size() > 1) {
                    for (NamespaceElement cnd : element.nodetypes) {
                        item = extension.addNode(cnd.getElementName(), "hippo:initializeitem");
                        if (sequence) {
                            item.setProperty("hippo:sequence", sequenceNumber);
                            sequenceNumber += 1.0;
                        }
                        item.setProperty("hippo:nodetypesresource", cnd.getElementName() + ".cnd");
                    }
                }
            } else {
               item.setProperty("hippo:nodetypesresource", element.getElementName() + ".cnd");
               try {
                   element.cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(extension.getSession().getWorkspace(), element.getElementName());
               } catch(IOException ex) {
                   throw new NotExportableException("error writing content node definition", ex);
               }
            }
        }
        for(ContentElement element : contentElements(elements).values()) {
            String name = element.getElementName();
            if(extension.hasNode(name)) {
                int count = 1;
                while(extension.hasNode(name+count))
                    ++count;
                name += count;
            }
            Node item = extension.addNode(name, "hippo:initializeitem");
            if (sequence) {
                item.setProperty("hippo:sequence", sequenceNumber);
                sequenceNumber += 1.0;
            }
            item.setProperty("hippo:contentroot", element.path.substring(0, element.path.lastIndexOf("/")));
            item.setProperty("hippo:contentresource", element.file);
        }
        return new ContentElement(extension, "hippoecm-extension.xml");
    }
    
    public List<Element> getElements(Element parent) {
        if(parent == null) {
            return new LinkedList<Element>(projectElements(elements));
        } else if(parent instanceof ProjectElement) {
            return new LinkedList<Element>(((ProjectElement)parent).elements);
        } else {
            return null;
        }
    }

    public Iterator<String> projectIterator() throws RepositoryException {
        Set<String> projects = new HashSet<String>();
        Node projectsScratch = session.getRootNode().getNode("hippo:configuration/hippo:temporary/projects");
        for(NodeIterator iter = projectsScratch.getNodes(); iter.hasNext(); )
            projects.add(iter.nextNode().getName());
        return projects.iterator();
    }
    private String selectedProject;
    private Collection<ContentElement> selectedContentElements;
    private Collection<NamespaceElement> selectedNamespaceElements;
    public void selectProject(String name) throws RepositoryException, IOException, NotExportableException {
        if (log.isDebugEnabled()) {
            log.debug("export project selected "+name);
        }
        selectedContentElements = prepare(name);
    }
    public void exportProject(OutputStream ostream) throws RepositoryException, IOException, NotExportableException {
        if (log.isDebugEnabled()) {
            log.debug("export project to stream ");
        }
        export(selectedProject, selectedContentElements, selectedNamespaceElements, ostream);
    }
    public void exportProject(File basedir) throws RepositoryException, IOException, NotExportableException {
        if (log.isDebugEnabled()) {
            log.debug("export project to basedir "+basedir.toString());
        }
        export(selectedProject, selectedContentElements, selectedNamespaceElements, basedir);
    }

    Collection<ContentElement> prepare(String project) throws IOException, RepositoryException, NotExportableException {
        Node scratch = session.getRootNode().getNode("hippo:configuration/hippo:temporary/projects");
        Set<Element> projectElements = projectElements(elements, project);
        Set<ContentElement> contents = new HashSet<ContentElement>(contentElements(projectElements).values());
        ContentElement extension = buildExtension(projectElements, scratch);
        scratch.save();
        contents.add(extension);
        selectedNamespaceElements = namespaceElements(projectElements);
        return contents;
    }

    void export(String project, Collection<ContentElement> elements, Collection<NamespaceElement> nsElements, OutputStream ostream) throws RepositoryException, IOException {
        JarOutputStream output = new JarOutputStream(ostream);
        ZipEntry ze = new ZipEntry("pom.xml");
        output.putNextEntry(ze);
        exportPOM(project, output);
        output.closeEntry();

        ze = new ZipEntry("src/");
        output.putNextEntry(ze);
        output.closeEntry();
        ze = new ZipEntry("src/main/");
        output.putNextEntry(ze);
        output.closeEntry();
        ze = new ZipEntry("src/main/resources/");
        output.putNextEntry(ze);
        output.closeEntry();

        for (NamespaceElement namespace : nsElements) {
            if(namespace.cnd != null && !namespace.excluded) {
                ze = new ZipEntry("src/main/resources/" + namespace.prefix + ".cnd");
                output.putNextEntry(ze);
                PrintWriter writer = new PrintWriter(output);
                writer.print(namespace.cnd);
                writer.flush();
                output.closeEntry();
            }
        }
        for (ContentElement element : elements) {
            if(!element.excluded) {
                ze = new ZipEntry("src/main/resources/" + element.file);
                output.putNextEntry(ze);
                xmlexport.export(element.getCurrent(), output, paths);
                output.closeEntry();
            }
        }

        output.close();
    }

    void export(String project, Collection<ContentElement> elements, Collection<NamespaceElement> nsElements, File basedir) throws IOException, RepositoryException {
        File pom = new File(basedir, "pom.xml");
        if (pom.createNewFile()) {
            FileOutputStream ostream = new FileOutputStream(pom);
            exportPOM(project, ostream);
            ostream.close();
        }
        File resources = new File(basedir, "src" + File.separator + "main" + File.separator + "resources");
        if (!resources.isDirectory()) {
            resources.mkdirs();
        }
        for (NamespaceElement namespace : nsElements) {
            if(namespace.cnd != null && !namespace.excluded) {
                File file = new File(resources, namespace.prefix + ".cnd");
                FileOutputStream ostream = new FileOutputStream(file);
                PrintWriter writer = new PrintWriter(ostream);
                writer.print(namespace.cnd);
                writer.flush();
                ostream.close();
            }
        }
        for (ContentElement element : elements) {
            if(!element.excluded) {
                File file = new File(resources, element.file);
                if (file.exists()) {
                    xmlexport.export(element.getCurrent(), file, paths);
                } else {
                    xmlexport.export(element.getCurrent(), new FileOutputStream(file), paths);
                }
            }
        }
    }
}
