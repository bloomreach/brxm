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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.wicket.util.collections.MiniMap;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.picker.ImagePicker;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImageLinkFieldType extends LinkFieldType {

    private static final ImageItemFactory IMAGE_ITEM_FACTORY = new ImageItemFactory();

    private ObjectNode config;
    private final ImageItemFactory imageItemFactory;

    public ImageLinkFieldType() {
        this(IMAGE_ITEM_FACTORY);
    }

    ImageLinkFieldType(final ImageItemFactory imageItemFactory) {
        setType(Type.IMAGE_LINK);
        this.imageItemFactory = imageItemFactory;
    }

    public ObjectNode getConfig() {
        return config;
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        config = Json.object();
        config.set("imagepicker", ImagePicker.build(fieldContext));

        return super.init(fieldContext);
    }

    @Override
    protected Map<String, Object> createMetadata(final String uuid, final Node node, final Session session) {
        final MiniMap<String, Object> map = new MiniMap<>(1);
        map.put("url", getImageUrl(uuid, session));
        return map;
    }

    private String getImageUrl(final String uuid, final Session session) {
        final ImageItem imageItem = imageItemFactory.createImageItem(uuid);
        return imageItem.getPrimaryUrl(() -> session);
    }
}
