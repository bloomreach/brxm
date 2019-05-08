/*
 * Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.poll.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;

import com.google.common.base.Strings;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.forge.poll.bean.PollVotesBean;
import org.onehippo.forge.poll.contentbean.PollDocument;
import org.onehippo.forge.poll.contentbean.compound.Option;
import org.onehippo.forge.poll.contentbean.compound.Poll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider that can be instantiated within a component to add poll functionality by composition.
 * <p>
 * The provider retrieves a poll document as configured by component parameter 'poll-docsPath'.
 * The type of document bean that is retrieved is determined by the parameter 'poll-docClass' and defaults to
 * "org.onehippo.forge.poll.contentbean.PollDocument".
 * <p>
 * If the 'poll-docsPath' points to a poll document of the correct class, it returns that document directly.
 * <p>
 * If the 'poll-docsPath' points to a folder, it uses that folder as scope to search for documents that are of the
 * correct class. It then takes the first of these documents and returns it.
 */
public class PollProvider {

    public static final String POLL_DOCUMENT = "pollDocument";
    public static final String POLL_DOCUMENT_PATH = "path";
    public static final String POLL_OPTION = "option";
    public static final String POLL_VOTE_SUCCESS = "voteSuccess";
    public static final String NO_COOKIE_SUPPORT = "pollNoCookies";
    public static final String POLL_VOTES = "pollVotes";

    private static final Logger logger = LoggerFactory.getLogger(PollProvider.class);

    private static final String PARAM_DATA_PATH_DEFAULT = "polldata";
    private static final Class<? extends HippoDocumentBean> PARAM_DOCS_CLASS_DEFAULT = PollDocument.class;
    private static final String PARAM_DOC_POLL_COMPOUND_NAME_DEFAULT = "poll:poll";

    private static final int POLL_COOKIE_MAX_AGE = 3600 * 24 * 365;
    private static final String POLL_COOKIE_NAME_PREFIX = "Hippo.PollProvider.";

    private static final String POLL_DATA_NODE_PREVIEW = "preview";

    // search limit for poll documents, arbitrarily initialized
    private int queryLimit = 100;

    /**
     * Setting for query limit
     */
    @SuppressWarnings("unused")
    public void setQueryLimit(final int queryLimit) {
        this.queryLimit = queryLimit;
    }

    /**
     * Retrieve vote parameters and cast the vote, saving it in repository.
     */
    public void doAction(final HstRequest request,
                         final HstResponse response,
                         final PollComponentInfo pollComponentInfo) throws RepositoryException {

        Session persistableSession = request.getRequestContext().getSession();

        final String optionId = preventXSS(request.getParameter(POLL_OPTION));
        if (optionId == null) {
            logger.warn("Option parameter '" + POLL_OPTION + "' not received");
            response.setRenderParameter(POLL_VOTE_SUCCESS, Boolean.toString(false));
            return;
        }

        final String documentPath = preventXSS(request.getParameter(POLL_DOCUMENT_PATH));
        if (documentPath == null) {
            logger.warn("Document path parameter '" + POLL_DOCUMENT + "' not received");
            response.setRenderParameter(POLL_VOTE_SUCCESS, Boolean.toString(false));
            return;
        }

        // vote already stored?
        final String documentName = documentPath.substring(documentPath.lastIndexOf("/") + 1);
        final String persistedValue = getPersistentValue(request, documentName);
        if (persistedValue != null) {
            logger.warn("Persisted value for poll " + documentPath + " already present");
            response.setRenderParameter(POLL_VOTE_SUCCESS, Boolean.toString(false));
            return;
        }

        boolean success = castVote(request, persistableSession, documentPath, optionId, pollComponentInfo);

        // preserve selected option to make it available during doBeforeRender.
        response.setRenderParameter(POLL_OPTION, optionId);
        response.setRenderParameter(POLL_VOTE_SUCCESS, Boolean.toString(success));
    }

