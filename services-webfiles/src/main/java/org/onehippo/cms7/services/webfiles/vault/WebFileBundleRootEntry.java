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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.jackrabbit.vault.fs.io.Archive;

/**
 * Decorates an {@link Archive.Entry} and includes a child entry for a default ".content.xml" file
 * if it does not exist as a child yet.
 */
class WebFileBundleRootEntry implements Archive.Entry {

    private final Archive.Entry decorated;

    WebFileBundleRootEntry(Archive.Entry decorated) {
        this.decorated = decorated;
    }

    @Override
    public String getName() {
        return decorated.getName();
    }

    @Override
    public boolean isDirectory() {
        return decorated.isDirectory();
    }

    @Override
    public Collection<? extends Archive.Entry> getChildren() {
        final Collection<? extends Archive.Entry> children = decorated.getChildren();
        final Archive.Entry defaultContentXml = DefaultBundleRootContentXmlEntry.getInstance();
        if (children == null) {
            return Collections.singleton(defaultContentXml);
        }
        for (Archive.Entry child : children) {
            if (child.getName().equals(defaultContentXml.getName())) {
                // use the custom .content.xml file at root level
                return children;
            }
        }
        // include the default .content.xml file at root level
        final ArrayList<Archive.Entry> childrenPlusDefaultContentXml = new ArrayList(children.size() + 1);
        childrenPlusDefaultContentXml.addAll(children);
        childrenPlusDefaultContentXml.add(defaultContentXml);
        return childrenPlusDefaultContentXml;
    }

    @Override
    public Archive.Entry getChild(final String name) {
        Archive.Entry child = decorated.getChild(name);
        if (child == null) {
            final Archive.Entry defaultContentXml = DefaultBundleRootContentXmlEntry.getInstance();
            if (name.equals(defaultContentXml.getName())) {
                child = defaultContentXml;
            }
        }
        return child;
    }
}
