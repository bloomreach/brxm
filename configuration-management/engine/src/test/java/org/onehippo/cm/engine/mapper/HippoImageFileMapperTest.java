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
package org.onehippo.cm.engine.mapper;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ConfigSourceImpl;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        Optional<String> result = testedClass.apply(value);
        assertTrue(result.isPresent());
        assertEquals(PATH_TO_IMAGE_NODE_IMAGE_NODE_PNG, result.get());
    }

    @Test
    public void testApplyArrayValue() {
        Optional<String> arrayValueName = testedClass.apply(arrValue2);
        assertTrue(arrayValueName.isPresent());
        assertEquals(PATH_TO_JPG_ARR_VALUE_2, arrayValueName.get());
    }

    @Test
    public void testApplyImageSetValue() {
        Optional<String> imageSetResult = testedClass.apply(giValue);
        assertTrue(imageSetResult.isPresent());
        assertEquals(PATH_TO_GIF_GALLERY_IMAGE, imageSetResult.get());
    }

    @Test
    public void testApplyImageSetArrayValue() {
        Optional<String> imageSetArrayResult = testedClass.apply(garrValue0);
        assertTrue(imageSetArrayResult.isPresent());
        assertEquals(PATH_TO_GIF_GALLERY_IMAGE_ARR, imageSetArrayResult.get());
    }

    private void setupNodesWithProperties() {
        GroupImpl configuration = new GroupImpl("dummyConfiguration");
        ProjectImpl project = new ProjectImpl("dummyProject", configuration);
        ModuleImpl module = new ModuleImpl("dummyModule", project);
        SourceImpl source = new ConfigSourceImpl("somePath", module);
        Definition definition = new ConfigDefinitionImpl(source);
        DefinitionNodeImpl parentNode = new DefinitionNodeImpl("/path/to/", "parentNode", definition);

        DefinitionNodeImpl childNode1 = new DefinitionNodeImpl("pngImageNode", parentNode);
        value = new ValueImpl("Dummy value");
        childNode1.addProperty("dummyProperty", value);
        childNode1.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/png"));
        ValueImpl jcrTypeValue = new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGE);
        childNode1.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, jcrTypeValue);

        DefinitionNodeImpl childNode2 = new DefinitionNodeImpl("jpgImageNode", parentNode);
        childNode2.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/jpg"));
        childNode2.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, jcrTypeValue);
        ValueImpl arrValue0 = new ValueImpl("v1".getBytes());
        ValueImpl arrValue1 = new ValueImpl("v2".getBytes());
        arrValue2 = new ValueImpl("v3".getBytes());
        childNode2.addProperty("dataProperty", ValueType.BINARY, new ValueImpl[]{arrValue0, arrValue1, arrValue2});

        DefinitionNodeImpl imageSetNode = new DefinitionNodeImpl("imageSetNode", parentNode);
        ValueImpl imageSetTypevalue = new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGESET);
        imageSetNode.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, imageSetTypevalue);

        DefinitionNodeImpl galleryImage = new DefinitionNodeImpl("galleryImage", imageSetNode);
        galleryImage.addProperty(AbstractFileMapper.JCR_MIME_TYPE, new ValueImpl("image/gif"));
        galleryImage.addProperty(AbstractFileMapper.JCR_PRIMARY_TYPE, new ValueImpl(HippoImageFileMapper.HIPPOGALLERY_IMAGE));

        giValue = new ValueImpl("dummy");
        galleryImage.addProperty("dummy", giValue);

        garrValue0 = new ValueImpl("v1".getBytes());
        ValueImpl garrValue1 = new ValueImpl("v2".getBytes());
        ValueImpl garrValue2 = new ValueImpl("v3".getBytes());

        galleryImage.addProperty("dataProperty", ValueType.BINARY, new ValueImpl[]{garrValue0, garrValue1, garrValue2});
    }
}