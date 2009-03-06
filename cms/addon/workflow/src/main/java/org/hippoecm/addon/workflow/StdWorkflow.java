package org.hippoecm.addon.workflow;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.StringResourceModel;

public class StdWorkflow extends Workflow {
    private static final long serialVersionUID = 1L;
    
    private String name;

    public StdWorkflow(MenuContainer container, String id, String name) {
        super(container, id);
        this.name = name;

        add(new WorkflowFragment("text") {
            @Override
            protected void initialize() {
                MenuLink link;
                add(link = new MenuLink("text") {
                    @Override
                    public void onClick() {
                        execute();
                    }
                });
                link.add(new Label("label", getTitle()));
            }
        });

        add(new WorkflowFragment("icon") {
            @Override
            protected void initialize() {
                ResourceReference model = getIcon();
                add(new Image("icon", model));
            }
        });

        add(new WorkflowFragment("panel") {
            @Override
            protected void initialize() {
            }
        });
    }

    protected final String getName() {
        return name;
    }

    protected StringResourceModel getTitle() {
        return new StringResourceModel(getName(), this, null, getName());
    }

    protected ResourceReference getIcon() {
        return new ResourceReference(container.getClass(), "workflow-16.png");
    }

    protected void execute() {
    }
}
