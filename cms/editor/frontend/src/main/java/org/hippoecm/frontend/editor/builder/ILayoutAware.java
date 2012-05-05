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

import java.util.List;

import org.hippoecm.frontend.editor.layout.ILayoutContext;
import org.hippoecm.frontend.service.IRenderService;

/**
 * Interface to declare a render service to be layout aware.
 * When a render service marked with this interface is registered
 * in the template builder, it can control its position in the
 * layout by addressing the {@link org.hippoecm.frontend.editor.layout.ILayoutContext}.
 */
public interface ILayoutAware extends IRenderService {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    void setLayoutContext(ILayoutContext control);

    ILayoutAware getDefaultChild();

    List<ILayoutAware> getChildren();

    String getTemplateBuilderPluginId();

    String getTemplateBuilderExtensionPoint();
}
