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
package org.onehippo.cm.model.impl.tree;

import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ModelItem;

public abstract class ModelItemImpl implements ModelItem {

    protected JcrPathSegment name;

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public JcrPathSegment getJcrName() {
        return name;
    }

    public void setName(final String name) {
        this.name = JcrPaths.getSegment(name);
    }

    public void setName(final JcrPathSegment name) {
        if (name == null) {
            throw new IllegalArgumentException("Item name must not be null!");
        }
        this.name = name;
    }

    public abstract JcrPath getJcrPath();

    public String toString() {
        return getClass().getSimpleName()+"{path='"+ getPath()+"'}";
    }
}
