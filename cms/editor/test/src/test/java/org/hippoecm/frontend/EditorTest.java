/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import org.hippoecm.frontend.editor.builder.TemplateBuilderTest;
import org.hippoecm.frontend.editor.field.FieldPluginTest;
import org.hippoecm.frontend.editor.impl.DefaultEditorFactoryTest;
import org.hippoecm.frontend.editor.impl.JcrTemplateStoreTest;
import org.hippoecm.frontend.editor.layout.LayoutDescriptorTest;
import org.hippoecm.frontend.editor.layout.TwoColumnTest;
import org.hippoecm.frontend.editor.tools.JcrTypeDescriptorTest;
import org.hippoecm.frontend.editor.validator.ValidationPluginTest;
import org.hippoecm.frontend.types.TypeStoreTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(PluginSuite.class)
@Suite.SuiteClasses({
    JcrTypeDescriptorTest.class,
    ValidationPluginTest.class,
    FieldPluginTest.class,
    TemplateBuilderTest.class,
    TwoColumnTest.class,
    LayoutDescriptorTest.class,
    DefaultEditorFactoryTest.class,
    JcrTemplateStoreTest.class,
    TypeStoreTest.class
})
public class EditorTest {

}
