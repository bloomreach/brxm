/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.cms.blog;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.jsoup.Jsoup;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

public class BlogImporterJob implements RepositoryJob {

    private static final Logger log = LoggerFactory.getLogger(BlogImporterJob.class);
    public static final String BLOGS_BASE_PATH = "blogsBasePath";
    public static final String URLS = "urls";
    public static final String AUTHORS_BASE_PATH = "authorsBasePath";
    public static final String AUTHORS = "authors";
    public static final String PROJECT_NAMESPACE = "projectNamespace";
    public static final String MAX_DESCRIPTION_LENGTH = "maxDescriptionLength";
    private static final int DEFAULT_MAX_DESCRIPTION_LENGTH = 200;
    private static final Pattern PATH_PATTERN = Pattern.compile("/");
    private static final String DOCUMENT_TYPE_AUTHOR = "author";
    private static final String DOCUMENT_TYPE_BLOGPOST = "blogpost";
    public static final char SPLITTER = '|';
    public static final String DELIMITER = "|";
    private static final String USER_ADMIN = "admin";

    @Override
    public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
        final Session jcrSession = context.createSystemSession();
        if (jcrSession == null) {
            log.error("Error in getting session");
            return;
        }

        try {
            log.info("+---------------------------------------------------+");
            log.info("|          Start importing blogs                    |");
            log.info("+---------------------------------------------------+");

            final String blogBasePath = context.getAttribute(BLOGS_BASE_PATH);
            final String urlsAttribute = context.getAttribute(URLS);
            final String authorsAttribute = context.getAttribute(AUTHORS);
            final String[] urls = extractArray(urlsAttribute);

            cleanupUrls(urls);
            if (urls.length == 0) {
                log.warn("There are no valid URL configured to import");
                return;
            }
            final String authorsBasePath = context.getAttribute(AUTHORS_BASE_PATH);
            final String projectNamespace = context.getAttribute(PROJECT_NAMESPACE);
            final String[] authors = extractArray(authorsAttribute);
            int maxDescriptionLength;
            try {
                maxDescriptionLength = Integer.parseInt(context.getAttribute(MAX_DESCRIPTION_LENGTH));

            } catch (NumberFormatException e) {
                maxDescriptionLength = DEFAULT_MAX_DESCRIPTION_LENGTH;
            }
            if (blogBasePath != null) {
                importBlogs(jcrSession, projectNamespace, blogBasePath, urls, authorsBasePath, authors, maxDescriptionLength);
            } else {
                log.warn("Import path variable not defined (base path for importing blogs): {}", BLOGS_BASE_PATH);
            }

            log.info("+----------------------------------------------------+");
            log.info("|           Finished importing blogs                 |");
            log.info("+----------------------------------------------------+");
        } finally {
            jcrSession.logout();
        }
    }

    private String[] extractArray(final String value) {
        final String[] retValue;
        if (Strings.isNullOrEmpty(value)) {
            retValue = ArrayUtils.EMPTY_STRING_ARRAY;
        } else{
            final Iterator<String> iterator = Splitter.on(SPLITTER).omitEmptyStrings().split(value).iterator();
            final List<String> strings = Lists.newArrayList(iterator);
            retValue = strings.toArray(new String[strings.size()]);
        }
        return retValue;
    }

    private String[] cleanupUrls(final String[] urls) {
        final List<String> urlList = new ArrayList<>();
        for (String url : urls) {
            if (Strings.isNullOrEmpty(url)) {
                continue;
            }
            urlList.add(url);
        }

        return urlList.toArray(new String[urlList.size()]);
    }

    private void importBlogs(Session session, String projectNamespace, final String blogsBasePath,
                             final String[] blogUrls, final String authorsBasePath, final String[] authors, int maxDescriptionLength) {
        String myBlogsBasePath = blogsBasePath;
        String myAuthorsBasePath = authorsBasePath;
        try {
            if (myBlogsBasePath.startsWith("/")) {
                myBlogsBasePath = myBlogsBasePath.substring(1);
            }
            createBasePath(session, myBlogsBasePath);
            final Node blogNode = session.getRootNode().getNode(myBlogsBasePath);

            final String prefixedNamespace = projectNamespace + ':';
            final String fullNameProperty = prefixedNamespace + "fullname";
            final List<String> documentMixins = getDocumenttypeMixins(session, projectNamespace, DOCUMENT_TYPE_AUTHOR);

            for (int i = 0; i < blogUrls.length; i++) {
                Node authorNode = null;
                if (myAuthorsBasePath != null && authors != null) {
                    if (myAuthorsBasePath.startsWith("/")) {
                        myAuthorsBasePath = myAuthorsBasePath.substring(1);
                    }
                    final String author = authors[i];
                    try {

                        final Node rootNode = session.getRootNode();
                        if (rootNode.hasNode(myAuthorsBasePath)) {
                            Node authorsNode = rootNode.getNode(myAuthorsBasePath);
                            // check if author node exists otherwise create one:
                            if (authorsNode.hasNode(author)) {

                                authorNode = authorsNode.getNode(author);
                            } else {
                                // create author node;
                                log.info("Creating new Author document for name: {}", author);
                                final Node documentNode = createDocument(prefixedNamespace, DOCUMENT_TYPE_AUTHOR, authorsNode, author, documentMixins);
                                setDefaultDocumentProperties(prefixedNamespace, documentNode, Calendar.getInstance());
                                documentNode.setProperty(fullNameProperty, author);
                                authorNode = authorsNode.getNode(author);
                                session.save();
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error(MessageFormat.format("Error finding author document for {0}", author), e);
                        cleanupSession(session);
                    }
                }

                final String url = blogUrls[i];
                log.info("Starting feed import for url {}", url);
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = null;
                try {
                    feed = input.build(new XmlReader(new URL(url)));
                } catch (MalformedURLException mExp) {
                    log.error(MessageFormat.format("Blog URL is malformed  URL is {0}", url), mExp);
                } catch (FeedException | IOException fExp) {
                    log.error(MessageFormat.format("Error in parsing feed {0}", url), fExp);
                }
                processFeed(session, projectNamespace, maxDescriptionLength, blogNode, authorNode, feed);
            }
        } catch (RepositoryException rExp) {
            log.error("Error in getting base folder for blogs", rExp);
            cleanupSession(session);
        }
    }

    private void createBasePath(final Session session, final String myBlogsBasePath) throws RepositoryException {
        if (!session.getRootNode().hasNode(myBlogsBasePath)) {
            log.warn("Blog base path ({}) is missing, attempting to create it", myBlogsBasePath);
            Node node = session.getRootNode();
            for (String path : PATH_PATTERN.split(myBlogsBasePath)) {
                if (!node.hasNode(path)) {
                    createBlogFolder(node, path);
                    session.save();
                }
                node = node.getNode(path);
            }
        }
    }

    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    private void cleanupSession(final Session session) {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("Error refreshing session", e);
        }
    }

    private void processFeed(final Session session, final String projectNamespace, final int maxDescriptionLength, final Node blogNode, final Node authorNode, final SyndFeed feed) {
        if (feed != null) {
            final List<String> documentMixins = getDocumenttypeMixins(session, projectNamespace, DOCUMENT_TYPE_BLOGPOST);
            for (Object entry : feed.getEntries()) {
                if (entry instanceof SyndEntry) {
                    SyndEntry syndEntry = (SyndEntry) entry;
                    try {
                        if (!blogExists(blogNode, syndEntry)) {
                            createBlogDocument(projectNamespace, blogNode, authorNode, syndEntry, maxDescriptionLength, documentMixins);
                            BlogUpdater.handleSaved(blogNode, projectNamespace);
                            session.save();
                        } else {
                            log.info("Blogpost already exists for node: {}", blogNode.getPath());
                        }
                    } catch (RepositoryException rExp) {
                        log.error("Error in saving blog document", rExp);
                        cleanupSession(session);
                    }
                }
            }
        } else {
            log.debug("Feed was null");
        }
    }

    private List<String> getDocumenttypeMixins(final Session session, final String projectNamespace, final String documentTypeName) {
        final List<String> mixins = new ArrayList<>();
        final String docNamespacePrototype = "hippo:namespaces/" + projectNamespace + "/" + documentTypeName + "/hipposysedit:prototypes/hipposysedit:prototype";
        try {
            final Node docNamespacePrototypeNode = session.getRootNode().getNode(docNamespacePrototype);
            final NodeType[] mixinNodeTypes = docNamespacePrototypeNode.getMixinNodeTypes();
            for(NodeType nt : mixinNodeTypes) {
                mixins.add(nt.getName());
            }
        } catch (RepositoryException rExp) {
            log.error("Error in retrieving document namespace prototype", rExp);
        }

        return mixins;
    }

    private boolean blogExists(Node baseNode, SyndEntry syndEntry) throws RepositoryException {
        Node blogFolder = getBlogFolder(baseNode, syndEntry);
        String documentName = NodeNameCodec.encode(syndEntry.getTitle().replace("?", ""), true);
        final boolean exist = blogFolder.hasNode(documentName);
        if (exist) {
            log.info("Blog folder {} already has document with name: {}", blogFolder.getPath(), documentName);
        }
        return exist;
    }

    private boolean createBlogDocument(final String namespace, final Node baseNode, final Node authorHandleNode,
                                       final SyndEntry syndEntry, final int maxDescriptionLength,
                                       final List<String> mixins) throws RepositoryException {
        final String prefixedNamespace = namespace + ':';
        Node blogFolder = getBlogFolder(baseNode, syndEntry);
        String documentName = NodeNameCodec.encode(syndEntry.getTitle(), true).replace("?", "");
        Node documentNode = createDocument(prefixedNamespace, DOCUMENT_TYPE_BLOGPOST, blogFolder, documentName, mixins);
        documentNode.setProperty(prefixedNamespace + "title", syndEntry.getTitle());
        documentNode.setProperty(prefixedNamespace + "introduction", processDescription(syndEntry, maxDescriptionLength));
        if (authorHandleNode != null) {
            link(documentNode, prefixedNamespace + AUTHORS, authorHandleNode);
            final Node authorNode = authorHandleNode.getNode(authorHandleNode.getName());
            final Property nameProperty = authorNode.getProperty(prefixedNamespace + "fullname");
            final String name = nameProperty.getString();
            documentNode.setProperty(prefixedNamespace + DOCUMENT_TYPE_AUTHOR, name);
            documentNode.setProperty(prefixedNamespace + "authornames", new String[]{name});
        } else {
            final String author = syndEntry.getAuthor();
            documentNode.setProperty(prefixedNamespace + "author", author);
            documentNode.setProperty(prefixedNamespace + "authornames", new String[]{author});
        }
        documentNode.setProperty(prefixedNamespace + "link", syndEntry.getLink());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(syndEntry.getPublishedDate());
        setDefaultDocumentProperties(prefixedNamespace, documentNode, calendar);
        documentNode.addNode(prefixedNamespace + "content", "hippostd:html");
        documentNode.getNode(prefixedNamespace + "content").setProperty("hippostd:content", processContent(syndEntry));
        documentNode.addNode(prefixedNamespace + "image", "hippostd:html");
        documentNode.getNode(prefixedNamespace + "image").setProperty("hippostd:content", "");
/*
// TODO check this: project specific
        setEmptyLinkProperty(documentNode, projectNamespace + "documentlink");
        setEmptyLinkProperty(documentNode, projectNamespace + "downloadlink");
        setEmptyLinkProperty(documentNode, projectNamespace + "relatedboxdocument");

*/
        return true;
    }

    private void setDefaultDocumentProperties(final String prefixedNamespace, final Node documentNode, final Calendar calendar) throws RepositoryException {
        documentNode.setProperty(prefixedNamespace + "publicationdate", calendar);
        documentNode.setProperty("hippostdpubwf:lastModifiedBy", USER_ADMIN);
        documentNode.setProperty("hippostdpubwf:createdBy", USER_ADMIN);
        calendar.setTime(new Date());
        documentNode.setProperty("hippostdpubwf:lastModificationDate", calendar);
        documentNode.setProperty("hippostdpubwf:creationDate", calendar);
        documentNode.setProperty("hippostd:stateSummary", "preview");
        documentNode.setProperty("hippostd:state", "published");
        documentNode.setProperty("hippostd:holder", USER_ADMIN);
        // TODO make locale dynamic
        documentNode.setProperty("hippotranslation:locale", "en");
        documentNode.setProperty("hippotranslation:id", UUID.randomUUID().toString());
    }

    private Node createDocument(final String prefixedNamespace, final String docType, final Node rootNode,
                                final String documentName, final List<String> mixins) throws RepositoryException {
        Node handleNode = rootNode.addNode(documentName, "hippo:handle");
        handleNode.addMixin("mix:referenceable");
        Node documentNode = handleNode.addNode(documentName, prefixedNamespace + docType);
        for (String mixin : mixins) {
            documentNode.addMixin(mixin);
        }
        documentNode.setProperty("hippo:availability", new String[]{"live", "preview"});
        return documentNode;
    }

    private void link(final Node source, final String name, final Node target) throws RepositoryException {
        final Node link = source.addNode(name, "hippo:mirror");
        link.setProperty("hippo:docbase", target.getIdentifier());
    }

    /*
    private void setEmptyLinkProperty(Node documentNode, String propertyName) throws RepositoryException {
        documentNode.addNode(propertyName, propertyName);
        documentNode.getNode(propertyName).setProperty("hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe");
        documentNode.getNode(propertyName).setProperty("hippo:facets", ArrayUtils.EMPTY_STRING_ARRAY);
        documentNode.getNode(propertyName).setProperty("hippo:modes", ArrayUtils.EMPTY_STRING_ARRAY);
        documentNode.getNode(propertyName).setProperty("hippo:values", ArrayUtils.EMPTY_STRING_ARRAY);
    }
    */

    private String processContent(SyndEntry entry) {
        List<?> contents = entry.getContents();
        if (contents != null && !contents.isEmpty()) {
            StringBuilder blogContent = new StringBuilder();
            for (Object contentObject : contents) {
                SyndContent content = (SyndContent) contentObject;
                if (content != null && !Strings.isNullOrEmpty(content.getValue())) {
                    blogContent.append(content.getValue());
                }
            }
            return blogContent.toString();
        } else {
            SyndContent description = entry.getDescription();
            if (description != null && !Strings.isNullOrEmpty(description.getValue())) {
                return description.getValue();
            }
        }
        return "";
    }

    private String processDescription(SyndEntry entry, int maxDescriptionLength) {
        SyndContent description = entry.getDescription();
        if (description != null && !Strings.isNullOrEmpty(description.getValue())) {
            String text = Jsoup.parse(description.getValue()).text();
            if (text.length() > maxDescriptionLength) {
                return text.substring(0, maxDescriptionLength) + "...";
            } else {
                return text;
            }
        } else {
            List<?> contents = entry.getContents();
            if (contents != null && !contents.isEmpty()) {
                StringBuilder blogContent = new StringBuilder();
                for (Object contentObject : contents) {
                    SyndContent content = (SyndContent) contentObject;
                    if (content != null && !Strings.isNullOrEmpty(content.getValue())) {
                        blogContent.append(Jsoup.parse(content.getValue()).text());
                    }
                }
                if (blogContent.toString().length() > maxDescriptionLength) {
                    return blogContent.toString().substring(0, maxDescriptionLength) + "...";
                } else {
                    return blogContent.toString();
                }
            } else {
                return "";
            }
        }
    }

    private Node getBlogFolder(Node blogFolder, SyndEntry syndEntry) throws RepositoryException {

        Date blogDate = syndEntry.getPublishedDate();
        if (blogDate == null) {
            blogDate = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(blogDate);

        String timePart = String.valueOf(calendar.get(Calendar.YEAR));
        Node baseNode;
        if (blogFolder.hasNode(timePart)) {
            baseNode = blogFolder.getNode(timePart);
        } else {
            baseNode = createBlogFolder(blogFolder, timePart);
        }

        timePart = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        if (timePart.length() == 1) {
            timePart = '0' + timePart;
        }
        if (baseNode.hasNode(timePart)) {
            baseNode = baseNode.getNode(timePart);
        } else {
            baseNode = createBlogFolder(baseNode, timePart);
        }
        return baseNode;

    }

    private Node createBlogFolder(Node node, String name) throws RepositoryException {
        Node blogFolder = node.addNode(name, "hippostd:folder");
        blogFolder.addMixin("mix:referenceable");
        blogFolder.setProperty("hippostd:foldertype", new String[]{"new-blog-folder", "new-blog-document"});
        return blogFolder;
    }

}
