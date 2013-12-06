/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextEditorImageService implements IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RichTextEditorImageService.class);

    private IRichTextImageFactory factory;

    public RichTextEditorImageService(IRichTextImageFactory factory) {
        this.factory = factory;
    }

    public RichTextEditorImageLink createRichTextEditorImage(Map<String, String> p) {
        RichTextImage rti = loadImageItem(p);
        return new RichTextEditorImageLink(p, rti != null ? rti.getTarget() : null) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isValid() {
                return super.isValid() && factory.isValid(getLinkTarget(), getFacetSelectPath());
            }

            @Override
            public void setLinkTarget(IModel<Node> model) {
                super.setLinkTarget(model);
                setFacetSelectPath(factory.getDefaultFacetSelectPath(model));
                setInitType(getType());
            }

            public void save() {
                 if (isAttacheable() || !isSameType(getType())) {
                    try {
                        RichTextImage item = createImageItem(getLinkTarget());
                        final String type = getType();
                        if (!isSameType(type) || !isExisting()) {
                            put(WIDTH, "");
                            put(HEIGHT, "");
                        }
                        item.setSelectedResourceDefinition(type);
                        setUrl(item.getUrl());
                    } catch (RichTextException e) {
                        log.error("Could not create link");
                    }
                }
            }

            public void delete() {
                RichTextImage item = loadImageItem(this);
                if (item != null) {
                    setFacetSelectPath("");
                    setType("");
                    setUrl("");
                    setUuid("");
                }
            }

        };
    }

    public void detach() {
        factory.detach();
    }

    private RichTextImage loadImageItem(Map<String, String> values) {
        String path = values.get(RichTextEditorImageLink.FACET_SELECT);
        if (!Strings.isEmpty(path)) {
            path = RichTextUtil.decode(path);
            try {
                return factory.loadImageItem(path);
            } catch (RichTextException e) {
                log.warn("Could not load rich text image " + path);
            }
        }
        return null;
    }

    private RichTextImage createImageItem(IDetachable nodeModel) throws RichTextException {
        return factory.createImageItem(nodeModel);
    }
}
