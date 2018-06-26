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

import javax.jcr.Session;

import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImageLinkFieldType extends LinkFieldType {

    private static final Logger log = LoggerFactory.getLogger(ImageLinkFieldType.class);

    private static final String[] IMAGE_PICKER_STRING_PROPERTIES = {
            "base.uuid",
            "cluster.name",
            "enable.upload",
            "last.visited.enabled",
            "last.visited.key",
    };
    private static final String[] IMAGE_PICKER_MULTIPLE_STRING_PROPERTIES = {
            "nodetypes",
    };

    private static final String IMAGE_PICKER_PROPERTY_PREFIX = "image.";
    private static final String[] IMAGE_PICKER_PREFIXED_STRING_PROPERTIES = {
            IMAGE_PICKER_PROPERTY_PREFIX + "validator.id"
    };

    private static final ImageItemFactory IMAGE_ITEM_FACTORY = new ImageItemFactory();

    private ObjectNode config;
    private ImageItemFactory imageItemFactory;

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

        final ObjectNode imagePickerConfig = new FieldTypeConfig(fieldContext)
                .strings(IMAGE_PICKER_STRING_PROPERTIES)
                .multipleStrings(IMAGE_PICKER_MULTIPLE_STRING_PROPERTIES)
                .removePrefix(IMAGE_PICKER_PROPERTY_PREFIX)
                .strings(IMAGE_PICKER_PREFIXED_STRING_PROPERTIES)
                .build();
        config.set("imagepicker", imagePickerConfig);

        return super.init(fieldContext);
    }

    @Override
    protected String createUrl(final String uuid, final Session session) {
        final ImageItem imageItem = imageItemFactory.createImageItem(uuid);
        return imageItem.getPrimaryUrl(() -> session);
    }

}
