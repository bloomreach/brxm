/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.quartz.JCRScheduler;
import org.jsoup.Jsoup;
import org.onehippo.forge.utilities.repository.scheduler.JobConfiguration;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @version "$Id$"
 */
public class BlogImporterJob implements InterruptableJob {

    private final static Logger log = LoggerFactory.getLogger(BlogImporterJob.class);
    private static final String BLOGS_BASE_PATH = "blogsBasePath";
    private static final String URLS = "urls";
    private static final String AUTHORS_BASE_PATH = "authorsBasePath";
    private static final String AUTHORS = "authors";
    private static final String PROJECT_NAMESPACE = "projectNamespace";
    private static final String MAX_DESCRIPTION_LENGTH = "maxDescriptionLength";
    private static final Pattern PATH_PATTERN = Pattern.compile("/");


    @Override
    public void interrupt() throws UnableToInterruptJobException {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("+---------------------------------------------------+");
        log.info("|          Start importing blogs                    |");
        log.info("+---------------------------------------------------+");
        JCRScheduler jcrScheduler = (JCRScheduler) context.getScheduler();

        // get session from jcrScheduling context!
        final Session jcrSession = jcrScheduler.getJCRSchedulingContext().getSession();
        if (jcrSession == null) {
            log.error("Error in getting session");
            return;
        }

        final JobConfiguration jobConfiguration = (JobConfiguration) context.getMergedJobDataMap().get(JobConfiguration.class.getName());
        final String blogBasePath = jobConfiguration.getString(BLOGS_BASE_PATH);
        final String[] urls = jobConfiguration.getStrings(URLS, ArrayUtils.EMPTY_STRING_ARRAY);
        final String authorsBasePath = jobConfiguration.getString(AUTHORS_BASE_PATH, null);
        final String projectNamespace = jobConfiguration.getString(PROJECT_NAMESPACE, null);
        final String[] authors = jobConfiguration.getStrings(AUTHORS, null);
        int maxDescriptionLength;
        try {
            maxDescriptionLength = Integer.parseInt(jobConfiguration.getString(MAX_DESCRIPTION_LENGTH));

        } catch (NumberFormatException e) {
            maxDescriptionLength = -1;
        }
        if (urls != null && urls.length > 0 && blogBasePath != null && maxDescriptionLength > -1) {
            importBlogs(jcrSession, projectNamespace, blogBasePath, urls, authorsBasePath, authors, maxDescriptionLength);
        }

        log.info("+----------------------------------------------------+");
        log.info("|           Finished importing blogs                 |");
        log.info("+----------------------------------------------------+");
    }

