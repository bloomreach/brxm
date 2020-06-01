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
package org.hippoecm.hst.core.sitemenu;

import java.util.List;

public interface EditableMenuItem extends CommonMenuItem {

    /**
     * @return The {@link List} of {@link EditableMenuItem}s and empty List if no child menu items available 
     */
    List<EditableMenuItem> getChildMenuItems();

    /**
     * 
     * @param childMenuItem - Add this {@link EditableMenuItem} to the list of childs
     */
    void addChildMenuItem(EditableMenuItem childMenuItem);

    /**
     * @return The {@link EditableMenu} for this {@link EditableMenuItem}
     */
    EditableMenu getEditableMenu();

    /**
     * @return The parent {@link EditableMenuItem} of this item and <code>null</code> when there
     * is not parent
     */
    EditableMenuItem getParentItem();

    /**
     * Set this EditableMenuItem to expanded (true) or not 
     * @param expanded - When <code>true</code> sets the {@link EditableMenuItem} to expanded
     */
    void setExpanded(boolean expanded);
    

    
}
