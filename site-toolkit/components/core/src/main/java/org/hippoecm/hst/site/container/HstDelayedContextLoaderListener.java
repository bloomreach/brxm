/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.site.container;

import javax.servlet.ServletContextEvent;

import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

/**
 * If the Spring ContextLoaderListener loading a Spring root application context that relies on
 * HstService#isAvailable because it requires beans from the HST Spring Component Manager, then this
 * {@link HstDelayedContextLoaderListener} must be used instead of {@link ContextLoaderListener} because
 * it postpones initialization to a separate thread until the HST Spring Component Manager is available
 */
public class HstDelayedContextLoaderListener extends ContextLoaderListener {

    private final static Logger log = LoggerFactory.getLogger(HstDelayedContextLoaderListener.class);

    private Thread initThread;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        if (HstServices.isAvailable()) {
            super.contextInitialized(event);
        } else {
            initThread = new Thread(() -> {
                boolean retry = true;
                while (retry && !HstServices.isAvailable()) {
                    log.info("Waiting for the HstServices to become available before initializing the Spring root application context.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.info("Waiting for the HstServices got interrupted. Quiting");
                        retry = false;
                        Thread.currentThread().interrupt();
                    }
                }
                if (HstServices.isAvailable()) {
                    log.info("HstServices is available. Initializing the Spring root application context");
                    super.contextInitialized(event);
                }
            });
            // stop this init thread when the jvm exits without this init thread to finish, hence make is a daemon
            initThread.setDaemon(true);
            initThread.start();
        }

    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (initThread != null && initThread.isAlive()) {
            initThread.interrupt();
            try {
                initThread.join();
            } catch (InterruptedException e) {
                log.error("Interrupted while stopping initThread", e);
                initThread.interrupt();
            }
        }
        super.contextDestroyed(event);
    }
}
