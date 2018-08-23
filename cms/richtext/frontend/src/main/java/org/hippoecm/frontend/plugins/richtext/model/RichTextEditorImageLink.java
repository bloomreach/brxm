/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RichTextEditorImageLink extends RichTextEditorDocumentLink {

    private static final Logger log = LoggerFactory.getLogger(RichTextEditorImageLink.class);

    public static final String URL = "f_url";
    public static final String ALT = "f_alt";
    public static final String ALIGN = "f_align";
    public static final String WIDTH = "f_width";
    public static final String HEIGHT = "f_height";
    public static final String TYPE = "f_type";

    public RichTextEditorImageLink(final Map<String, String> values, final IModel<Node> targetModel) {
        super(values, targetModel);
    }

    public void setUrl(final String url) {
        put(URL, url);
    }

    public String getUrl() {
        return get(URL);
    }

    public void setType(final String type){
        put(TYPE, type);
    }

    public String getType(){
        return get(TYPE);
    }

    @Override
    public void setLinkTarget(final IModel<Node> model) {
        super.setLinkTarget(model);
        if (model != null) {
            trySetUuid(model);

            if (!model.equals(getInitialModel())) {
                put(WIDTH, "");
                put(HEIGHT, "");
            }
        }
    }

    private void trySetUuid(final IModel<Node> model) {
        final Node targetNode = model.getObject();
        try {
            setUuid(targetNode.getIdentifier());
        } catch (final RepositoryException e) {
            log.warn("Cannot get the identifier of linked image '{}'", JcrUtils.getNodePathQuietly(targetNode));
        }
    }
}
