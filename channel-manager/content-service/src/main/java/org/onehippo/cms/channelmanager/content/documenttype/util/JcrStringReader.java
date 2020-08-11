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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads String properties from JCR nodes.
 */
public class JcrStringReader implements JcrPropertyReader<String> {

    private static final Logger log = LoggerFactory.getLogger(JcrStringReader.class);
    private static final JcrStringReader INSTANCE = new JcrStringReader();

    public static JcrStringReader get() {
        return INSTANCE;
    }

    private JcrStringReader() {
    }

    @Override
    public Optional<String> read(final Node node, final String name) {
        try {
            if (node.hasProperty(name)) {
                return Optional.of(node.getProperty(name).getString());
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read property '{}' from node '{}'.", name, JcrUtils.getNodePathQuietly(node), e);
        }
        return Optional.empty();
    }
}