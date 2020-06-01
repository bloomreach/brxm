/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.platform.api.BlueprintService;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintServiceImpl implements BlueprintService {

    private final static Logger log = LoggerFactory.getLogger(BlueprintService.class);

    private HstModelRegistryImpl hstModelRegistry;

    public BlueprintServiceImpl(final HstModelRegistryImpl hstModelRegistry) {
        this.hstModelRegistry = hstModelRegistry;
    }

    @Override
    public List<Blueprint> getBlueprints(final Session userSession) {
        List<Blueprint> blueprints = new ArrayList<>();
        for (HstModel hstModel : hstModelRegistry.getModels().values()) {

            hstModel.getVirtualHosts().getBlueprints().stream()
                    .filter(blueprint -> {
                        final String configurationRootPath = ((InternalHstModel) hstModel).getConfigurationRootPath();
                        try {
                            final boolean granted = userSession.hasPermission(configurationRootPath + "/accesstest", Session.ACTION_ADD_NODE);
                            if (granted) {
                                log.info("Adding blueprint '{}' for user '{}'", blueprint, userSession.getUserID());
                            } else {
                                log.info("Skipping blueprint '{}' for user '{}' since not granted", blueprint, userSession.getUserID());
                            }
                            return granted;
                        } catch (RepositoryException e) {
                            log.error("Repository error when determining channel manager access", e);
                        }
                        return false;
                    })
                    // make sure to always use Blueprint.copy since we should not return the actual HST model instances
                    .forEach(blueprint -> blueprints.add(Blueprint.copy(blueprint)));

        }
        return blueprints;
    }

    @Override
    public Blueprint getBlueprint(final Session userSession, final String id) throws ChannelException {
        final List<Blueprint> blueprints = getBlueprints(userSession);
        return blueprints.stream().filter(blueprint -> blueprint.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ChannelException(String.format("Cannot find blueprint of id '%s'", id )));
    }
}
