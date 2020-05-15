/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;

public class LastVisited implements IClusterable {

    public static final String MODEL_ID = "LAST_VISITED_MODEL";

    public static LastVisited document(final IModel<Node> model) {
        final String path = JcrUtils.getNodePathQuietly(model.getObject());
        final String label = getDocumentName(model);
        return new LastVisited(path, label);
    }

    public static LastVisited folder(final IModel<Node> model) {
        final String path = JcrUtils.getNodePathQuietly(model.getObject());
        return new LastVisited(path, StringUtils.EMPTY);
    }

    private static String getDocumentName(final IModel<Node> model) {
        try {
            final IModel<String> nameModel = DocumentUtils.getDocumentNameModel(model);
            if (nameModel != null) {
                return nameModel.getObject();
            }
        } catch (final RepositoryException ignored) {
        }
        return StringUtils.EMPTY;
    }

    private final String label;
    private final String path;

    LastVisited(final String path, final String label) {
        this.path = path;
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LastVisited that = (LastVisited) o;
        return Objects.equals(getLabel(), that.getLabel()) &&
                Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel(), getPath());
    }
}
