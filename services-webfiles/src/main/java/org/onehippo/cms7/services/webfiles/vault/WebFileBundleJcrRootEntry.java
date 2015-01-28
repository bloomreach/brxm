/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.vault;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.jackrabbit.vault.fs.io.Archive;

/**
 * Decorates a jcr_root entry and returns its first child decorated with a
 * {@link WebFileBundleRootEntry}.
 */
class WebFileBundleJcrRootEntry implements Archive.Entry {

    private final Archive.Entry decoratedJcrRoot;
    private final Archive.Entry bundleRoot;

    WebFileBundleJcrRootEntry(Archive.Entry decoratedJcrRoot) {
        this.decoratedJcrRoot = decoratedJcrRoot;

        final Iterator<? extends Archive.Entry> decoratedChildIterator = decoratedJcrRoot.getChildren().iterator();
        if (decoratedChildIterator.hasNext()) {
            bundleRoot = new WebFileBundleRootEntry(decoratedChildIterator.next());
        } else {
            bundleRoot = null;
        }
    }

    @Override
    public String getName() {
        return decoratedJcrRoot.getName();
    }

    @Override
    public boolean isDirectory() {
        return decoratedJcrRoot.isDirectory();
    }

    @Override
    public Collection<? extends Archive.Entry> getChildren() {
        if (bundleRoot != null) {
            return Collections.singleton(bundleRoot);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Archive.Entry getChild(final String name) {
        if (bundleRoot != null && bundleRoot.getName().equals(name)) {
            return bundleRoot;
        }
        return null;
    }
}
