/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugins.richtext.processor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.onehippo.cms7.services.processor.richtext.RichTextException;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImage;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextEditorImageService implements IDetachable {

    static final Logger log = LoggerFactory.getLogger(RichTextEditorImageService.class);

    private RichTextImageFactory factory;

    public RichTextEditorImageService(RichTextImageFactory factory) {
        this.factory = factory;
    }

    public RichTextEditorImageLink createRichTextEditorImage(Map<String, String> p) {
        final RichTextImage rti = loadImageItem(p);
        final String path = rti != null ? rti.getPath() : null;
        final IModel<Node> parentModel = path != null ? new JcrNodeModel(path).getParentModel() : null;

        return new RichTextEditorImageLink(p, parentModel) {

            @Override
            public boolean isValid() {
                return super.isValid() && factory.isValid(WicketModel.of(getLinkTarget()));
            }

            @Override
            public void setLinkTarget(IModel<Node> model) {
                super.setLinkTarget(model);
                setInitType(getType());
            }

            @Override
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

            @Override
            public void delete() {
                RichTextImage item = loadImageItem(this);
                if (item != null) {
                    setType("");
                    setUrl("");
                    setUuid("");
                }
            }
        };
    }

    @Override
    public void detach() {
        factory.release();
    }

    private RichTextImage loadImageItem(Map<String, String> values) {
        final String uuid = values.get(RichTextEditorImageLink.UUID);
        if (!Strings.isEmpty(uuid)) {
            final String type = values.get(RichTextEditorImageLink.TYPE);
            try {
                return factory.loadImageItem(uuid, type);
            } catch (RichTextException e) {
                log.warn("Could not load rich text image " + uuid);
            }
        }
        return null;
    }

    private RichTextImage createImageItem(IModel<Node> nodeModel) throws RichTextException {
        return factory.createImageItem(WicketModel.of(nodeModel));
    }
}
