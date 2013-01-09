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

import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Resource;
import org.apache.wicket.model.IModel;

/**
 * Descriptor for layout plugins (i.e. html only).  The extension points
 * of the plugin correspond to pads in the layout.
 */
public interface ILayoutDescriptor extends IClusterable {

    /**
     * The "class" name of the layout.  When there is a plugin corresponding
     * to the layout, it's name will be used.
     * @return the name of the plugin class
     */
    String getPluginClass();

    /**
     * The markup variant.
     * @return the variant
     */
    String getVariant();

    /**
     * The localized name of the layout.
     * @return
     */
    IModel<String> getName();
    
    Resource getIcon();

    Map<String, ILayoutPad> getLayoutPads();

}
