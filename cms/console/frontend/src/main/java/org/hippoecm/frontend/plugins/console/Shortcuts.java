/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    public static final KeyType[] CTRL_S = {KeyType.Ctrl, KeyType.s};
    // open node (based on path input)
    public static final KeyType[] CTRL_O = {KeyType.Ctrl, KeyType.o};
    // add node
    public static final KeyType[] CTRL_N = {KeyType.Ctrl, KeyType.n};
    // add property
    public static final KeyType[] CTRL_P = {KeyType.Ctrl, KeyType.p};
    // delete multiple nodes
    public static final KeyType[] CTRL_M = {KeyType.Ctrl, KeyType.m};
    // show help popup
    public static final KeyType[] CTRL_H = {KeyType.Ctrl, KeyType.h};

    private Shortcuts() {
    }

    /**
     * Descriptions for help dialog
     */
    public static final List<HelpDescription> HELP_ITEMS = new ArrayList<>();

    static {
        HELP_ITEMS.add(new HelpDescription("SAVE", "Save pending changes", "Ctrl + s"));
        HELP_ITEMS.add(new HelpDescription("ADD NODE", "Add node to selected node", "Ctrl + n"));
        HELP_ITEMS.add(new HelpDescription("ADD PROPERTY", "Add property to selected node", "Ctrl + p"));
        HELP_ITEMS.add(new HelpDescription("DELETE MULTIPLE NODES", "Select multiple nodes and delete those", "Ctrl + m"));
        HELP_ITEMS.add(new HelpDescription("OPEN", "Open node by path (rel & abs) or UUID", "Ctrl + o"));
        // The JUMP shortcut is registered on the client only as it does not require round-trips to the server
        HELP_ITEMS.add(new HelpDescription("JUMP", "Jump to the menu bar", "Alt + j"));
        HELP_ITEMS.add(new HelpDescription("HELP", "Opens help (this) dialog", "Ctrl + h"));
        HELP_ITEMS.add(new HelpDescription("Up, Down", "Navigate the tree", "Up, Down"));
        HELP_ITEMS.add(new HelpDescription("Left / Right", "Collapse / expand the tree", "Left / Right"));
    }

}
