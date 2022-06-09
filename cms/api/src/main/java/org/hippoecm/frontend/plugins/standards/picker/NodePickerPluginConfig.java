/*
 *  Copyright 2022 Bloomreach (https://www.bloomreach.com)
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

package org.hippoecm.frontend.plugins.standards.picker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.l10n.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodePickerPluginConfig extends JavaPluginConfig {

    private static final Logger log = LoggerFactory.getLogger(NodePickerPluginConfig.class);

    private static final Set<String> VALID_PARAMETER_KEYS = Stream.of("branchId",
                    "channelId",
                    "componentId",
                    "documentId",
                    "documentPath",
                    "fieldId",
                    "fieldIndex",
                    "fieldPath",
                    "folderPath",
                    "locale")
            .collect(Collectors.toSet());

    private static final Set<String> VALID_SUBSTITUTION_KEYS = Stream.of(
                    NodePickerControllerSettings.BASE_PATH,
                    NodePickerControllerSettings.LAST_VISITED_KEY,
                    NodePickerControllerSettings.CLUSTER_NAME)
            .collect(Collectors.toSet());

    public NodePickerPluginConfig(final IPluginConfig config, final Map<String, Object> parameters) {
        super(config);

        final Map<String, Object> params = createContextParameters(parameters);
        final StringSubstitutor stringSubstitutor = new StringSubstitutor(params, "#{", "}");

        VALID_SUBSTITUTION_KEYS.forEach(key -> {
            if (containsKey(key)) {
                put(key, stringSubstitutor.replace(get(key)));
            }
        });
    }

    private Map<String, Object> createContextParameters(final Map<String, Object> parameters) {
        final Map<String, Object> params  = new HashMap<>();
        if (parameters != null) {
            params.putAll(parameters);
        }

        final Set<String> unsupportedParams = params.keySet().stream()
                .filter(VALID_PARAMETER_KEYS::contains)
                .collect(Collectors.toSet());

        if (!unsupportedParams.isEmpty()){
            log.warn("The following supplied parameters are not supported: {}", String.join(",", unsupportedParams));
            unsupportedParams.forEach(params::remove);
        }

        params.putIfAbsent("branchId", BranchConstants.MASTER_BRANCH_ID);
        params.putIfAbsent("channelId", StringUtils.EMPTY);
        params.putIfAbsent("componentId", StringUtils.EMPTY);
        params.putIfAbsent("documentId", StringUtils.EMPTY);
        params.putIfAbsent("documentPath", StringUtils.EMPTY);
        params.putIfAbsent("fieldId", StringUtils.EMPTY);
        params.putIfAbsent("fieldIndex", StringUtils.EMPTY);
        params.putIfAbsent("fieldPath", StringUtils.EMPTY);
        params.putIfAbsent("folderPath", StringUtils.EMPTY);

        final Locale locale = UserSession.get().getLocale();
        params.putIfAbsent("locale", locale != null
                ? locale.getLanguage()
                : LocalizationService.DEFAULT_LOCALE.getLanguage());

        final HippoSession jcrSession = UserSession.get().getJcrSession();
        Node handleNode = null;

        if (params.containsKey("fieldId") && StringUtils.isNotEmpty((CharSequence) params.get("fieldId"))) {
            try {
                final Node fieldNode = jcrSession.getNodeByIdentifier((String) params.get("fieldId"));
                params.put("fieldPath", JcrUtils.getNodePathQuietly(fieldNode));
                handleNode = DocumentUtils.findHandle(fieldNode).orElse(null);
                if (handleNode != null) {
                    params.put("documentId", handleNode.getIdentifier());
                }
            } catch (RepositoryException e) {
                log.error("Error retrieving field node with ID '{}'", params.get("fieldId"), e);
            }
        }

        if (handleNode == null && params.containsKey("documentId")
                && StringUtils.isNotEmpty((CharSequence) params.get("documentId"))) {
            // try to find the document related properties
            final String documentId = (String) params.get("documentId");
            handleNode = DocumentUtils.getHandle(documentId, jcrSession).orElse(null);
        }

        if (handleNode != null) {
            params.put("documentPath", JcrUtils.getNodePathQuietly(handleNode));
            if (params.containsKey("fieldPath") && !((String) params.get("fieldPath")).startsWith("/")) {
                params.put("fieldPath",
                        String.format("%s/%s/%s",
                                JcrUtils.getNodePathQuietly(handleNode),
                                JcrUtils.getNodeNameQuietly(handleNode),
                                params.get("fieldPath")));
            }

            DocumentUtils.getDisplayName(handleNode).ifPresent(s -> params.put("documentName", s));
            DocumentUtils.getVariantNodeType(handleNode).ifPresent(s -> params.put("documentType", s));

            try {
                params.put("folderPath", JcrUtils.getNodePathQuietly(handleNode.getParent()));
            } catch (RepositoryException e) {
                log.error("Error retrieving folder of handle node '{}'", JcrUtils.getNodePathQuietly(handleNode), e);
            }
        }

        return params;
    }
}