    /***
     * Get the current poll document and put it on request by attribute "pollDocument".
     */
    public void doBeforeRender(final HstRequest request, final HstResponse response, final PollComponentInfo pollComponentInfo) {

        // prevent back button from showing form again
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        final HippoDocumentBean pollDocument = getPollDocument(request, pollComponentInfo);
        if (pollDocument != null) {

            request.setAttribute(POLL_DOCUMENT, pollDocument);

            final Node pollDataNode = getPollDataNode(request, pollDocument, pollComponentInfo);
            request.setAttribute(POLL_VOTES, getPollVotesBean(request, pollDocument, pollDataNode, pollComponentInfo));

            // value stored?
            final String persistedValue = getPersistentValue(request, pollDocument.getName());
            request.setAttribute(POLL_OPTION, persistedValue);

            if ((persistedValue != null) && !NO_COOKIE_SUPPORT.equals(persistedValue)) {
                request.setAttribute(POLL_VOTE_SUCCESS, true);
            } else {

                // just voted? store attributes and cookie when success
                final String voteSuccess = request.getParameter(POLL_VOTE_SUCCESS);
                if (voteSuccess != null) {
                    request.setAttribute(POLL_VOTE_SUCCESS, Boolean.parseBoolean(voteSuccess));
                }

                final String option = request.getParameter(POLL_OPTION);
                if (option != null) {
                    request.setAttribute(POLL_OPTION, option);
                }

                if (Boolean.TRUE.toString().equals(voteSuccess)) {

                    // Note that we set the cookie in the GET request processing rather than the
                    // POST request processing, as this seems more robust in terms of having the
                    // client accept the cookie. Also, this circumvents issues with including the
                    // cookie in the redirected GET request.
                    setPersistentValue(request, response, pollDocument.getName(), option);
                } else {

                    // prepare form data (poll document path)
                    request.setAttribute(POLL_DOCUMENT_PATH, getPollDocumentPath(pollDocument, request));
                }
            }
        }
    }

    /**
     * Get the first document of a searched list of documents containing poll compounds.
     */
    public HippoDocumentBean getPollDocument(final HstRequest request, final PollComponentInfo pollComponentInfo) {

        final List<HippoDocumentBean> pollDocuments = searchPollDocuments(request, pollComponentInfo);

        if ((pollDocuments != null) && (pollDocuments.size() > 0)) {
            return pollDocuments.get(0);
        }

        return null;
    }

    /**
     * Cast a vote to an option of a poll compound in a poll document, and persist it.
     *
     * @param request            the HST request
     * @param persistableSession JCR session with write rights
     * @param pollDocumentPath   the poll document's path, relative from content root
     * @param optionId           the id of the option to vote
     * @param pollComponentInfo  poll component parameter interface
     */
    public boolean castVote(final HstRequest request, final Session persistableSession,
                            final String pollDocumentPath, final String optionId,
                            final PollComponentInfo pollComponentInfo) {

        final Node votesNode = createPollDataVotesNode(request, persistableSession, pollDocumentPath, optionId, pollComponentInfo);

        if (votesNode != null) {

            LockHelper lockHelper = new LockHelper();
            if (lockHelper.getLock(votesNode)) {
                try {
                    long votesCount = 0;
                    if (votesNode.hasProperty("poll:count")) {
                        votesCount = votesNode.getProperty("poll:count").getLong();
                    }
                    votesNode.setProperty("poll:count", votesCount + 1);

                    persistableSession.save();

                    return true;
                } catch (RepositoryException e) {
                    logger.error("RepositoryException while saving a vote", e);
                } finally {
                    lockHelper.unlock(votesNode);
                }
            } else {
                logger.warn("Could not get a lock on node while saving a vote; document path is {}, optionId is {}", pollDocumentPath, optionId);
            }
        }

        return false;
    }

