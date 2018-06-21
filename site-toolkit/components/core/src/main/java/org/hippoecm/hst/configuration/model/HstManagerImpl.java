/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

public class HstManagerImpl implements HstManager, ServletContextAware {
    
    private static final Logger log = LoggerFactory.getLogger(HstManagerImpl.class);

    private Object hstModelMutex;
    private HstModelProvider hstModelProvider;

    private volatile VirtualHosts prevVirtualHostsModel;
    private volatile VirtualHosts virtualHostsModel;


    protected volatile BuilderState state = BuilderState.UNDEFINED;



    protected enum BuilderState {
        UNDEFINED,
        UP2DATE,
        FAILED,
        STALE,
        SCHEDULED,
        RUNNING;
    }
    volatile int consecutiveBuildFailCounter = 0;

    private boolean staleConfigurationSupported = false;

    private HstURLFactory urlFactory;

    private HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    private HstComponentRegistry componentRegistry;

    private HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry;
    private HstCache pageCache;
    private boolean clearPageCacheAfterModelLoad;
    /**
     *
     * the default cms preview prefix : The prefix all URLs when accessed through the CMS
     */
    private String cmsPreviewPrefix;

    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";

    private String[] hstFilterPrefixExclusions;

    private String[] hstFilterSuffixExclusions;
    private ServletContext servletContext;

    /**
     * The list of implicit configuration augmenters which can provide extra hst configuration after the {@link VirtualHosts} object
     * has been created
     */
    List<HstConfigurationAugmenter> hstConfigurationAugmenters = new ArrayList<>();

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setHstModelProvider(HstModelProvider hstModelProvider) {
        this.hstModelProvider = hstModelProvider;
    }

    public void setHstModelMutex(Object hstModelMutex) {
        this.hstModelMutex = hstModelMutex;
    }

    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public void setSiteMapItemHandlerRegistry(HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
    }

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
    }

    public void setClearPageCacheAfterModelLoad(final boolean clearPageCacheAfterModelLoad) {
        this.clearPageCacheAfterModelLoad = clearPageCacheAfterModelLoad;
    }


    public String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public void setCmsPreviewPrefix(String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }

    /** @deprecated Since CMS 10.0, HST 2.30.00.
    */
    @Deprecated
    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    @Deprecated
    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }

    public HstSiteMapMatcher getSiteMapMatcher() {

        return hstModelProvider.getHstModel().getHstSiteMapMatcher();
    }

    @Override
    public List<HstConfigurationAugmenter> getHstConfigurationAugmenters() {
        return hstConfigurationAugmenters;
    }

    /**
     * Adds <code>hstConfigurationProvider</code> to {@link #hstConfigurationAugmenters}
     * @param augmenter
     */
    public void addHstConfigurationAugmenter(HstConfigurationAugmenter augmenter) {
        hstConfigurationAugmenters.add(augmenter);
    }

    /**
     * @deprecated since CMS 10.0, HST 2.30.00
     */
    @Deprecated
    public void setSiteMapItemHandlerFactory(HstSiteMapItemHandlerFactory siteMapItemHandlerFactory) {
        this.siteMapItemHandlerFactory = siteMapItemHandlerFactory;
    }

    /**
     * @deprecated since CMS 10.0, HST 2.30.00
     */
    @Deprecated
    public HstSiteMapItemHandlerFactory getSiteMapItemHandlerFactory() {
        return siteMapItemHandlerFactory;
    } 
    
    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }

    public void setHstFilterPrefixExclusions(final String[] hstFilterPrefixExclusions) {
        this.hstFilterPrefixExclusions = hstFilterPrefixExclusions;
    }

    public void setHstFilterSuffixExclusions(final String[] hstFilterSuffixExclusions) {
        this.hstFilterSuffixExclusions = hstFilterSuffixExclusions;
    }

    public String getContextPath() {
        return servletContext.getContextPath();
    }

    public void setStaleConfigurationSupported(boolean staleConfigurationSupported) {
        log.info("Is stale configuration for HST model supported: '{}'", staleConfigurationSupported);
        this.staleConfigurationSupported = staleConfigurationSupported;
    }

    @Deprecated
    public boolean isExcludedByHstFilterInitParameter(String pathInfo) {
        return isHstFilterExcludedPath(pathInfo);
    }

    @Override
    public boolean isHstFilterExcludedPath(final String pathInfo) {
        if (hstFilterPrefixExclusions != null) {
            for(String excludePrefix : hstFilterPrefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludePrefix '{}'", pathInfo, excludePrefix);
                    return true;
                }
            }
        }
        if (hstFilterSuffixExclusions != null) {
            for(String excludeSuffix : hstFilterSuffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludeSuffix '{}'", pathInfo, excludeSuffix);
                    return true;
                }
            }
        }
        return false;
    }

