/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.sitemenu;

import java.util.List;

public interface EditableMenu extends CommonMenu{

    /**
     * note: the method should have been called getSelectedMenuItem for because of an 
     * historical type it is called getSelectMenuItem
     * @return the selected {@link EditableMenuItem} and <code>null</code> if none selected
     */
    EditableMenuItem getSelectMenuItem();

    /**
     * sets the <code>selectedMenuItem</code> as selected
     * @param selectedMenuItem the {@link EditableMenuItem} that is selected
     */
    void setSelectedMenuItem(EditableMenuItem selectedMenuItem);

    /**
     * 
     * @return the {@link List} of root {@link EditableMenuItem}s for this {@link EditableMenu}
     */
    List<EditableMenuItem> getMenuItems();

    /**
     * 
     * @return the backing {@link HstSiteMenus} for this {@link EditableMenu}
     */
    HstSiteMenus getHstSiteMenus();

    /**
     * @return the deepest expanded {@link EditableMenuItem} and <code>null</code> if none
     * of the items are expanded
     */
    EditableMenuItem getDeepestExpandedItem();
}
