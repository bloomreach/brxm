/*
 *  Copyright 2011 Hippo.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOTE: Do not re-use a {@link LinkChecker} instance after calling {@link #run(Iterable)} as after the call to
 * {@link #run(Iterable)} is finished connection resources are released and it can not be reused again.
 * To run another check create a new instance of {@link LinkChecker} and then call {@link #run(Iterable)} again
 */
public class LinkChecker {

    private static Logger log = LoggerFactory.getLogger(LinkChecker.class);

    private final HttpClient httpClient;
    private final int nrOfThreads;

    public LinkChecker(CheckExternalBrokenLinksConfig config) {
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

    private void runCheckerThreads(final Iterable<Link> links) {
        
        ConcurrentLinkedQueue<Link> queue = new ConcurrentLinkedQueue<Link>();
        for (Link link : links) {
            queue.add(link);
        }

        final int threadCount = Math.min(queue.size(), nrOfThreads);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new LinkCheckerRunner(queue);
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

    }

    private class LinkCheckerRunner extends Thread {
        private final ConcurrentLinkedQueue<Link> queue;
        public LinkCheckerRunner(final ConcurrentLinkedQueue<Link> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while(true) {
                    // Get the next item to process, throws a NoSuchElementException when we're done
                    Link link = queue.remove();
                    String url = link.getUrl();
                    if (url != null) {
                        final HttpContext httpContext = new BasicHttpContext();
                        HttpRequestBase httpRequest = new HttpHead(url);
                        try {
                            HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);
                            int headResultCode = httpResponse.getStatusLine().getStatusCode();
                            httpRequest.reset();
                            if(headResultCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
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
                        } catch (IOException ex) {
                            link.setBroken(true);
                            link.setBrokenSince(Calendar.getInstance());
                            link.setResultMessage(ex.toString());
                        } finally {
                            if ((httpRequest != null) && (!httpRequest.isAborted())) {
                                httpRequest.reset();
                            }
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                // Deliberate ignore, end of run
            }

            // The thread is done with the {@link HttpClient} and hence it is a good practice to direct the connection
            // manager to shutdown to release any remaining resources
            // Also based on the usage of the {@link LinkChecker} from {@link CheckBrokenLinksWorkflow}
            // and its implementation class {@link CheckBrokenLinksWorkflowImpl} for each run a new {@link LinkChecker}
            // is created and hence it is again better to direct the connection manager to shutdown
            //
            // @see http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e635
            httpClient.getConnectionManager().shutdown();
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
