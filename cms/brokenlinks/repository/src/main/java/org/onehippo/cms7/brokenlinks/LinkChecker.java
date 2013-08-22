/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOTE: Do not re-use a {@link LinkChecker} instance after calling {@link #run(Iterable)} as after the call to
 * {@link #run(Iterable)} is finished connection resources are released and it can not be reused again.
 * To run another check create a new instance of {@link LinkChecker} and then call {@link #run(Iterable)} again
 */
public class LinkChecker {

    private static Logger log = LoggerFactory.getLogger(LinkChecker.class);

    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("^([A-Za-z]+):.*$");

    // refresh session after checking 50 internal links
    private static final int REFRESH_SESSION_INTERVAL = 50;

    private final Session session;
    private final HttpClient httpClient;
    private final int nrOfThreads;

    public LinkChecker(CheckExternalBrokenLinksConfig config, Session session) {
        this.session = session;
        ClientConnectionManager connManager = new PoolingClientConnectionManager();
        HttpParams params = new SyncBasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, config.getSocketTimeout());
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, config.getConnectionTimeout());
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
        params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, false);

        HttpClient client = null;
        try {
            final String httpClientClassName = config.getHttpClientClassName();
            Class<? extends HttpClient> clientClass = (Class<? extends HttpClient>) Class.forName(httpClientClassName);
            final Constructor<? extends HttpClient> constructor = clientClass.getConstructor(
                    ClientConnectionManager.class, HttpParams.class);
            client = constructor.newInstance(connManager, params);
        } catch (ClassNotFoundException e) {
            log.error("Could not find configured http client class", e);
        } catch (NoSuchMethodException e) {
            log.error("Could not find constructor of signature <init>(ClientConnectionmanager, HttpParams)", e);
        } catch (InvocationTargetException e) {
            log.error("Could not invoke constructor of httpClient", e);
        } catch (InstantiationException e) {
            log.error("Could not instantiate http client", e);
        } catch (IllegalAccessException e) {
            log.error("Not allowed to access http client constructor", e);
        }
        if (client == null) {
            client = new DefaultHttpClient(connManager, params);
        }

        httpClient = client;
        nrOfThreads = config.getNrOfHttpThreads();
        // authentication preemptive true
        // allow circular redirects true
    }

    /**
     * Note that this method does not always return the same Link instances as in the <code>links</code> argument
     * because if the url of a Link was already scanned before, we replace the Link object with an already scanned
     * Link object
     * @param links
     */
    public void run(final Iterable<Link> links) {
        runCheckerThreads(links);
    }

    public void shutdown() {
        // see http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e635
        httpClient.getConnectionManager().shutdown();
    }

    private void runCheckerThreads(final Iterable<Link> links) {

        ConcurrentLinkedQueue<Link> queue = new ConcurrentLinkedQueue<Link>();

        for (Link link : links) {
            queue.add(link);
        }

        final int threadCount = Math.min(queue.size(), nrOfThreads);
        final AtomicInteger internalLinksChecked = new AtomicInteger();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new LinkCheckerRunner(queue, internalLinksChecked);
            threads[i].setUncaughtExceptionHandler(new LogUncaughtExceptionHandler(log));
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }

        try {
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
        } catch (InterruptedException ex) {
            // aborted
        }

        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.warn("Failed to clear the session.", e);
        }
    }

    private class LinkCheckerRunner extends Thread {

        private final ConcurrentLinkedQueue<Link> queue;
        private final AtomicInteger internalLinksChecked;

        public LinkCheckerRunner(final ConcurrentLinkedQueue<Link> queue, final AtomicInteger internalLinksChecked) {
            this.queue = queue;
            this.internalLinksChecked = internalLinksChecked;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Get the next item to process, throws a NoSuchElementException when we're done
                    Link link = queue.remove();
                    String url = link.getUrl();

                    if (StringUtils.isNotBlank(url)) {
                        Matcher schemedUrlMatcher = URL_SCHEME_PATTERN.matcher(url);

                        if (schemedUrlMatcher.matches()) {
                            final String scheme = StringUtils.lowerCase(schemedUrlMatcher.group(1));

                            if (StringUtils.equals("http", scheme) || StringUtils.equals("https", scheme)) {
                                checkExternalHttpLink(link);
                            } else {
                                log.debug("LinkChecker doesn't check non http(s) urls: '{}'.", url);
                            }
                        } else {
                            checkInternalLink(link);
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                // Deliberate ignore, end of run
            }
        }

        private void checkInternalLink(Link link) {
            String url = link.getUrl();

            if (StringUtils.contains(url, "/")) {
                log.debug("Not a CMS internal link which cannot have a '/': {}", url);
                return;
            }

            if (StringUtils.isEmpty(link.getSourceNodeIdentifier())) {
                log.debug("Unable to check internal link. The link is unaware of source node identifier: {}", url);
                return;
            }

            if (session == null) {
                log.warn("Session is not given to LinkChecker!");
                return;
            }

            synchronized (session) {
                try {
                    Node sourceNode = session.getNodeByIdentifier(link.getSourceNodeIdentifier());
                    Node linkedNode = findLinkedNode(sourceNode, url);

                    if (linkedNode == null) {
                        link.setBroken(true);
                        link.setBrokenSince(Calendar.getInstance());
                        link.setResultCode(Link.ERROR_CODE);
                        link.setResultMessage("Broken reference");
                    }

                    if (internalLinksChecked.incrementAndGet() % REFRESH_SESSION_INTERVAL == 0) {
                        session.refresh(false);
                    }
                } catch (RepositoryException e) {
                    log.warn("Failed to find the source node.", e);
                }
            }
        }

        private Node findLinkedNode(final Node sourceNode, String linkName) {
            try {
                if (!sourceNode.hasNode(linkName)) {
                    log.debug("The source node doesn't have the link node named '{}'.", linkName);
                    return null;
                }

                Node linkNode = sourceNode.getNode(linkName);

                if (!linkNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    log.debug("The link node doesn't have the '{}' property.", HippoNodeType.HIPPO_DOCBASE);
                    return null;
                }

                String docbase = linkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();

                if (StringUtils.isBlank(docbase)) {
                    log.debug("The link node has a blank '{}' property.", HippoNodeType.HIPPO_DOCBASE);
                    return null;
                }

                return session.getNodeByIdentifier(docbase);
            } catch (ItemNotFoundException e) {
                log.debug("The linked node is not found.", e);
            } catch (RepositoryException e) {
                log.warn("Failed to find linked node.", e);
            }

            return null;
        }

        private void checkExternalHttpLink(Link link) {
            String url = StringUtils.trim(link.getUrl());

            final HttpContext httpContext = new BasicHttpContext();
            HttpRequestBase httpRequest = null;

            try {
                httpRequest = new HttpHead(url);
                HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);
                int headResultCode = httpResponse.getStatusLine().getStatusCode();
                httpRequest.reset();

                if (headResultCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                    httpRequest = new HttpGet(url);
                    httpResponse = httpClient.execute(httpRequest, httpContext);
                    headResultCode = httpResponse.getStatusLine().getStatusCode();
                    httpRequest.reset();
                }

                if (headResultCode == HttpStatus.SC_MOVED_PERMANENTLY || headResultCode >= HttpStatus.SC_BAD_REQUEST) {
                    link.setBroken(true);
                    link.setBrokenSince(Calendar.getInstance());
                    link.setResultCode(headResultCode);
                }
            } catch (IOException ioException) {
                link.setBroken(true);
                link.setBrokenSince(Calendar.getInstance());
                link.setResultCode(Link.EXCEPTION_CODE);
                link.setResultMessage(ioException.getClass().getCanonicalName());
            } catch (IllegalArgumentException ex) {
                link.setBroken(true);
                link.setBrokenSince(Calendar.getInstance());
                link.setResultCode(Link.EXCEPTION_CODE);
                link.setResultMessage(ex.getClass().getCanonicalName());
            } finally {
                if ((httpRequest != null) && (!httpRequest.isAborted())) {
                    httpRequest.reset();
                }
            }
        }
    }

    static class LogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Logger exceptionLog;

        LogUncaughtExceptionHandler(Logger exceptionLog) {
            this.exceptionLog = exceptionLog;
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            exceptionLog.error(e.getClass().getName() + ": " + e.getMessage(), e);
            t.getThreadGroup().uncaughtException(t, e);
        }

    }

}
