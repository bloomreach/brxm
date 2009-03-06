package org.hippoecm.addon.workflow;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;

public class Workflow extends Panel {
    private static final long serialVersionUID = 1L;
    MenuContainer container;

    public abstract class WorkflowFragment extends Fragment {
        protected WorkflowFragment(String id) {
            super(id, id, Workflow.this, Workflow.this.getModel());
        }
        abstract protected void initialize();
        void substantiate() {
            initialize();
        }
    }

    public Workflow(MenuContainer container, String id) {
        super(id);
        this.container = container;
    }

    public final MarkupContainer add(final Fragment component) {
        String id = component.getId();
        if(get(id) != null) {
            return addOrReplace(component);
        } else {
            return super.add(component);
        }
    }
}
