/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.HashSet;
import java.util.Set;

import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DocumentRepresentationTest {

    @Test
    public void test_DocumentRepresentation_creation() {

        DocumentRepresentation presentation = new DocumentRepresentation(
                "News/News1",
                "/unittestcontent/documents/unittestproject", "News One", true, true);

        assertEquals("News/News1", presentation.getPath());
        assertEquals("News One", presentation.getDisplayName());
    }

    @Test
    public void test_DocumentRepresentation_equals_and_hashCode_based_on_getPath_and_contentRoot() {

        Set<DocumentRepresentation> testSet = new HashSet<>();
        DocumentRepresentation presentation1 = new DocumentRepresentation(
                "News/News1",
                "/unittestcontent/documents/unittestproject", "News One", true, true);


        DocumentRepresentation presentation2 = new DocumentRepresentation(
                "News/News1",
                "/unittestcontent/documents/unittestproject", "News DIFFERENT NAME", true, true);

        testSet.add(presentation1);
        testSet.add(presentation2);

        assertEquals("Different Display name should not matter",1, testSet.size());


        DocumentRepresentation presentationDifferentRootContent = new DocumentRepresentation(
                "News/News1",
                "/unittestcontent/documents", "News One", true, true);


        testSet.add(presentationDifferentRootContent);
        assertEquals("presentationDifferentRootContent should not be equal because different root content",2, testSet.size());


    }

}
