/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation.PickerType.DOCUMENTS;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation.PickerType.PAGES;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation.Type.DOCUMENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation.Type.FOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TreePickerRepresentationGetterSetterTest {

    @Test
    public void TreePickerRepresentation_pojo_test() {
        TreePickerRepresentation presentation = new TreePickerRepresentation();
        assertNotNull(presentation.getItems());
        assertEquals(0, presentation.getItems().size());
        assertNull(presentation.getState());
        assertTrue("Default collapsed", presentation.isCollapsed());
        assertEquals(DOCUMENTS.getName(), presentation.getPickerType());
        assertEquals(FOLDER.getName(), presentation.getType());
        assertFalse(presentation.isExpandable());

        presentation.setExpandable(true);
        presentation.setDisplayName("DisplayName");
        presentation.setId("my-uuid");
        List<TreePickerRepresentation> items = new ArrayList<>();
        items.add(new TreePickerRepresentation());
        items.add(new TreePickerRepresentation());
        presentation.setItems(items);
        presentation.setNodeName("nodeName");
        presentation.setPathInfo("/foo/bar");
        presentation.setSelectable(true);
        presentation.setCollapsed(false);
        presentation.setSelected(true);
        presentation.setState("new");


        assertTrue(presentation.isExpandable());
        assertEquals("DisplayName", presentation.getDisplayName());
        assertEquals("my-uuid", presentation.getId());
        assertEquals(2, presentation.getItems().size());
        assertEquals("nodeName", presentation.getNodeName());
        assertEquals("/foo/bar", presentation.getPathInfo());
        assertTrue(presentation.isSelectable());
        assertTrue(presentation.isSelected());
        assertFalse(presentation.isCollapsed());
        assertEquals("new", presentation.getState());

        presentation.setType(DOCUMENT.getName());
        assertEquals(DOCUMENT.getName(), presentation.getType());

        presentation.setType("non-existing");
        assertEquals(FOLDER.getName(), presentation.getType());


        presentation.setPickerType("non-existing");
        assertEquals(DOCUMENTS.getName(), presentation.getPickerType());

        presentation.setPickerType("pages");
        assertEquals(PAGES.getName(), presentation.getPickerType());

        presentation.setPickerType("documents");
        assertEquals(DOCUMENTS.getName(), presentation.getPickerType());

    }
}
