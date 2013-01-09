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

import java.util.LinkedList;
import java.util.List;

public class ListItemPad extends JavaLayoutPad {

    private static final long serialVersionUID = 1L;

    ILayoutPad parent;
    Orientation orientation;

    List<ListItemLayoutContext> controls;
    ILayoutContext control;

    public ListItemPad(List<ListItemLayoutContext> controls, ILayoutPad parent) {
        super("item");
        this.controls = controls;
        this.parent = parent;
    }

    public ListItemPad(List<ListItemLayoutContext> controls, Orientation orientation) {
        super("item");
        this.controls = controls;
        this.orientation = orientation;
    }

    /**
     * Set the layout control for the item builder plugin.
     * This control is used to determine the index of this item in the list.
     * @param control
     */
    public void setLayoutControl(ILayoutContext control) {
        this.control = control;
    }
    
    public ILayoutContext getLayoutControl() {
        return this.control;
    }

    public String getUpName() {
        switch (getListOrientation()) {
        case HORIZONTAL:
            return "left";
        case VERTICAL:
        default:
            return "up";
        }
    }

    public String getDownName() {
        switch (getListOrientation()) {
        case HORIZONTAL:
            return "right";
        case VERTICAL:
        default:
            return "down";
        }
    }

    public ILayoutPad getParent() {
        return parent;
    }

    @Override
    public List<String> getTransitions() {
        List<String> transitions = new LinkedList<String>();
        if (controls.indexOf(control) > 0) {
            transitions.add(getUpName());
        }
        if (controls.indexOf(control) < controls.size() - 1) {
            transitions.add(getDownName());
        }

        if (parent != null) {
            for (String transition : parent.getTransitions()) {
                if (!transitions.contains(transition)) {
                    transitions.add(transition);
                }
            }
        }
        return transitions;
    }

    @Override
    public ILayoutTransition getTransition(String name) {
        if (getUpName().equals(name)) {
            return new ILayoutTransition() {
                private static final long serialVersionUID = 1L;

                public String getName() {
                    return getUpName();
                }

                public ILayoutPad getTarget() {
                    ListItemLayoutContext previous = controls.get(controls.indexOf(control) - 1);
                    return previous.getLayoutPad();
                }

            };
        } else if (getDownName().equals(name)) {
            return new ILayoutTransition() {
                private static final long serialVersionUID = 1L;

                public String getName() {
                    return getDownName();
                }

                public ILayoutPad getTarget() {
                    ListItemLayoutContext next = controls.get(controls.indexOf(control) + 1);
                    return next.getLayoutPad();
                }

            };
        } else if (parent != null) {
            return parent.getTransition(name);
        } else {
            throw new RuntimeException("Unknown transition " + name);
        }
    }

    protected Orientation getListOrientation() {
        if (this.orientation != null) {
            return this.orientation;
        }
        if (parent.getOrientation() != null) {
            return parent.getOrientation();
        }
        return Orientation.VERTICAL;
    }

}
