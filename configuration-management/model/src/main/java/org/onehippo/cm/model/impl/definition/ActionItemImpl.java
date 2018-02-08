/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.definition;

import java.util.Objects;

import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;

public class ActionItemImpl implements ActionItem {

    private final JcrPath path;
    private final ActionType type;

    public ActionItemImpl(final String path, final ActionType type) {
        this.path = JcrPaths.getPath(path);
        this.type = type;
    }

    @Override public JcrPath getPath() {
        return path;
    }

    @Override public ActionType getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActionItem)) {
            return false;
        }
        final ActionItem that = (ActionItem) o;
        return Objects.equals(type, that.getType()) && Objects.equals(path, that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, type);
    }

    @Override
    public String toString() {
        return "ActionItem{" +
                "path='" + path + '\'' +
                ", type=" + type +
                '}';
    }
}