//    private void asynchronousBuild() {
//        synchronized (hstModelMutex) {
//            if (state == BuilderState.UP2DATE) {
//                // other thread already built the model
//                return;
//            }
//            if (state == BuilderState.SCHEDULED) {
//                // already scheduled
//                return;
//            }
//            if (state == BuilderState.RUNNING) {
//                log.error("BuilderState should not be possible to be in RUNNING state at this point. Return");
//                return;
//            }
//            state = BuilderState.SCHEDULED;
//            log.info("Asynchronous hst model build will be scheduled");
//            Thread scheduled = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        long reloadDelay = computeReloadDelay(consecutiveBuildFailCounter);
//                        if (reloadDelay > 0) {
//                            Thread.sleep(reloadDelay);
//                        }
//                        synchronousBuild();
//                    } catch (ContainerException e) {
//                        log.warn("Exception during building virtualhosts model. ", e);
//                    } catch (InterruptedException e) {
//                        log.info("InterruptedException ", e);
//                    }
//                }
//            });
//            scheduled.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//                @Override
//                public void uncaughtException(final Thread t, final Throwable e) {
//                    log.warn("Runtime exception "+e.getClass().getName()+" during building asynchronous " +
//                            "HST model. Reason : " + e.getMessage(), e);
//                }
//            });
//            scheduled.start();
//        }
//    }

    @Override
    public VirtualHosts getVirtualHosts(boolean allowStale) throws ContainerException {

        // TODO HSTTWO-4355 support for allowStale?
        final VirtualHosts virtualHosts = hstModelProvider.getHstModel().getVirtualHosts();

        // TODO HSTTWO-4355 if the hstModelRegistry#getVirtualHosts triggered a new model build because of a change, it must
        // TODO HSTTWO-4355 inform the current webapp to invoke componentRegistry.unregisterAllComponents(); and siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();
        // TODO HSTTWO-4355 how will we communicate this? A guava bus registered by hst-platform?

        // TODO HSTTWO-4355 and invoke after a new model the following:
//        if (clearPageCacheAfterModelLoad) {
//            log.info("Clearing page cache after new model is loaded");
//            pageCache.clear();
//        } else {
//            log.debug("Page cache won't be cleared because 'clearPageCacheAfterModelLoad = false'");
//        }

        // TODO HSTTWO-4355 componentRegistry.unregisterAllComponents();
        // TODO HSTTWO-4355 siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();


        return virtualHosts;

//        if (state == BuilderState.UP2DATE) {
//            return virtualHostsModel;
//        }
//        if (state == BuilderState.UNDEFINED) {
//            return synchronousBuild();
//        }
//        if (allowStale && staleConfigurationSupported) {
//            asynchronousBuild();
//            return prevVirtualHostsModel;
//        }
//        return synchronousBuild();
    }

    @Override
    public VirtualHosts getVirtualHosts() throws ContainerException {
        return getVirtualHosts(false);
    }

//    private VirtualHosts synchronousBuild() throws ContainerException {
//        if (state != BuilderState.UP2DATE) {
//            synchronized (hstModelMutex) {
//                if (state == BuilderState.UP2DATE) {
//                    return virtualHostsModel;
//                } else {
//                    try {
//                        state = BuilderState.RUNNING;
//                        try {
//                            buildSites();
//                            state = BuilderState.UP2DATE;
//                        } catch (ModelLoadingException e) {
//                            state = BuilderState.FAILED;
//                            consecutiveBuildFailCounter++;
//                            if (prevVirtualHostsModel == null) {
//                                throw new ContainerException("HST model failed to load : " + e.toString(), e);
//                            } else {
//                                log.warn("Exception during model loading happened. Return previous stale model. Reason: " + e.toString(), e);
//                            }
//                            return prevVirtualHostsModel;
//                        }
//                    } finally {
//                        if (state == BuilderState.RUNNING) {
//                            log.warn("Model failed to built. Serve old virtualHosts model.");
//                            consecutiveBuildFailCounter++;
//                            state = BuilderState.FAILED;
//                        }
//                    }
//                    if (state == BuilderState.FAILED) {
//                        // do not flush pageCache but return old prev virtual host instance instead
//                        return prevVirtualHostsModel;
//                    }
//                    if (clearPageCacheAfterModelLoad) {
//                        log.info("Clearing page cache after new model is loaded");
//                        pageCache.clear();
//                    } else {
//                        log.debug("Page cache won't be cleared because 'clearPageCacheAfterModelLoad = false'");
//                    }
//                }
//                if (state == BuilderState.UP2DATE) {
//                    consecutiveBuildFailCounter = 0;
//                    prevVirtualHostsModel = virtualHostsModel;
//                }
//                return virtualHostsModel;
//            }
//        }
//        return virtualHostsModel;
//    }

    private long computeReloadDelay(final int consecutiveBuildFailCounter) {
        switch (consecutiveBuildFailCounter) {
            case 0 : return 0L;
            case 1 : return 0L;
            case 2 : return 100L;
            case 3 : return 1000L;
            case 4 : return 10000L;
            case 5 : return 30000L;
            default : return 60000L;
        }
    }

    private void buildSites() {

        // hstEventsDispatcher.dispatchHstEvents();

        log.info("Start building in memory hst configuration model");

        // TODO HSTTWO-4355 arrange something for componentRegistry.unregisterAllComponents(); and  siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();

//        try {
//            long start = System.currentTimeMillis();
//
//            //VirtualHostsService newModel = new VirtualHostsService(this, hstNodeLoadingCache);
//
              // TODO below is the HstConfigurationAugmenter which is still needed! However only in CMS webapp MOST LIKELY!
//            for (HstConfigurationAugmenter configurationAugmenter : hstConfigurationAugmenters) {
//                log.info("Configuration augmenter '{}' will be augmented.", configurationAugmenter.getClass().getName());
//                configurationAugmenter.augment(newModel);
//            }
//
//            componentRegistry.unregisterAllComponents();
//            siteMapItemHandlerRegistry.unregisterAllSiteMapItemHandlers();
//
//            log.info("Finished build in memory hst configuration model in '{}' ms.", (System.currentTimeMillis() - start));
//            virtualHostsModel = newModel;
//        } catch (ModelLoadingException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new ModelLoadingException("Could not load hst node model due to Runtime Exception :", e);
//        }
    }

    // TODO HSTTWO-4355 get rid of the logic below, the CMS webapp should control the HST model loading / staleness
    @Override
    public void markStale() {
        synchronized (hstModelMutex) {
            if (state != BuilderState.UNDEFINED) {
                state = BuilderState.STALE;
            }
        }
    }

}
