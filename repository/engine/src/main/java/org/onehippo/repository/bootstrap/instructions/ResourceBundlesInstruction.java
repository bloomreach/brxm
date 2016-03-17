/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;
import org.onehippo.repository.bootstrap.util.BundleFileInfo;
import org.onehippo.repository.bootstrap.util.BundleInfo;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RESOURCEBUNDLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;

public class ResourceBundlesInstruction extends InitializeInstruction {

    private static final String TRANSLATIONS_PATH = "/hippo:configuration/hippo:translations";
    private BundleFileInfo bundleFileInfo;
    private Collection<String> contextPaths;

    public ResourceBundlesInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected void initializeItem() throws RepositoryException {
        final Node itemNode = this.item.getItemNode();
        final Collection<String> contextPaths = getContextPaths();
        itemNode.setProperty(HIPPO_CONTEXTPATHS, contextPaths.toArray(new String[contextPaths.size()]));
    }

    @Override
    protected boolean isDownstream(final String[] reloadPaths) throws RepositoryException {
        final Collection<String> contextPaths = getContextPaths();
        for (String reloadPath : reloadPaths) {
            for (String contextPath : contextPaths) {
                if (reloadPath.equals(contextPath) || contextPath.startsWith(reloadPath + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Collection<String> getContextPaths() throws RepositoryException {
        if (contextPaths == null) {
            contextPaths = new HashSet<>();
            for (BundleInfo bundleInfo : getBundleFileInfo().getBundleInfos()) {
                contextPaths.add(TRANSLATIONS_PATH + '/'
                        + StringUtils.replaceChars(bundleInfo.getName(), '.', '/')
                        + '/' + bundleInfo.getLocale().toString());
            }
        }
        return contextPaths;
    }

    private BundleFileInfo getBundleFileInfo() throws RepositoryException {
        if (bundleFileInfo == null) {
            try (final InputStream in = item.getResourceBundlesURL().openStream()) {
                bundleFileInfo = BundleFileInfo.readInfo(in);
            } catch (IOException e) {
                throw new RepositoryException("Failed to read bundle file", e);
            }
        }
        return bundleFileInfo;
    }

    @Override
    protected String getName() {
        return HIPPO_RESOURCEBUNDLES;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final Collection<String> contextPaths = new ArrayList<>();
        if (item.isDownstreamItem()) {
            for (InitializeItem upstreamItem : item.getUpstreamItems()) {
                contextPaths.addAll(Arrays.asList(upstreamItem.getContextPaths()));
            }
        }
        final Collection<BundleInfo> bundleInfos = getBundleFileInfo().getBundleInfos();
        if (!contextPaths.isEmpty()) {
            final Iterator<BundleInfo> bundlesInfoIterator = bundleInfos.iterator();
            while (bundlesInfoIterator.hasNext()) {
                if (!isInContext(bundlesInfoIterator.next(), contextPaths)) {
                    bundlesInfoIterator.remove();
                }
            }
        }
        for (BundleInfo bundleInfo : bundleInfos) {
            getOrCreateResourceBundle(bundleInfo);
        }
        session.save();
        return null;
    }

    private boolean isInContext(final BundleInfo bundleInfo, final Collection<String> contextPaths) {
        for (String contextPath : contextPaths) {
            final String relpath = contextPath.substring(TRANSLATIONS_PATH.length() + 1);
            if (relpath.equals(bundleInfo.getBundlePath())) {
                return true;
            }
        }
        return false;
    }

    private void getOrCreateResourceBundle(final BundleInfo bundleInfo) throws RepositoryException {
        final Node bundles = getOrCreateResourceBundles(bundleInfo.getName());
        final Node bundle = getOrCreateNode(bundleInfo.getLocale().toString(), bundles, NT_RESOURCEBUNDLE);
        for (Map.Entry<String, String> entry : bundleInfo.getTranslations().entrySet()) {
            bundle.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private Node getOrCreateResourceBundles(final String name) throws RepositoryException {
        Node node = session.getNode(TRANSLATIONS_PATH);
        final String[] pathElements = StringUtils.split(name, '.');
        for (String pathElement : pathElements) {
            node = getOrCreateNode(pathElement, node, NT_RESOURCEBUNDLES);
        }
        return node;
    }

    private Node getOrCreateNode(final String name, final Node resourceBundles, final String type) throws RepositoryException {
        if (resourceBundles.hasNode(name)) {
            return resourceBundles.getNode(name);
        } else {
            return resourceBundles.addNode(name, type);
        }
    }

}
