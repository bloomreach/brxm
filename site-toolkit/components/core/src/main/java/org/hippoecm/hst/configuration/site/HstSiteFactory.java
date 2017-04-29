/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableMap;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCHOF;

public class HstSiteFactory {

    private static final Logger log = LoggerFactory.getLogger(HstSiteFactory.class);


    public HstSite createLiveSiteService(final HstNode site,
                                                       final Mount mount,
                                                       final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                       final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return createSiteService(site, mount, mountSiteMapConfiguration, hstNodeLoadingCache, false);
    }

    public HstSite createPreviewSiteService(final HstNode site,
                                                          final Mount mount,
                                                          final MountSiteMapConfiguration mountSiteMapConfiguration,
                                                          final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {
        return createSiteService(site, mount, mountSiteMapConfiguration, hstNodeLoadingCache, true);
    }


    private HstSite createSiteService(final HstNode site,
                                      final Mount mount,
                                      final MountSiteMapConfiguration mountSiteMapConfiguration,
                                      final HstNodeLoadingCache hstNodeLoadingCache,
                                      final boolean isPreviewSite) {

        final HstSite master = new HstSiteService(site, mount, mountSiteMapConfiguration, hstNodeLoadingCache, isPreviewSite);

        if (master.getChannel() == null) {
            log.debug("Branches can only exist for configurations that have a channel");
            return master;
        }

        final String masterConfigPath = master.getConfigurationPath();
        final String masterName = StringUtils.substringAfterLast(masterConfigPath, "/");
        HstNode masterConfiguration = hstNodeLoadingCache.getNode(masterConfigPath);
        if (masterConfiguration.getValueProvider().hasProperty(BRANCH_PROPERTY_BRANCHOF)) {
            throw new ModelLoadingException(String.format("Invalid HST configuration for '%s' : It should be a master " +
                    "branch because directly referenced by an hst:site but '%s' does have the property '%s' which is " +
                    "only alowed on branches. Correct the configuration.", masterConfigPath, masterConfigPath, BRANCH_PROPERTY_BRANCHOF));
        }

        HstNode configurationsNode = masterConfiguration.getParent();

        Map<String, HstSite> branches = new HashMap<>();
        for (HstNode branchNode : configurationsNode.getNodes()) {
            if (!branchNode.getValueProvider().hasProperty(BRANCH_PROPERTY_BRANCHOF)) {
                log.debug("Skipping config '{}' which is not a branch.", branchNode.getName());
                continue;
            }
            final String branchOf = branchNode.getValueProvider().getString(BRANCH_PROPERTY_BRANCHOF);

            if (!masterName.equals(branchOf)) {
                log.debug("Skipping branch '{}' because not a branch of '{}'.", branchNode.getName(), masterName);
                continue;
            }
            if (!branchNode.getName().startsWith(masterName + "-")) {
                log.warn("Invalid branch '{}' because the branch node name '{}' does not start with the master '{}' name " +
                        " plus '-'. Skip trying to load this branch", branchNode.getName(),  branchNode.getName(), masterName);
                continue;
            }
            if (isPreviewSite && !branchNode.getName().endsWith("-preview")) {
                log.debug("Skip loading live branch '{}' for preview master.", branchNode.getName());
                continue;
            }
            if (!isPreviewSite && branchNode.getName().endsWith("-preview")) {
                log.debug("Skip loading preview branch '{}' for live master.", branchNode.getName());
                continue;
            }
            log.info("Found branch '{}' for configuration '{}'. Loading branch.", branchNode.getName(), branchOf);
            try {
                final HstSite branch = new HstSiteService(site, mount, mountSiteMapConfiguration,
                        hstNodeLoadingCache, branchNode.getValueProvider().getPath(), master.getChannel());
                branches.put(branch.getChannel().getBranchId(), branch);
            } catch (ModelLoadingException e) {
                log.error("Could not load branch '{}'. Skip that branch.", branchNode.getName(), e);
            }
        }

        if (branches.isEmpty()) {
            return master;
        }
        log.info("Return CompositeHstSite consisting of master '{}' and branches '{}'", master, branches);

        return new CompositeHstSiteImpl(master, unmodifiableMap(branches));

    }

}
