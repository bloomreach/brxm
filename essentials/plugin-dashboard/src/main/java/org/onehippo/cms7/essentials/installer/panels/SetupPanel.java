package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.onehippo.cms7.essentials.dashboard.config.ConfigDocument;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigDocument;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigProperty;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeroen Reijn
 */
public class SetupPanel extends Panel {

    private static final long serialVersionUID = 1L;
    public static final String PROPERTY_NAMESPACE = "project-namespace";
    public static final String PROPERTY_COMPONENTS_PACKAGE = "components-package";
    public static final String PROPERTY_REST_PACKAGE = "rest-package";
    public static final String PROPERTY_BEANS_PACKAGE = "beans-package";
    public static final String CONFIG_NAME = "projectSettings";
    private static Logger log = LoggerFactory.getLogger(SetupPanel.class);

    private final PluginContext context;
    private final Plugin descriptor;

    final DropDownChoice<String> dropdownNamespace;
    final DropDownChoice<String> dropdownRest;
    final DropDownChoice<String> dropdownBeansPackage;
    final DropDownChoice<String> dropdownComponentsPackage;
    final TextField<String> inputBeansPackage;
    final TextField<String> inputRestPackage;
    final TextField<String> inputComponentsPackage;
    final TextField<String> inputNamespace;
    private String beansPackage;
    private String restPackage;
    private String componentsPackage;
    private String projectNamespace;
    private String selectedBeansPackage;
    private String selectedComponentsPackage;
    private String selectedRestPackage;
    private String selectedProjectNamespace;
    private List<String> projectNamespaces;
    private List<String> sitePackages;
    private List<String> restPackages;

    public SetupPanel(final String id, final Plugin descriptor, final PluginContext context) {
        super(id);

        this.context = context;
        this.descriptor = descriptor;

        loadModel();
        //############################################
        // DROPDOWNS
        //############################################
        dropdownNamespace = new DropDownChoice<>("projectNamespace", new PropertyModel<String>(this, "projectNamespace"), projectNamespaces);
        dropdownNamespace.add(new OnChangeBehavior(SelectBox.NAMESPACE, "onchange"));
        dropdownRest = new DropDownChoice<>("restPackage", new PropertyModel<String>(this, "restPackage"), restPackages);
        dropdownRest.add(new OnChangeBehavior(SelectBox.REST, "onchange"));
        dropdownComponentsPackage = new DropDownChoice<>("componentPackage", new PropertyModel<String>(this, "componentsPackage"), sitePackages);
        dropdownComponentsPackage.add(new OnChangeBehavior(SelectBox.COMPONENT, "onchange"));
        dropdownBeansPackage = new DropDownChoice<>("beanPackage", new PropertyModel<String>(this, "beansPackage"), sitePackages);
        dropdownBeansPackage.add(new OnChangeBehavior(SelectBox.BEAN, "onchange"));
        //############################################
        // INPUTS
        //############################################
        inputBeansPackage = new TextField<>("selectedBeansPackage", new PropertyModel<String>(this, "selectedBeansPackage"));
        inputRestPackage = new TextField<>("selectedRestPackage", new PropertyModel<String>(this, "selectedRestPackage"));
        inputComponentsPackage = new TextField<>("selectedComponentsPackage", new PropertyModel<String>(this, "selectedComponentsPackage"));
        inputNamespace = new TextField<>("selectedProjectNamespace", new PropertyModel<String>(this, "selectedProjectNamespace"));

        //############################################
        // ADD ITEMS
        //############################################
        final WebMarkupContainer webMarkupContainer = new WebMarkupContainer("container");
        // drop down
        webMarkupContainer.add(dropdownNamespace);
        webMarkupContainer.add(dropdownRest);
        webMarkupContainer.add(dropdownBeansPackage);
        webMarkupContainer.add(dropdownComponentsPackage);
        // input
        webMarkupContainer.add(inputRestPackage);
        webMarkupContainer.add(inputBeansPackage);
        webMarkupContainer.add(inputComponentsPackage);
        webMarkupContainer.add(inputNamespace);
        // save button
        final SaveButton saveButton = new SaveButton("save-button");
        webMarkupContainer.add(saveButton);
        // form
        final Form<?> form = new Form<Object>("form");
        form.add(webMarkupContainer);

        add(form);

        // output
        inputBeansPackage.setOutputMarkupId(true);
        inputComponentsPackage.setOutputMarkupId(true);
        inputNamespace.setOutputMarkupId(true);
        inputRestPackage.setOutputMarkupId(true);

    }

