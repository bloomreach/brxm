/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.builder;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.editor.builder.IEditorContext.Mode;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Context for builder plugins.  Provides the configuration of
 * the plugin that must be edited to the plugin doing the editing.
 */
public interface IBuilderContext extends IClusterable {

    /**
     * @return editable plugin config
     */
    IPluginConfig getEditablePluginConfig();

    /**
     * @return the mode of the editor (VIEW or EDIT)
     */
    Mode getMode();
    
    /**
     * Deletes the plugin.  Context is invalid after this method has been called.
     */
    void delete();
    
    /**
     * Sets focus to the render service
     */
    void focus();

    /**
     * register a listener with the builder context.
     * 
     * @param listener
     */
    void addBuilderListener(IBuilderListener listener);

    /**
     * remove a listener from the builder
     * 
     * @param listener
     */
    void removeBuilderListener(IBuilderListener listener);
}
