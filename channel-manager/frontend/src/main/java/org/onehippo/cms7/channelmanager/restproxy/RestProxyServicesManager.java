/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.channelmanager.restproxy;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.rest.SiteService;
import org.onehippo.cms7.channelmanager.channels.util.rest.RestClientProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.channelmanager.ChannelManagerConsts.CONFIG_REST_PROXY_SERVICE_ID;

public class RestProxyServicesManager implements IInitializer {

    private static final Logger log = LoggerFactory.getLogger(RestProxyServicesManager.class);

    public static final String DEFAULT_CONTEXT_PATH = "/site";
    private static final int MAX_THREADS = 5;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS, new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

    private static ConcurrentHashMap<String, RestProxyInfo> restProxyInfos = new ConcurrentHashMap<>();

    public static <T> List<Future<T>> submitJobs(List<? extends Callable<T>> jobs) {
        try {
            return executorService.invokeAll(jobs);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    public static Map<String, IRestProxyService> getLiveRestProxyServices(final IPluginContext context,
                                                                          final IPluginConfig config) {
        final String restProxyServiceId = config.getString(CONFIG_REST_PROXY_SERVICE_ID, IRestProxyService.class.getName());
        if (restProxyServiceId == null) {
            log.warn("Cannot load rest proxy services for config '{}' since the config does not have a value for '{}'",
                    config);

        }
        final List<IRestProxyService> restProxyServices = context.getServices(restProxyServiceId, IRestProxyService.class);
        if (restProxyServices == null || restProxyServices.isEmpty()) {
            log.info("Channel manager is not avaiable since no rest services found for '{}'", CONFIG_REST_PROXY_SERVICE_ID);
            return Collections.emptyMap();
        }
        Map<String, IRestProxyService> restProxyServiceMap = new HashMap<>();

        for (IRestProxyService restProxyService : restProxyServices) {
            String contextPath = restProxyService.getContextPath();
            if (contextPath == null) {
                contextPath = DEFAULT_CONTEXT_PATH;
            }
            final IRestProxyService previous = restProxyServiceMap.put(contextPath, new RestClientProxyFactory(restProxyService));
            if (previous != null) {
                log.error("Multiple rest proxy services are configured for '{}' but " +
                        "the rest services either do have duplicate values for property 'context.path' or do not have the " +
                        "'context.path' configured. If multiple rest proxy services are required, unique 'context.path' " +
                        "values that are in sync with the 'rest.uri' need to be configured. Only one rest of the rest " +
                        "proxy services will be available.", restProxyServiceId);
            }
        }

        // check whether the rest proxies are available
        final Map<String, IRestProxyService> liveRestProxyServiceMap = new HashMap<>();

        final long currentTime = System.currentTimeMillis();
        final List<Future<?>> futures = new ArrayList<>();

        for (Map.Entry<String, IRestProxyService> entry : restProxyServiceMap.entrySet()) {
            final String contextPath = entry.getKey();
            final IRestProxyService proxyService = entry.getValue();
            final RestProxyInfo restProxyInfo = getOrCreateRestProxyInfo(contextPath);

            synchronized (restProxyInfo) {
                if (restProxyInfo.isLive && currentTime < restProxyInfo.nextCheckTimestamp) {
                    liveRestProxyServiceMap.put(contextPath, proxyService);
                } else if (currentTime > restProxyInfo.nextCheckTimestamp) {
                    if (restProxyInfo.alreadyScheduled) {
                        continue;
                    }
                    // live check required
                    final SiteService siteService = proxyService.createSecureRestProxy(SiteService.class);
                    restProxyInfo.alreadyScheduled = true;
                    futures.add(executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final boolean alive = siteService.isAlive();
                                if (alive) {
                                    liveRestProxyServiceMap.put(contextPath, proxyService);
                                    restProxyInfo.isLive = true;
                                    restProxyInfo.nextCheckTimestamp = System.currentTimeMillis() + restProxyInfo.checkIntervalMilliSeconds;
                                    restProxyInfo.consecutiveFailures = 0;
                                } else {
                                    liveRestProxyServiceMap.remove(contextPath);
                                    markFailed(restProxyInfo);

                                    log.warn("Site for contextPath {} is not live. Re-checking in {} milliseconds. Cause: {}",
                                            contextPath, (restProxyInfo.nextCheckTimestamp - currentTime));

                                }
                            } catch (Exception e) {
                                liveRestProxyServiceMap.remove(contextPath);
                                markFailed(restProxyInfo);

                                if (log.isDebugEnabled()) {
                                    log.warn("Site for contextPath {} is not live. Re-checking in {} milliseconds.",
                                            contextPath, (restProxyInfo.nextCheckTimestamp - currentTime), e);
                                } else {
                                    log.warn("Site for contextPath {} is not live. Re-checking in {} milliseconds. Cause: {}",
                                            contextPath, (restProxyInfo.nextCheckTimestamp - currentTime), e.toString());
                                }
                            } finally {
                                restProxyInfo.alreadyScheduled = false;
                            }
                        }
                    }));

                }
            }
        }
        // make sure that all submitted tasks are really finished
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Exception while retrieving future.", e);
            }
        }

        return Collections.unmodifiableMap(liveRestProxyServiceMap);
    }

    private static RestProxyInfo getOrCreateRestProxyInfo(final String contextPath) {
        RestProxyInfo restProxyInfo = restProxyInfos.get(contextPath);
        if (restProxyInfo == null) {
            synchronized (RestProxyServicesManager.class) {
                restProxyInfo = restProxyInfos.get(contextPath);
                if (restProxyInfo == null) {
                    restProxyInfo = new RestProxyInfo();
                    restProxyInfos.put(contextPath, restProxyInfo);
                }
            }
        }
        return restProxyInfo;
    }

    private static void markFailed(final RestProxyInfo restProxyInfo) {
        restProxyInfo.isLive = false;
        restProxyInfo.consecutiveFailures++;
        // after every consecutive new failure we delay the next try...until at most 5 times
        long reCheckIntervalMilliSeconds = restProxyInfo.checkIntervalMilliSeconds * Math.min(5, restProxyInfo.consecutiveFailures);
        restProxyInfo.nextCheckTimestamp = System.currentTimeMillis() + reCheckIntervalMilliSeconds;
    }

    @Override
    public void init(final Application application) {

    }

    @Override
    public void destroy(final Application application) {
        if (executorService.isShutdown() || executorService.isTerminated()) {
            log.debug("Executor service has already shut down");
        } else {
            log.debug("Shutting down the executor service");
            executorService.shutdown();
            try {
                final long duration = 10L;
                final TimeUnit unit = TimeUnit.SECONDS;
                log.debug("Awaiting termination of running tasks for at most {} {}", duration, unit);
                if (executorService.awaitTermination(duration, unit)) {
                    log.debug("All tasks terminated within {} {}", duration, unit);
                } else {
                    final int pendingTasks = executorService.shutdownNow().size();
                    log.debug("{} pending tasks did not execute and are destroyed", pendingTasks);
                }
            } catch (InterruptedException e) {
                log.error("An error occurred while awaiting termination", e);
            }
            log.debug("Executor service successfully shut down");
        }
    }

    private static class RestProxyInfo {
        private boolean isLive;
        private long nextCheckTimestamp = 0;
        private boolean alreadyScheduled;
        private int consecutiveFailures = 0;
        //one minute
        private long checkIntervalMilliSeconds = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);

    }
}
