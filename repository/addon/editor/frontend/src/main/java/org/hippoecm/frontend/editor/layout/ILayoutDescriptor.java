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
package org.hippoecm.frontend.editor.layout;

import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.util.resource.IResourceStream;

/**
 * Descriptor for layout plugins (i.e. html only).  The extension points
 * of the plugin correspond to pads in the layout.
 */
public interface ILayoutDescriptor extends IClusterable {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    String getPluginClass();

    // FIXME: abstract away to a Resource
    IResourceStream getIcon();

    Map<String, ILayoutPad> getLayoutPads();

}