    private void onSaveButton(final AjaxRequestTarget target) {
        if (Strings.isEmpty(selectedBeansPackage)
                || Strings.isEmpty(selectedComponentsPackage)
                || Strings.isEmpty(selectedProjectNamespace)
                || Strings.isEmpty(selectedRestPackage)){
            //.TODO validation error
            log.error("TODO: throw validation error");


        }else{
            final PluginConfigService configService = getContext().getConfigService();
            final ConfigDocument projectSettings = new PluginConfigDocument(CONFIG_NAME);
            projectSettings.addProperty(new PluginConfigProperty(PROPERTY_NAMESPACE, selectedProjectNamespace));
            projectSettings.addProperty(new PluginConfigProperty(PROPERTY_REST_PACKAGE, selectedRestPackage));
            projectSettings.addProperty(new PluginConfigProperty(PROPERTY_COMPONENTS_PACKAGE, selectedComponentsPackage));
            projectSettings.addProperty(new PluginConfigProperty(PROPERTY_BEANS_PACKAGE, selectedBeansPackage));
            configService.write(projectSettings);
        }
    }

    private void loadModel() {
        final PluginContext context = getContext();
        projectNamespaces = HippoNodeUtils.getProjectNamespaces(context.getSession());
        sitePackages = ProjectUtils.getSitePackages(context);
        restPackages = ProjectUtils.getSitePackages(context);
        // load existing data:
        final Session session = context.getSession();
        // TODO: load existing
        final PluginConfigService configService = getContext().getConfigService();
        final ConfigDocument document = configService.read();
        if(document !=null){
            selectedComponentsPackage = document.getValue(PROPERTY_COMPONENTS_PACKAGE);
            selectedProjectNamespace = document.getValue(PROPERTY_NAMESPACE);
            selectedBeansPackage = document.getValue(PROPERTY_BEANS_PACKAGE);
            selectedRestPackage = document.getValue(PROPERTY_REST_PACKAGE);
        }



    }

    private static enum SelectBox {
        COMPONENT, BEAN, NAMESPACE, REST
    }

    private class OnChangeBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;
        final SelectBox type;

        public OnChangeBehavior(final SelectBox type, final String event) {
            super(event);
            this.type = type;
        }

        @Override
        protected void onUpdate(final AjaxRequestTarget target) {
            log.info("type {}", type);
            String modelObject;
            switch (type) {
                case BEAN:
                    modelObject = dropdownBeansPackage.getModelObject();
                    log.info("modelObject {}", modelObject);
                    selectedBeansPackage = modelObject;
                    target.add(inputBeansPackage);
                    break;
                case COMPONENT:
                    modelObject = dropdownComponentsPackage.getModelObject();
                    log.info("modelObject {}", modelObject);
                    selectedComponentsPackage = modelObject;
                    target.add(inputComponentsPackage);
                    break;
                case NAMESPACE:
                    modelObject = dropdownNamespace.getModelObject();
                    log.info("modelObject {}", modelObject);
                    selectedProjectNamespace = modelObject;
                    target.add(inputNamespace);
                    break;
                case REST:
                    modelObject = dropdownRest.getModelObject();
                    log.info("modelObject {}", modelObject);
                    selectedRestPackage = modelObject;
                    target.add(inputRestPackage);
                    break;
            }
        }
    }

    private class SaveButton extends AjaxButton {
        private static final long serialVersionUID = 1L;


        private SaveButton(String id) {
            super(id);

        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onSaveButton(target);
        }

    }

    public Plugin getDescriptor() {
        return descriptor;
    }

    public PluginContext getContext() {
        return context;
    }

}
