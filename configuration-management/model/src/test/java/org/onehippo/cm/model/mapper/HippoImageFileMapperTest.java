/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.mapper;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.tree.ValueType;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HippoImageFileMapperTest {

    private static final String PATH_TO_IMAGE_NODE_IMAGE_NODE_PNG = "/path/to/pngImageNode/pngImageNode.png";
    private static final String PATH_TO_JPG_ARR_VALUE_2 = "/path/to/jpgImageNode/jpgImageNode[2].jpg";
    private static final String PATH_TO_GIF_GALLERY_IMAGE = "/path/to/imageSetNode_galleryImage.gif";
    private static final String PATH_TO_GIF_GALLERY_IMAGE_ARR = "/path/to/imageSetNode_galleryImage[0].gif";

    private HippoImageFileMapper testedClass = new HippoImageFileMapper();

    private ValueImpl value;
    private ValueImpl arrValue2;
    private ValueImpl giValue;
    private ValueImpl garrValue0;

    @Before
    public void setUp() {
        setupNodesWithProperties();
    }

    @Test
    public void testApply() {
        String result = testedClass.apply(value);
        assertNotNull(result);
        assertEquals(PATH_TO_IMAGE_NODE_IMAGE_NODE_PNG, result);
    }

    @Test
    public void testApplyArrayValue() {
        String arrayValueName = testedClass.apply(arrValue2);
        assertNotNull(arrayValueName);
        assertEquals(PATH_TO_JPG_ARR_VALUE_2, arrayValueName);
    }

    @Test
    public void testApplyImageSetValue() {
        String imageSetResult = testedClass.apply(giValue);
        assertNotNull(imageSetResult);
        assertEquals(PATH_TO_GIF_GALLERY_IMAGE, imageSetResult);
    }

    @Test
    public void testApplyImageSetArrayValue() {
        String imageSetArrayResult = testedClass.apply(garrValue0);
        assertNotNull(imageSetArrayResult);
        assertEquals(PATH_TO_GIF_GALLERY_IMAGE_ARR, imageSetArrayResult);
    }

    private void setupNodesWithProperties() {
        GroupImpl group = new GroupImpl("dummyGroup");
        ProjectImpl project = new ProjectImpl("dummyProject", group);
        ModuleImpl module = new ModuleImpl("dummyModule", project);
        ConfigSourceImpl source = new ConfigSourceImpl("somePath", module);
        ConfigDefinitionImpl definition = new ConfigDefinitionImpl(source);
        DefinitionNodeImpl parentNode = new DefinitionNodeImpl("/path/to/", "parentNode", definition);

        DefinitionNodeImpl childNode1 = new DefinitionNodeImpl("pngImageNode", parentNode);
        value = new ValueImpl("Dummy value");
        childNode1.addProperty("dummyProperty", value);
        childNode1.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/png"));
        ValueImpl jcrTypeValue = new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGE.getName());
        childNode1.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, jcrTypeValue);

        DefinitionNodeImpl childNode2 = new DefinitionNodeImpl("jpgImageNode", parentNode);
        childNode2.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/jpg"));
        childNode2.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, jcrTypeValue);
        ValueImpl arrValue0 = new ValueImpl("v1".getBytes());
        ValueImpl arrValue1 = new ValueImpl("v2".getBytes());
        arrValue2 = new ValueImpl("v3".getBytes());
        childNode2.addProperty("dataProperty", ValueType.BINARY, asList(arrValue0, arrValue1, arrValue2));

        DefinitionNodeImpl imageSetNode = new DefinitionNodeImpl("imageSetNode", parentNode);
        ValueImpl imageSetTypevalue = new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGESET.getName());
        imageSetNode.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, imageSetTypevalue);

        DefinitionNodeImpl galleryImage = new DefinitionNodeImpl("galleryImage", imageSetNode);
        galleryImage.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/gif"));
        galleryImage.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE,
                new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGE.getName()));

        giValue = new ValueImpl("dummy");
        galleryImage.addProperty("dummy", giValue);

        garrValue0 = new ValueImpl("v1".getBytes());
        ValueImpl garrValue1 = new ValueImpl("v2".getBytes());
        ValueImpl garrValue2 = new ValueImpl("v3".getBytes());

        galleryImage.addProperty("dataProperty", ValueType.BINARY, asList(garrValue0, garrValue1, garrValue2));
    }
}