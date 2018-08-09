/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.types.ITypeDescriptor;

public interface ITemplateEngine extends IClusterable {

    /**
     * The configuration key to use when locating the template engine service.
     */
    String ENGINE = "engine";

    String MODE = "mode";

    ITypeDescriptor getType(String type) throws TemplateEngineException;

    ITypeDescriptor getType(IModel<?> model) throws TemplateEngineException;

    IClusterConfig getTemplate(ITypeDescriptor type, IEditor.Mode mode) throws TemplateEngineException;

    IModel<?> getPrototype(ITypeDescriptor type) throws TemplateEngineException;

    List<String> getEditableTypes();

}