    private void importBlogs(Session session, String projectNamespace, String blogsBasePath, String[] blogUrls, String authorsBasePath, String[] authors, int maxDescriptionLength) {
        try {
            if (blogsBasePath.startsWith("/")) {
                blogsBasePath = blogsBasePath.substring(1);
            }
            if (!session.getRootNode().hasNode(blogsBasePath)) {
                log.warn("Blog base path (" + blogsBasePath + ") is missing, attempting to create it");
                Node node = session.getRootNode();
                for (String path : PATH_PATTERN.split(blogsBasePath)) {
                    if (!node.hasNode(path)) {
                        createBlogFolder(node, path);
                        session.save();
                    }
                    node = node.getNode(path);
                }
            }
            final Node blogNode = session.getRootNode().getNode(blogsBasePath);

            for (int i = 0; i < blogUrls.length; i++) {
                Node authorNode = null;
                if (authorsBasePath != null && authors != null) {
                    if (authorsBasePath.startsWith("/")) {
                        authorsBasePath = authorsBasePath.substring(1);
                    }
                    final String author = authors[i];
                    try {
                        Node authorsNode = session.getRootNode().getNode(authorsBasePath);
                        authorNode = authorsNode.getNode(author);
                    } catch (RepositoryException e) {
                        log.error(MessageFormat.format("Error finding author document for {0}", author), e);
                    }
                }

                final String url = blogUrls[i];
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = null;
                try {
                    feed = input.build(new XmlReader(new URL(url)));
                } catch (MalformedURLException mExp) {
                    log.error(MessageFormat.format("Blog URL is malformed  URL is {0}", url), mExp);
                } catch (FeedException | IOException fExp) {
                    log.error(MessageFormat.format("Error in parsing feed {0}", url), fExp);
                }
                if (feed != null) {
                    for (Object entry : feed.getEntries()) {
                        if (entry instanceof SyndEntry) {
                            SyndEntry syndEntry = (SyndEntry) entry;
                            try {
                                if (!blogExists(blogNode, syndEntry)) {
                                    createBlogDocument(projectNamespace, blogNode, authorNode, syndEntry, maxDescriptionLength);
                                    BlogUpdater.handleSaved(blogNode, projectNamespace);
                                    session.save();
                                }
                            } catch (RepositoryException rExp) {
                                log.error("Error in saving blog document", rExp);
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException rExp) {
            log.error("Error in getting base folder for blogs", rExp);
        }
    }

    private boolean blogExists(Node baseNode, SyndEntry syndEntry) throws RepositoryException {
        Node blogFolder = getBlogFolder(baseNode, syndEntry);
        String documentName = NodeNameCodec.encode(syndEntry.getTitle().replace("?", ""), true);
        return blogFolder.hasNode(documentName);
    }

    private boolean createBlogDocument(final String projectNamespace, Node baseNode, Node authorHandleNode, SyndEntry syndEntry, int maxDescriptionLength) throws RepositoryException {
        Node blogFolder = getBlogFolder(baseNode, syndEntry);
        String documentName = NodeNameCodec.encode(syndEntry.getTitle(), true).replace("?", "");
        Node handleNode = blogFolder.addNode(documentName, "hippo:handle");
        handleNode.addMixin("hippo:hardhandle");
        Node documentNode = handleNode.addNode(documentName, projectNamespace + "blogpost");
        documentNode.addMixin("hippo:harddocument");
        documentNode.setProperty("hippo:availability", new String[]{"live", "preview"});
        documentNode.setProperty(projectNamespace + "title", syndEntry.getTitle());
        documentNode.setProperty(projectNamespace + "introduction", processDescription(syndEntry, maxDescriptionLength));
        if (authorHandleNode != null) {
            link(documentNode, projectNamespace + "authors", authorHandleNode);
            final Node authorNode = authorHandleNode.getNode(authorHandleNode.getName());
            final Property nameProperty = authorNode.getProperty(projectNamespace + "title");
            final String name = nameProperty.getString();
            documentNode.setProperty(projectNamespace + "author", name);
            documentNode.setProperty(projectNamespace + "authornames", name);
        } else {
            documentNode.setProperty(projectNamespace + "author", syndEntry.getAuthor());
        }
        documentNode.setProperty(projectNamespace + "link", syndEntry.getLink());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(syndEntry.getPublishedDate());
        documentNode.setProperty(projectNamespace + "publicationdate", calendar);
        documentNode.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        documentNode.setProperty("hippostdpubwf:createdBy", "admin");
        calendar.setTime(new Date());
        documentNode.setProperty("hippostdpubwf:lastModificationDate", calendar);
        documentNode.setProperty("hippostdpubwf:creationDate", calendar);
        documentNode.setProperty("hippostd:stateSummary", "preview");
        documentNode.setProperty("hippostd:state", "published");
        documentNode.setProperty("hippostd:holder", "admin");
        documentNode.addNode(projectNamespace + "body", "hippostd:html");
        documentNode.getNode(projectNamespace + "body").setProperty("hippostd:content", processContent(syndEntry));
        documentNode.addNode(projectNamespace + "image", "hippostd:html");
        documentNode.getNode(projectNamespace + "image").setProperty("hippostd:content", "");
/*
// TODO check this: project specific
        setEmptyLinkProperty(documentNode, projectNamespace + "documentlink");
        setEmptyLinkProperty(documentNode, projectNamespace + "downloadlink");
        setEmptyLinkProperty(documentNode, projectNamespace + "relatedboxdocument");

*/
        return true;
    }

    private void link(final Node source, final String name, final Node target) throws RepositoryException {
        final Node link = source.addNode(name, "hippo:mirror");
        link.setProperty("hippo:docbase", target.getIdentifier());
    }

    private void setEmptyLinkProperty(Node documentNode, String propertyName) throws RepositoryException {
        documentNode.addNode(propertyName, propertyName);
        documentNode.getNode(propertyName).setProperty("hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe");
        documentNode.getNode(propertyName).setProperty("hippo:facets", ArrayUtils.EMPTY_STRING_ARRAY);
        documentNode.getNode(propertyName).setProperty("hippo:modes", ArrayUtils.EMPTY_STRING_ARRAY);
        documentNode.getNode(propertyName).setProperty("hippo:values", ArrayUtils.EMPTY_STRING_ARRAY);
    }

    private String processContent(SyndEntry entry) {
        List<?> contents = entry.getContents();
        if (contents != null && contents.size() > 0) {
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
            if (contents != null && contents.size() > 0) {
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
        blogFolder.addMixin("hippo:harddocument");
        blogFolder.setProperty("hippostd:foldertype", new String[]{"new-blog-folder", "new-blog-document"});
        return blogFolder;
    }
}
