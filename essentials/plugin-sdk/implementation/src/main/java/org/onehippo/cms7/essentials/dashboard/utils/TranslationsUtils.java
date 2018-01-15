/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.repository.bootstrap.util.BundleFileInfo;
import org.onehippo.repository.bootstrap.util.BundleInfo;

import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;

public class TranslationsUtils {

    private static final String TRANSLATIONS_PATH = "/hippo:configuration/hippo:translations";

    private TranslationsUtils() {}

    public static void importTranslations(String json, Session session) throws RepositoryException, IOException {
        try (final InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            for (BundleInfo bundleInfo : BundleFileInfo.readInfo(in).getBundleInfos()) {
                getOrCreateResourceBundle(bundleInfo, session);
            }
        }
    }

    private static void getOrCreateResourceBundle(final BundleInfo bundleInfo, final Session session)
            throws RepositoryException {
        final Node bundles = getOrCreateResourceBundles(bundleInfo.getName(), session);
        final Node bundle = getOrCreateNode(bundleInfo.getLocale().toString(), bundles, NT_RESOURCEBUNDLE);
        for (Map.Entry<String, String> entry : bundleInfo.getTranslations().entrySet()) {
            bundle.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static Node getOrCreateResourceBundles(final String name, final Session session)
            throws RepositoryException {
        Node node = session.getNode(TRANSLATIONS_PATH);
        final String[] pathElements = StringUtils.split(name, '.');
        for (String pathElement : pathElements) {
            node = getOrCreateNode(pathElement, node, NT_RESOURCEBUNDLES);
        }
        return node;
    }

    private static Node getOrCreateNode(final String name, final Node resourceBundles, final String type)
            throws RepositoryException {
        if (resourceBundles.hasNode(name)) {
            return resourceBundles.getNode(name);
        } else {
            return resourceBundles.addNode(name, type);
        }
    }

}
