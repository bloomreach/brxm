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
package org.hippoecm.repository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>EventListener</code> implementation that calls <code>Session</code>.refresh(true) for 
 * pulling in changes on the session. The refresh(true) will trigger dispatching the events.
 * The <code>RefreshingEventListener</code> must either be explicitly shut down with stopRefresher() or 
 * the the session must be logout out.
 */
public abstract class RefreshingEventListener implements EventListener {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** logger */
    private static Logger log = LoggerFactory.getLogger(RefreshingEventListener.class);

    /** whether the refresher should go on running */
    private volatile boolean keepRunning = false;

    /** the refresher thread */
    private Thread refresher;

    /** the interval between session refreshes in milliseconds */
    private long intervalMillis;

    /** the session that needs to be refreshed */
    private Session session;

    /**
     * Create a new RefreshingEventListener, which will automatically start the 
     * refresher thread. 
     * @param session the session to refresh, e.g. the session of the <code>ObservationManager</code>
     * @param intervalMillis the interval in milliseconds between refreshes
     */
    public RefreshingEventListener(Session session, long intervalMillis) {
        this.session = session;
        this.intervalMillis = intervalMillis;
        startRefresher();
    }

    /**
     * Stop the refresher thread.
     */
    public final void stopRefresher() {
        log.info("Stopping RefreshingEventListener");
        keepRunning = false;
    }

    /**
     * Start the refresher thread.
     */
    private final void startRefresher() {
        log.info("Starting RefreshingEventListener");
        keepRunning = true;
        refresher = new Thread("RefreshingEventListener") {
            @Override
            public void run() {
                while (keepRunning) {
                    try {
                        if (session != null && session.isLive()) {
                            session.refresh(true);
                        } else {
                            log.info("Session is gone. Stopping event listener refresher.");
                            stopRefresher();
                            break;
                        }
                    } catch (RepositoryException e) {
                        log.error("Error while refreshing session. Stopping event listener refresher.", e);
                        stopRefresher();
                        break;
                    }
                    try {
                        Thread.sleep(intervalMillis);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        };
        refresher.start();
    }
}
