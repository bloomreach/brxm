/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hippoecm.hst.content.annotations.Persistable;
import org.junit.Test;

/**
 * TestHstComponentMetadataReader
 */
public class TestHstComponentMetadataReader {

    @Test
    public void testMetadataReading() throws Exception {
        HstComponentMetadata metadata = HstComponentMetadataReader.getHstComponentMetadata(getClass().getClassLoader(), DetailComponent.class.getName());

        assertNotNull(metadata);
        assertFalse(metadata.hasMethodAnnotatedBy(Persistable.class.getName(), "doBeforeRender"));
        assertTrue(metadata.hasMethodAnnotatedBy(Persistable.class.getName(), "doAction"));
        assertFalse(metadata.hasMethodAnnotatedBy(Persistable.class.getName(), "doBeforeServeResource"));
    }

    public static class DetailComponent extends GenericHstComponent {
        @Persistable
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }
    }

}
