package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: HippoNodeUtils.java 169724 2013-07-05 08:32:08Z dvandiepen $"
 */
public final class HippoNodeUtils {

    public static final String HIPPOSYSEDIT_PATH = HippoNodeType.HIPPO_PATH;
    private static final Predicate<String> INTERNAL_TYPES_PREDICATE = new Predicate<String>() {
        @Override
        public boolean apply(String documentType) {
            if (!documentType.startsWith("hippo:")
                    && !documentType.startsWith("hipposys:")
                    && !documentType.startsWith("hipposysedit:")
                    && !documentType.startsWith("reporting:")
                    && !documentType.equals("nt:unstructured")
                    && !documentType.startsWith("hippogallery:")) {
                return true;
            }
            return false;
        }
    };
    private static final Predicate<String> NAMESPACE_PREDICATE = new Predicate<String>() {
        @Override
        public boolean apply(String namespace) {
            return EssentialConst.HIPPO_BUILT_IN_NAMESPACES.contains(namespace);
        }
    };
    private static Logger log = LoggerFactory.getLogger(HippoNodeUtils.class);

    private HippoNodeUtils() {
    }

    public static List<String> getProjectNamespaces(final Session session) {
        try {
            final Node rootNode = session.getRootNode();
            final Node namespace = rootNode.getNode("hippo:namespaces");
            final NodeIterator nodes = namespace.getNodes();
            final Collection<String> namespaceNames = new HashSet<>();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final String name = node.getName();
                namespaceNames.add(name);
            }
            return ImmutableList.copyOf(Iterables.filter(namespaceNames, Predicates.not(NAMESPACE_PREDICATE)));
        } catch (RepositoryException e) {
            log.error("Error fetching namespaces", e);
        }
        return Collections.emptyList();

    }

    public static void setSupertype(final Node namespaceNode, final Collection<String> values) throws RepositoryException {
        Node node = getSupertypeNode(namespaceNode);
        final String[] array = values.toArray(new String[values.size()]);
        node.setProperty("hipposysedit:supertype", array);
    }

    public static void setSupertype(final Node namespaceNode, final String... values) throws RepositoryException {
        Node node = getSupertypeNode(namespaceNode);
        node.setProperty("hipposysedit:supertype", values);
    }

    public static void setUri(final Node namespaceNode, final String uri) throws RepositoryException {
        Node node = getSupertypeNode(namespaceNode);
        node.setProperty("hipposysedit:uri", uri);
    }

    public static void setNodeType(final Node namespaceNode, final String value) throws RepositoryException {
        Node node = getPrototypeNode(namespaceNode);
        node.setPrimaryType(value);
    }

    public static String getStringProperty(final Node node, final String property) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property).getString();
        } else {
            return null;
        }
    }

    public static Long getLongProperty(final Node node, final String property, final Long defaultValue) throws RepositoryException {
        final Long value = getLongProperty(node, property);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static Long getLongProperty(final Node node, final String property) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property).getLong();
        } else {
            return null;
        }
    }

    public static Node getNode(final Session session, final String path) throws RepositoryException {
        if (session.nodeExists(path)) {
            return session.getNode(path);
        }
        return null;
    }

    public static Node retrieveExistingNodeOrCreateCopy(final Session session, final String path, final String pathToCopy) throws RepositoryException {
        if (session.nodeExists(path)) {
            return session.getNode(path);
        }

        return copyNode(session, pathToCopy, path);
    }

    public static Node retrieveExistingNodeOrCreateCopy(final Session session, final String path, final Node nodeToCopy) throws RepositoryException {
        if (session.nodeExists(path)) {
            return session.getNode(path);
        }

        return copyNode(session, nodeToCopy, path);
    }

    /**
     * Copy the node at the source path to the destination. When there exists no node at the source path, null will be
     * returned.
     *
     * @param session     the JCR session
     * @param source      the absolute path to the node to copy
     * @param destination the absolute path to copy node to
     * @return the copied destination node or null when source is not available
     * @throws RepositoryException generic repository exception
     */
    public static Node copyNode(final Session session, final String source, final String destination) throws RepositoryException {
        if (!session.nodeExists(source)) {
            return null;
        }

        return copyNode(session, session.getNode(source), destination);
    }

    /**
     * Copy the source node to the destination. When the source node is null, null will be returned.
     *
     * @param session     the JCR session
     * @param source      the node to copy
     * @param destination the absolute path to copy node to
     * @return the copied destination node or null when source is not available
     * @throws RepositoryException generic repository exception
     */
    public static Node copyNode(final Session session, final Node source, final String destination) throws RepositoryException {
        if (source == null) {
            return null;
        }
        final HippoSession hs = (HippoSession) session;
        return hs.copy(source, destination);
    }

    /**
     * @param node
     * @param child
     * @throws RepositoryException
     */
    public static void removeChildNode(final Node node, final String child) throws RepositoryException {
        if (node == null) {
            return;
        }

        // TODO check for multiple deletes?
        if (node.hasNode(child)) {
            final Node childNode = node.getNode(child);
            childNode.remove();
        }
    }

    /**
     * Retrieve the namespace prefix from a prefixed node type. This method will return null when the type is not
     * prefixed.
     *
     * @param type the namespace type, e.g. 'hippo:type'
     * @return the namespace prefix or null, e.g. 'hippo'
     */
    public static String getPrefixFromType(final String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        final int i = type.indexOf(':');
        if (i < 0) {
            return null;
        }
        return type.substring(0, i);
    }

    /**
     * Retrieve the namespace from a prefixed node type. This method will return null when the type empty. When the type
     * is not emtpy, but not prefixed as well, it will return the original type.
     *
     * @param type the namespace type, e.g. 'hippo:type'
     * @return the non prefixed name or null, e.g. 'type'
     */
    public static String getNameFromType(final String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        final int i = type.indexOf(':');
        if (i < 0) {
            return type;
        }
        return type.substring(i + 1);
    }

    //############################################
    // UTIL
    //############################################

    private static Node getPrototypeNode(final Node namespaceNode) throws RepositoryException {
        return namespaceNode.getNode("hipposysedit:prototypes").getNode("hipposysedit:prototype");
    }

    private static Node getSupertypeNode(final Node namespaceNode) throws RepositoryException {
        return namespaceNode.getNode("hipposysedit:nodetype").getNode("hipposysedit:nodetype");
    }

    private static Map<String, Set<String>> prototypes(final Session session, final String... templates) throws RepositoryException {
        Map<String, Set<String>> types = new LinkedHashMap<>();
        try {
            QueryManager qmgr = session.getWorkspace().getQueryManager();
            AbstractList<Node> foldertypes = new Vector<>();
            Node hippoTemplates = session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
            for (String template : templates) {
                if (hippoTemplates.hasNode(template)) {
                    foldertypes.add(hippoTemplates.getNode(template));
                }
            }
            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage()); // HREPTWO-1266
                        QueryResult rs = query.execute();
                        for (NodeIterator iter = rs.getNodes(); iter.hasNext(); ) {
                            Node typeNode = iter.nextNode();
                            if (typeNode.getName().equals("hipposysedit:prototype")) {
                                String documentType = typeNode.getPrimaryNodeType().getName();
                                final boolean isTemplate = INTERNAL_TYPES_PREDICATE.apply(documentType);
                                if (isTemplate) {
                                    prototypes.add(documentType);

                                }
                            } else {
                                prototypes.add(typeNode.getName());
                            }
                        }
                    }
                    types.put(foldertype.getName(), prototypes);
                } catch (InvalidQueryException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return types;
    }

    /**
     * Partially ripped from org.hippoecm.repository.standardworkflow.FolderWorkflowImpl#prototypes() to retrieve a list
     * of all used hippo document. Which match to the rule according to the specified implementation of the
     * org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher
     *
     * @param session
     * @param matcher
     * @param templates
     * @return
     * @throws RepositoryException
     */
    private static Map<String, Set<String>> prototypes(final Session session, JcrMatcher matcher, final String... templates) throws RepositoryException {
        Map<String, Set<String>> types = new LinkedHashMap<>();
        if (session == null) {
            // WHEN RUNNING WITHOUT CMS
            log.warn("Session was null, returning empty types");
            return types;
        }
        try {
            QueryManager qmgr = session.getWorkspace().getQueryManager();
            AbstractList<Node> foldertypes = new Vector<>();
            Node hippoTemplates = session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
            for (String template : templates) {
                if (hippoTemplates.hasNode(template)) {
                    foldertypes.add(hippoTemplates.getNode(template));
                }
            }
            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage()); // HREPTWO-1266
                        QueryResult rs = query.execute();
                        for (NodeIterator iter = rs.getNodes(); iter.hasNext(); ) {
                            Node typeNode = iter.nextNode();
                            if (typeNode.getName().equals("hipposysedit:prototype")) {
                                String documentType = typeNode.getPrimaryNodeType().getName();
                                final boolean isTemplate = INTERNAL_TYPES_PREDICATE.apply(documentType);
                                if (isTemplate && (matcher == null || matcher != null && matcher.matches(typeNode))) {
                                    prototypes.add(documentType);
                                }
                            } else {
                                prototypes.add(typeNode.getName());
                            }
                        }
                    }
                    types.put(foldertype.getName(), prototypes);
                } catch (InvalidQueryException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return types;
    }

    /**
     * Similar to the org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils#prototypes(javax.jcr.Session,
     * java.lang.String...) but instead of retrieving document. This method retrieves all available compound types
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public static Set<String> getCompounds(final Session session) throws RepositoryException {
        return getCompounds(session, null);
    }

    /**
     * Similar to the org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils#prototypes(javax.jcr.Session,
     * java.lang.String...) but instead of retrieving document. This method retrieves all available compound types
     *
     * @param session
     * @param matcher
     * @return
     * @throws RepositoryException
     */
    public static Set<String> getCompounds(final Session session, final JcrMatcher matcher) throws RepositoryException {
        String query = "//element(*,hipposysedit:namespacefolder)/element(*,mix:referenceable)/element(*,hipposysedit:templatetype)/hipposysedit:prototypes/element(hipposysedit:prototype,hippo:compound)";
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        @SuppressWarnings("deprecation")
        final QueryResult queryResult = queryManager.createQuery(query, Query.XPATH).execute();
        Set<String> prototypes = new TreeSet<>();
        for (NodeIterator iter = queryResult.getNodes(); iter.hasNext(); ) {
            Node typeNode = iter.nextNode();
            if (typeNode.getName().equals("hipposysedit:prototype")) {
                String documentType = typeNode.getPrimaryNodeType().getName();
                if (!documentType.startsWith("hippo:") && !documentType.startsWith("hipposys:") && !documentType.startsWith("hipposysedit:") && !documentType.startsWith("reporting:")
                        && !documentType.equals("nt:unstructured") && !documentType.startsWith("hippogallery:") && (matcher != null && matcher.matches(typeNode))) {
                    prototypes.add(documentType);
                }
            } else {
                prototypes.add(typeNode.getName());
            }
        }
        return prototypes;
    }

    /**
     * Similar to the org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils#prototypes(javax.jcr.Session,
     * java.lang.String...) but instead of retrieving document. This method retrieves all available compound types
     *
     * @param session
     * @param matcher
     * @return
     * @throws RepositoryException
     */
    public static Set<String> getContentBlocksProviders(final Session session, final JcrMatcher matcher) throws RepositoryException {
        String query = "//element(*,hipposysedit:namespacefolder)/element(*,mix:referenceable)/element(*,hipposysedit:templatetype)[@editor:templates/_default_/cbprovider='true']";
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        @SuppressWarnings("deprecation")
        final QueryResult queryResult = queryManager.createQuery(query, Query.XPATH).execute();
        Set<String> prototypes = new TreeSet<>();
        for (NodeIterator iter = queryResult.getNodes(); iter.hasNext(); ) {
            Node typeNode = iter.nextNode();

            if (typeNode.getName().equals("")&&typeNode.getParent().getName().equals("hipposysedit:prototype")) {
                String documentType = typeNode.getPrimaryNodeType().getName();
                if (!documentType.startsWith("hippo:") && !documentType.startsWith("hipposys:") && !documentType.startsWith("hipposysedit:") && !documentType.startsWith("reporting:")
                        && !documentType.equals("nt:unstructured") && !documentType.startsWith("hippogallery:") && (matcher != null && matcher.matches(typeNode))) {
                    prototypes.add(documentType);
                }
            } else {
                prototypes.add(typeNode.getName());
            }
        }
        return prototypes;
    }

    /**
     * Retrieves a list of available primary types which are retrieved with the #prototype method
     *
     * @param session
     * @param templates
     * @return
     * @throws RepositoryException
     */
    public static List<String> getPrimaryTypes(final Session session, final String... templates) throws RepositoryException {
        final Map<String, Set<String>> prototypes = prototypes(session, null, templates);
        final List<String> set = new ArrayList<>();
        final Collection<Set<String>> values = prototypes.values();
        for (Set<String> collection : values) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * Retrieves a list of available primary types which are retrieved with the #prototype method  and filtererd with
     * the org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher implementation
     *
     * @param session
     * @param templates
     * @return
     * @throws RepositoryException
     */
    public static List<String> getPrimaryTypes(final Session session, final JcrMatcher matcher, final String... templates) throws RepositoryException {
        final Map<String, Set<String>> prototypes = prototypes(session, matcher, templates);
        final List<String> set = new ArrayList<>();
        final Collection<Set<String>> values = prototypes.values();
        for (Set<String> collection : values) {
            set.addAll(collection);
        }
        return set;
    }


    public static String getDisplayValue(final Session session, final String type) throws RepositoryException {
        String name = type;
        final String resolvedPath = resolvePath(type);
        if (session.itemExists(resolvedPath)) {
            Node node = session.getNode(resolvedPath);
            try {
                name = NodeNameCodec.decode(node.getName());
                if (node.isNodeType("hippo:translated")) {
                    Locale locale = Locale.getDefault();
                    NodeIterator nodes = node.getNodes("hippo:translation");
                    while (nodes.hasNext()) {
                        Node child = nodes.nextNode();
                        if (child.isNodeType("hippo:translation") && !child.hasProperty("hippo:property")) {
                            String language = child.getProperty("hippo:language").getString();
                            if (locale.getLanguage().equals(language)) {
                                return child.getProperty("hippo:message").getString();
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return name;
    }

    public static String resolvePath(String type) {
        if (type.contains(":")) {
            return "/hippo:namespaces/" + type.replace(':', '/');
        } else {
            return "/hippo:namespaces/system/" + type;
        }
    }


    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^http:.*/[0-9].[0-9]$");


    public static void checkName(String name) throws Exception {
        if (name == null || "".equals(name)) {
            throw new Exception("No name specified");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new Exception("Invalid name; only alphabetic characters allowed in lower- or uppercase");
        }
    }

    public static void checkURI(String name) throws Exception {
        if (name == null) {
            throw new Exception("No URI specified");
        }
        if (!URL_PATTERN.matcher(name).matches()) {
            throw new Exception("Invalid URL; ");
        }
    }


}
