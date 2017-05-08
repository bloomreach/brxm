/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.backend;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.util.Text;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.api.ConfigurationService;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.engine.BaselineResourceInputProvider;
import org.onehippo.cm.engine.parser.ConfigSourceParser;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.engine.parser.RepoConfigParser;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ContentSourceImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_NODE;
import static org.onehippo.cm.engine.Constants.ACTIONS_TYPE;
import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.BASELINE_PATH;
import static org.onehippo.cm.engine.Constants.BASELINE_TYPE;
import static org.onehippo.cm.engine.Constants.BINARY_TYPE;
import static org.onehippo.cm.engine.Constants.CND_PROPERTY;
import static org.onehippo.cm.engine.Constants.CND_TYPE;
import static org.onehippo.cm.engine.Constants.CONFIG_FOLDER_TYPE;
import static org.onehippo.cm.engine.Constants.CONTENT_FOLDER_TYPE;
import static org.onehippo.cm.engine.Constants.CONTENT_PATH_PROPERTY;
import static org.onehippo.cm.engine.Constants.CONTENT_TYPE;
import static org.onehippo.cm.engine.Constants.DEFAULT_DIGEST;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.engine.Constants.DEFINITIONS_TYPE;
import static org.onehippo.cm.engine.Constants.MANIFEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_NODE;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_TYPE;
import static org.onehippo.cm.engine.Constants.DIGEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.GROUP_TYPE;
import static org.onehippo.cm.engine.Constants.LAST_UPDATED_PROPERTY;
import static org.onehippo.cm.engine.Constants.MODULE_TYPE;
import static org.onehippo.cm.engine.Constants.PROJECT_TYPE;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;
import static org.onehippo.cm.engine.Constants.REPO_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.YAML_PROPERTY;

public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final Session session;

    public ConfigurationServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void apply(final MergedModel mergedModel, final EnumSet<DefinitionType> includeDefinitionTypes)
            throws Exception {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            final ConfigurationPersistenceService service =
                    new ConfigurationPersistenceService(session, mergedModel.getResourceInputProviders());
            service.apply(mergedModel, includeDefinitionTypes);
            session.save();

            stopWatch.stop();
            log.info("MergedModel applied in {} for definitionTypes: {}", stopWatch.toString(), includeDefinitionTypes);
        }
        catch (Exception e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        }
    }

}
