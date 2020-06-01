/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.layout;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.wicket.model.IModel;
import org.junit.Test;

public class LayoutProviderTest {

    @Test
    public void testExtensionLoader() {
        final ClassLoader loader = getClass().getClassLoader();
        LayoutProvider provider = new LayoutProvider(new ClassLoaderModel(loader));
        List<String> layouts = provider.getLayouts();
        assertTrue("Test layout not found", layouts.contains("org.hippoecm.frontend.editor.layout.Test"));
    }

    @Test
    public void testNullClassLoaderDoesNotThrowException() {
        new LayoutProvider(new ClassLoaderModel(null));
    }

    private class ClassLoaderModel implements IModel<ClassLoader> {

        private transient ClassLoader loader;

        private ClassLoaderModel(ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        public ClassLoader getObject() {
            return loader;
        }

        @Override
        public void setObject(final ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        public void detach() {
            this.loader = null;
        }

    }

}
