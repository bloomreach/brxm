/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.xinha.services.images;

import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaImageService implements IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaImageService.class);

    private String editorId;
    private IRichTextImageFactory factory;

    public XinhaImageService(IRichTextImageFactory factory, String editorId) {
        this.factory = factory;
        this.editorId = editorId;
    }

    //Attach an image with only a JcrNodeModel. Method return json object wich 
    public String attach(JcrNodeModel model) {
        //TODO: fix drag-drop replacing
        try {
            RichTextImage item = createImageItem(model);
            StringBuilder sb = new StringBuilder(80);
            sb.append("xinha_editors.").append(editorId).append(".plugins.InsertImage.instance.insertImage(");
            sb.append("{ ");
            sb.append(XinhaImage.URL).append(": '").append(item.getUrl()).append("'");
            sb.append(", ").append(XinhaImage.FACET_SELECT).append(": '").append(item.getFacetSelectPath()).append("'");
            sb.append(" }, false)");
            return sb.toString();
        } catch (RichTextException e) {
            log.error("Could not attach model ", e);
        }
        return null;
    }

    public XinhaImage createXinhaImage(Map<String, String> p) {
        RichTextImage rti = loadImageItem(p);
        return new XinhaImage(p, rti != null ? rti.getTarget() : null) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isValid() {
                return super.isValid() && factory.isValid(getLinkTarget(), getFacetSelectPath());
            }

            @Override
            public void setLinkTarget(IDetachable model) {
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
                            item.setSelectedResourceDefinition(type);
                        }
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
                    setUrl("");
                }
            }

        };
    }

    public void detach() {
        factory.detach();
    }

    private RichTextImage loadImageItem(Map<String, String> values) {
        String path = values.get(XinhaImage.FACET_SELECT);
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
