package org.hippocms.repository.frontend.update;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class UpdateManager {

    private List updatables;

    public UpdateManager() {
        updatables = new ArrayList();
    }

    public void addUpdatable(IUpdatable updatable) {
        updatables.add(updatable);
    }

    public void updateAll(AjaxRequestTarget target, JcrNodeModel model) {
        Iterator it = updatables.iterator();
        while (it.hasNext()) {
            ((IUpdatable) it.next()).update(target, model);
        }
    }

}
