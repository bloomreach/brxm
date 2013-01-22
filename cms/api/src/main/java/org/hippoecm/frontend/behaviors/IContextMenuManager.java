/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.IBehavior;

/**
 * Marker interface for IContextMenuManager {@link Component}s.  All {@link IContextMenu}s
 * that are contained in the component hierarchy will be closed when the context menu
 * manager is notified.  Other context menu managers (and their sub-trees) are excluded
 * from the hierarchical search.
 * <p>
 * The interface can be set on a {@link Component} or one of it's {@link IBehavior}s.
 * When an ancestor context menu manager searches for context menu's, it 
 */
public interface IContextMenuManager {

    void showContextMenu(IContextMenu active);

    void collapseAllContextMenus();
}