    /**
     * Get the node where to save a vote for a poll option; create if necessary.
     *
     * @return the node where to store votes, or null if the node could not be created.
     */
    protected Node createPollDataVotesNode(final HstRequest request, final Session persistableSession,
                                           final String pollDocumentPath, final String optionId, final PollComponentInfo pollComponentInfo) {

        LockHelper lockHelper = new LockHelper();

        final String baseDataPath = getBaseDataPath(request, pollComponentInfo);

        try {
            final String pollDataPath = getPollDataPath(baseDataPath, pollDocumentPath, request, persistableSession.getRootNode());

            final String[] pathItems = pollDataPath.split("/");
            Node currentNode = persistableSession.getRootNode();

            for (String pathItem : pathItems) {
                if (!currentNode.hasNode(pathItem)) {
                    if (lockHelper.getLock(currentNode)) {
                        try {
                            currentNode.addNode(pathItem, "poll:polldata");
                            persistableSession.save();
                        } finally {
                            lockHelper.unlock(currentNode);
                        }
                    } else {
                        logger.warn("Could not get a lock on node while creating poll data path {}; item is {}", pollDataPath, pathItem);
                        return null;
                    }
                }

                currentNode = currentNode.getNode(pathItem);
            }

            // create optionId node that holds the count
            if (!currentNode.hasNode(optionId)) {
                if (lockHelper.getLock(currentNode)) {
                    try {
                        currentNode.addNode(optionId, "poll:votes");
                        persistableSession.save();
                    } finally {
                        lockHelper.unlock(currentNode);
                    }
                } else {
                    logger.warn("Could not get a lock on node while creating option node: poll data path is {}", pollDataPath);
                    return null;
                }
            }

            return currentNode.getNode(optionId);
        } catch (RepositoryException e) {
            logger.error(e.getClass().getName() + ": cannot create path from base path '" + baseDataPath
                    + "' and document path '" + pollDocumentPath + "' and option '" + optionId + "'", e);
        }
        return null;
    }

    /**
     * Get the path to where the data (votes) is saved, relative to the JCR root, so without a leading slash.
     *
     * @param baseDataPath     the configured base data path, usually 'polldata'
     * @param pollDocumentPath the path of the poll document, relative to site content root
     * @param request          HST request
     * @param rootNode         JCR root node
     */
    protected String getPollDataPath(final String baseDataPath, final String pollDocumentPath,
                                     final HstRequest request, final Node rootNode) throws RepositoryException {

        // Before 1.08.01, the structure was [base] / [relativepath]
        final String oldStylePath = baseDataPath + "/" + stripSlash(pollDocumentPath);
        if (rootNode.hasNode(oldStylePath)) {
            logger.debug("Found existing old style poll data path (without site content root name): {}", oldStylePath);
            return oldStylePath;
        }

        // On 1.08.01+, multi site is better supported with structure [base] / [site root] / [relativepath]
        return baseDataPath + "/" + request.getRequestContext().getSiteContentBaseBean().getName() + "/" + stripSlash(pollDocumentPath);
    }

    /**
     * Get the node where to save votes for a poll; return null if it doesn't exist.
     */
    protected Node getPollDataNode(final HstRequest request, final HippoDocumentBean pollDocument,
                                   final PollComponentInfo pollComponentInfo) {

        final String baseDataPath = getBaseDataPath(request, pollComponentInfo);
        final String pollDocumentPath = getPollDocumentPath(pollDocument, request);

        try {
            final Node repRoot = pollDocument.getNode().getSession().getRootNode();
            final String pollDataPath = getPollDataPath(baseDataPath, pollDocumentPath, request, repRoot);

            if (logger.isDebugEnabled()) {
                logger.debug("Got pollDataPath {} from document path {}, exists={}",
                        pollDataPath, pollDocumentPath, repRoot.hasNode(pollDataPath));
            }

            if (repRoot.hasNode(pollDataPath)) {
                return repRoot.getNode(pollDataPath);
            }
        } catch (RepositoryException re) {
            logger.error("Cannot get poll data node", re);
        }

        return null;
    }

