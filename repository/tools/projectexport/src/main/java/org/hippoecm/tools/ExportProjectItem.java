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
package org.hippoecm.tools;

import javax.swing.tree.TreeNode;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

public class ExportProjectItem extends Panel
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ExportItem.java 18965 2009-07-23 07:16:15Z bvanhalderen $";

    ExportProjectItem(MarkupContainer parent, String id, final ExportTreeModel tree, final TreeNode node, Element.ProjectElement element) {
        super(id);
    }
}
