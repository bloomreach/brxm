/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.console;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.plugins.console.menu.help.HelpDescription;

import wicket.contrib.input.events.key.KeyType;

public final class Shortcuts {


    // save
    public static final KeyType[] CTRL_S = new KeyType[]{KeyType.Ctrl, KeyType.s};
    // open node (based on path input)
    public static final KeyType[] CTRL_O = new KeyType[]{KeyType.Ctrl, KeyType.o};
    // add property
    public static final KeyType[] CTRL_P = new KeyType[]{KeyType.Ctrl, KeyType.p};
    // delete multiple nodes
    public static final KeyType[] CTRL_M = new KeyType[]{KeyType.Ctrl, KeyType.m};
    // show help popup
    public static final KeyType[] CTRL_H = new KeyType[]{KeyType.Ctrl, KeyType.h};

    private Shortcuts() {
    }

    /**
     * Descriptions for help dialog
     */
    public static final List<HelpDescription> HELP_ITEMS = new ArrayList<HelpDescription>();

    static {
        HELP_ITEMS.add(new HelpDescription("SAVE", "Save pending changes", "Ctrl + s"));
        HELP_ITEMS.add(new HelpDescription("ADD PROPERTY", "Add property to selected node", "Ctrl + p"));
        HELP_ITEMS.add(new HelpDescription("DELETE MULTIPLE NODES", "Select multiple nodes and delete those", "Ctrl + m"));
        HELP_ITEMS.add(new HelpDescription("OPEN", "Open node by path (rel & abs) or UUID", "Ctrl + o"));
        HELP_ITEMS.add(new HelpDescription("HELP", "Opens help (this) dialog", "Ctrl + h"));
        HELP_ITEMS.add(new HelpDescription("Up, Down", "Navigate the tree", "Up, Down"));
        HELP_ITEMS.add(new HelpDescription("Left / Right", "Collapse / expand the tree", "Left / Right"));
    }

}