    /**
     * Get the configured or default base data path where to store votes
     */
    protected String getBaseDataPath(HstRequest request, final PollComponentInfo pollComponentInfo) {
        String baseDataPath = pollComponentInfo.getPollDataPath();
        if (Strings.isNullOrEmpty(baseDataPath)) {
            baseDataPath = PARAM_DATA_PATH_DEFAULT;
        }

        // Keep the votes cast under "preview" (i.e. through the channel manager)
        // in a separate tree in order to avoid name collisions.
        if (request.getRequestContext().isPreview()) {
            baseDataPath += "/" + POLL_DATA_NODE_PREVIEW;
        }
        return baseDataPath;
    }

    /**
     * Get the persisted value that was voted for.
     */
    protected String getPersistentValue(HstRequest request, String pollDocumentName) {

        if (request.getCookies() == null) {
            return NO_COOKIE_SUPPORT;
        }

        String name = getCookieName(request, pollDocumentName);

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * Persist the value that was voted for.
     */
    protected void setPersistentValue(final HstRequest request, final HstResponse response,
                                      final String pollDocumentName, final String value) {

        try {
            String name = getCookieName(request, pollDocumentName);
            Cookie cookie = new Cookie(name, value);
            cookie.setMaxAge(POLL_COOKIE_MAX_AGE);
            response.addCookie(cookie);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Helper function to keep cookie name in sync between read and write ops.
     */
    protected String getCookieName(final HstRequest request, final String pollDocumentName) {

        String cookieName = POLL_COOKIE_NAME_PREFIX + pollDocumentName.replace(" ", ".");
        if (request.getRequestContext().isChannelManagerPreviewRequest()) {
            cookieName += ".preview";
        }

        return cookieName;
    }

    /**
     * Get the poll document path, relative from site content root.
     * This is also where to store it's data from the polldata directory.
     */
    protected String getPollDocumentPath(final HippoDocumentBean pollDocument, final HstRequest request) {
        try {
            final String handlePath = stripSlash(pollDocument.getNode().getParent().getPath());
            final String contentBasePath = stripSlash(request.getRequestContext().getSiteContentBasePath());
            if (!handlePath.startsWith(contentBasePath)) {
                throw new IllegalStateException("Path " + handlePath + " must start with " + contentBasePath);
            }
            return stripSlash(handlePath.substring(contentBasePath.length()));
        } catch (RepositoryException e) {
            logger.error("Cannot determine handle path of document " + pollDocument.getPath(), e);
            return null;
        }
    }

    /**
     * Get a bean by absolute or relative path.
     */
    protected HippoBean getBean(final String path, final HstRequest request) {

        if (path.startsWith("/")) {

            // Using the folder picker in component properties dialog (since 1.05.06), we get an absolute repository
            // path, rather than the (manually configured) path starting already at the site content. We try to detect
            // if this is the case, and if so, adjust the path to be relative to the site content root.
            String relPath = path;
            final String siteBasePath = request.getRequestContext().getResolvedMount().getMount().getContentPath();
            if (path.startsWith(siteBasePath)) {
                relPath = path.substring(siteBasePath.length());
            }

            final HippoBean bean = request.getRequestContext().getSiteContentBaseBean().getBean(relPath.substring(1));
            if (bean != null) {
                return bean;
            }
            if (!path.equals("/")) {
                logger.warn("Bean could not be found by path {}, relative to site content root {}", relPath.substring(1), siteBasePath);
            }
        } else {
            final HippoBean currentDocument = request.getRequestContext().getContentBean();
            if (currentDocument != null) {
                HippoDocumentBean bean = currentDocument.getParentBean().getBean(path);
                if (bean != null) {
                    logger.error("Bean could not be found by path {} relative to {}", path, currentDocument.getParentBean().getPath());
                }
            } else {
                logger.error("Bean could not be found by relative path {} because the current content bean is null", path);
            }
        }

        return null;
    }

    /**
     * Get the bean class of the poll documents to search for.
     */
    protected Class<? extends HippoDocumentBean> getPollDocumentClass(final HstRequest request,
                                                                      final PollComponentInfo pollComponentInfo) {

        String beanClass = pollComponentInfo.getPollDocsClass();
        if (beanClass != null && !beanClass.equals("")) {
            try {
                return (Class<? extends HippoDocumentBean>) Class.forName(beanClass);
            } catch (ClassNotFoundException cnfe) {
                logger.error("Class {} extending HippoDocumentBean could not be found", beanClass);
            }
        }

        return PARAM_DOCS_CLASS_DEFAULT;
    }

    /**
     * Get the field name of the poll compound within the document.
     */
    protected String getPollCompoundName(final HstRequest request,
                                         final PollComponentInfo pollComponentInfo) {
        final String pollCompoundName = pollComponentInfo.getPollCompoundName();
        return (Strings.isNullOrEmpty(pollCompoundName))
                ? PARAM_DOC_POLL_COMPOUND_NAME_DEFAULT : pollCompoundName;
    }

    /**
     * Search poll documents based on the given poll document class.
     */
    protected List<HippoDocumentBean> searchPollDocuments(final HstRequest request,
                                                          final PollComponentInfo pollComponentInfo) {

        final String docsPath = pollComponentInfo.getPollDocsPath();
        if (Strings.isNullOrEmpty(docsPath)) {
            logger.error("Parameter 'poll-docsPath' must be configured");
            return null;
        }

        final HippoBean documentOrScope = getBean(docsPath, request);

        if (documentOrScope == null) {
            return null;
        }

        final Class<? extends HippoDocumentBean> beanClass = getPollDocumentClass(request, pollComponentInfo);

        // 'poll-docsPath' points to poll document directly
        if (beanClass.isAssignableFrom(documentOrScope.getClass())) {
            return Arrays.asList((HippoDocumentBean) documentOrScope);
        }

        // 'poll-docsPath' points to a folder that is the scope to search for poll documents
        try {
            final HstQuery query = request.getRequestContext().getQueryManager().createQuery(documentOrScope, beanClass);
            query.setLimit(queryLimit);

            final HstQueryResult result = query.execute();
            final HippoBeanIterator iterator = result.getHippoBeans();
            final List<HippoDocumentBean> beans = new ArrayList<>(result.getSize());
            while (iterator.hasNext()) {
                final Object bean = iterator.next();
                if (bean != null) {
                    beans.add((HippoDocumentBean) bean);
                }
            }

            return beans;
        } catch (QueryException qe) {
            logger.error("Querying for scope " + documentOrScope.getPath() + " and " + beanClass + " failed", qe);
            return null;
        }
    }


    /**
     * Get votes that are stored in repository as a bean for the view.
     */
    protected PollVotesBean getPollVotesBean(final HstRequest request, final HippoDocumentBean pollDocument,
                                             final Node pollDataNode, final PollComponentInfo pollComponentInfo) {

        PollVotesBean bean = new PollVotesBean();

        final String pollCompoundName = getPollCompoundName(request, pollComponentInfo);
        final Poll pollCompound = pollDocument.getBean(pollCompoundName);

        if (pollCompound == null) {
            logger.warn("Cannot get compound type by name {}", pollCompoundName);
            return null;
        }

        try {
            for (Option option : pollCompound.getOptions()) {

                long votesCount = 0;

                // votes present for option?
                if (pollDataNode != null) {
                    if (pollDataNode.hasNode(option.getValue())) {
                        Node votesNode = pollDataNode.getNode(option.getValue());
                        if (votesNode.hasProperty("poll:count")) {
                            votesCount = votesNode.getProperty("poll:count").getLong();
                        }
                    }
                }

                bean.addOptionVotes(option.getValue(), option.getLabel(), votesCount);
            }
        } catch (RepositoryException re) {
            logger.error("Error setting votes from data, pollDocument path is " + pollDocument.getPath(), re);
        }

        return bean;
    }

    protected static String stripSlash(final String string) {
        if (string.startsWith("/")) {
            return string.substring(1);
        }
        return string;
    }

    protected static String preventXSS(final String input) {
        if (input == null) {
            return "";
        } else {
            return input.trim().replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                    .replaceAll("eval\\((.*)\\)", "")
                    .replaceAll("[\"'][\\s]*javascript:(.*)[\"']", "\"\"");
        }
    }
}
