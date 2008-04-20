/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.service;

import java.util.List;

import org.hippoecm.frontend.application.PluginRequestTarget;

public interface IRenderService {

    void render(PluginRequestTarget target);

    void focus(IRenderService child);

    // Rendering hierarchy management

    void bind(IRenderService parent, String id);

    void unbind();

    String getId();

    IRenderService getParentService();

    List<IRenderService> getChildServices(String name);

    // Service id that is used to register decorators

    String getDecoratorId();

    // Service ids that can be used to hook into the layout

    List<String> getExtensionPoints();
}
