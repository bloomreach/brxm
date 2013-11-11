/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.restservices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.Configuration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.eclipse.jface.text.templates.TemplateException;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ui.PluginFeedbackPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @version "$Id: RestServicesPlugin.java 169748 2013-07-05 12:03:01Z dvandiepen $"
 */
public class RestServicesPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(RestServicesPlugin.class);

    private static final String CONTAINER = "container";

    // The type of services to be able to select in the plugin
    private static final String SERVICE_TYPE_PLAIN = "plain"; // TODO check this
    private static final String SERVICE_TYPE_CONTENT = "content";
    private static final List<String> SERVICE_TYPES = Arrays.asList(SERVICE_TYPE_PLAIN, SERVICE_TYPE_CONTENT);

    private static final String HST_REST_TYPE_PLAIN = "JaxrsRestPlainPipeline"; // TODO check this
    private static final String HST_REST_TYPE_CONTENT = "JaxrsRestContentPipeline";
    //private static final List REST_SERVICE_TYPES = Arrays.asList(new String[]{REST_TYPE_PLAIN, REST_TYPE_CONTENT});


    private static final String DEFAULT_ROOT_MOUNT = "hst:hst/hst:hosts/dev-localhost/localhost/hst:root";

    private static final String HST_REST_PACKAGE_PLACEHOLDER = "hstRestPackage";
    // TODO check this
    private static final String DEFAULT_HST_REST_PACKAGE = "org.onehippo.cms7.essentials.site.rest";

    private final WebMarkupContainer container;
    private String mountName = StringUtils.EMPTY;
    private String serviceType = SERVICE_TYPE_PLAIN;

    // Used to set on the mount on property hst:types
    private final static String[] HST_REST_TYPES = new String[]{"rest"};

    private final FeedbackPanel feedback;

    public RestServicesPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        //############################################
        // FEEDBACK
        //############################################
        feedback = new PluginFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        //############################################
        // FORM
        //############################################


        final Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);

        container = new WebMarkupContainer(CONTAINER);
        container.setOutputMarkupId(true);
        form.add(container);

        final RadioGroup<String> myServiceType = new RadioGroup<>("serviceTypeField", new PropertyModel<String>(this, "serviceType"));
        myServiceType.add(new Radio<>("plain", new Model<>("plain")));
        myServiceType.add(new Radio<>("content", new Model<>("content")));

        container.add(myServiceType);

        container.add(new SubmitButton("submit"));

        add(form);
    }

    private void onFormSubmit(final AjaxRequestTarget target) {
        // TODO fix log level
        log.info("Submitting form.... (mountName={}, serviceType={})", mountName, serviceType);


        try {
            addSuccess("Create " + getSelectedRestType() + " rest service", target);
            createMount();
            getJCRSession().save();
            addSuccess("Created mount " + getRestMountName(), target);
            createAssemblyOverride();
            addSuccess("Created spring rest bean underneath " + getSiteAssemblyOverridesFolder(getContext()), target);
            createResourceProviderBean();
            addSuccess("Created java bean underneath " + getSiteRestPackageFolder(getContext()), target);
        } catch (RepositoryException e) {
            log.error("Unable to add REST mount: {}", e.getMessage());
            addError("Unable to add REST mount", target);
            resetSession();
        }
    }

    private void createMount() throws RepositoryException {

        final Node rootNode = getJCRSession().getRootNode();
        final Node rootMount = rootNode.getNode(DEFAULT_ROOT_MOUNT);

        final String restMountName = getRestMountName();

        if (rootMount.hasNode(restMountName)) {
            final Node oldNode = rootMount.getNode(restMountName);
            oldNode.remove();

        }
        final Node newNode = rootMount.addNode(restMountName, "hst:mount");
        newNode.setProperty("hst:alias", restMountName);
        newNode.setProperty("hst:isSite", false);
        if (SERVICE_TYPE_PLAIN.equals(getSelectedRestType())) {
            newNode.setProperty("hst:namedpipeline", HST_REST_TYPE_PLAIN);
        } else {
            newNode.setProperty("hst:namedpipeline", HST_REST_TYPE_CONTENT);
        }
        newNode.setProperty("hst:types", HST_REST_TYPES);
    }

    private String getSpringXmlFileName() {
        final StringBuilder sb = new StringBuilder();
        final String prefix = getContext().getProjectNamespacePrefix();
        if (StringUtils.isNotBlank(prefix)) {
            sb.append(prefix.trim());
            sb.append('-');
        }
        sb.append("rest-api.xml");
        return sb.toString();
    }

    private void createAssemblyOverride() {
        final File assemblyOverridesFolder = getSiteAssemblyOverridesFolder(getContext());
        if (!assemblyOverridesFolder.isDirectory()) {
            log.warn("Assembly overrides folder '{}' doesn't exist", assemblyOverridesFolder.getAbsolutePath());
            return;
        }

        final File springXmlFile = new File(assemblyOverridesFolder, getSpringXmlFileName());
        try {
            // TODO fix me
            //final Template template = loadResourceTemplate(getClass(), getSpringRestXmlTemplateName());
            final Map<String, String> parameterMap = new HashMap<>();
            parameterMap.put(HST_REST_PACKAGE_PLACEHOLDER, getRestPackage());
            saveTemplateToFile(parameterMap, springXmlFile);
        } catch (IOException e) {
            log.error("Unable to write file", e);
        } catch (TemplateException e) {
            log.error("Unable to process template", e);
        }
    }

    private String getSpringRestXmlTemplateName() {
        if (SERVICE_TYPE_PLAIN.equals(getSelectedRestType())) {
            return "spring-plain-rest-api.xml";
        }
        return "spring-content-rest-api.xml";
    }


    /**
     * Get the configured rest package path. When no path is specified the {@code #DEFAULT_HST_REST_PACKAGE} is used.
     *
     * @return the rest api package
     */
    private String getRestPackage() {
        final Path restPackagePath = getContext().getRestPackagePath();
        if (restPackagePath == null) {
            return DEFAULT_HST_REST_PACKAGE;
        }
        // TODO check this (regarding correct package name and dots)
        return restPackagePath.toString();
    }

    private static File getSiteRestPackageFolder(final PluginContext context) {
        // TODO check/fix this
        final Path restPackagePath = context.getRestPackagePath();
        if (restPackagePath != null) {
            return new File(String.valueOf(restPackagePath));
        }

        final File siteDirectory = context.getSiteDirectory();
        final String absSitePath = siteDirectory.getAbsolutePath();
        final StringBuilder sb = new StringBuilder();
        sb.append(absSitePath);
        sb.append(File.separatorChar);
        sb.append("src");
        sb.append(File.separatorChar);
        sb.append("main");
        sb.append(File.separatorChar);
        sb.append("java");
        sb.append(File.separatorChar);
        sb.append(StringUtils.replaceChars(DEFAULT_HST_REST_PACKAGE, '.', File.separatorChar));
        return new File(sb.toString());
    }

    public static File getSiteAssemblyOverridesFolder(final PluginContext context) {
        final File siteDirectory = context.getSiteDirectory();
        final String absSitePath = siteDirectory.getAbsolutePath();
        final StringBuilder sb = new StringBuilder();
        sb.append(absSitePath);
        sb.append(File.separatorChar);
        sb.append("src");
        sb.append(File.separatorChar);
        sb.append("main");
        sb.append(File.separatorChar);
        sb.append("resources");
        sb.append(File.separatorChar);
        sb.append("META-INF");
        sb.append(File.separatorChar);
        sb.append("hst-assembly");
        sb.append(File.separatorChar);
        sb.append("overrides");
        return new File(sb.toString());
    }

    private void createResourceProviderBean() {
        final File restPackageFolder = getSiteRestPackageFolder(getContext());
        if (!restPackageFolder.isDirectory()) {
            log.warn("REST package folder '{}' doesn't exist", restPackageFolder.getAbsolutePath());
            return;
        }

        File resourceProviderBeanFile = new File(restPackageFolder, getResourceProviderBeanName());
        try {
            // TODO fixme

            //final Template template = loadResourceTemplate(getClass(), getResourceProviderTemplateName());
            final Map<String, String> parameterMap = new HashMap<>();
            parameterMap.put(HST_REST_PACKAGE_PLACEHOLDER, getRestPackage());
            saveTemplateToFile(parameterMap, resourceProviderBeanFile);
        } catch (IOException e) {
            log.error("Unable to write file", e);
        } catch (TemplateException e) {
            log.error("Unable to process template", e);
        }

    }

    private String getResourceProviderTemplateName() {
        if (SERVICE_TYPE_PLAIN.equals(getSelectedRestType())) {
            return "HelloWorldPlainResource.txt";
        }
        return "HelloWorldContentResource.txt";
    }

    private String getResourceProviderBeanName() {
        if (SERVICE_TYPE_PLAIN.equals(getSelectedRestType())) {
            return "HelloWorldPlainResource.java";
        }
        return "HelloWorldContentResource.java";
    }

    private String getResourceProviderPackage() {
        // TODO: change this
        return "org.onehippo.cms7.essentials.site.restapi";
    }

    private String getSelectedRestType() {
        return serviceType;
    }

    private String getRestMountName() {
        return "restapi";
    }

    /**
     * Load a resource file as a Freemarker template.
     *
     * @param clazz        the clazz used to get to the resources folder
     * @param resourceFile the name of the resource file
     * @return a freemarker template
     * @throws IOException when the template can't be loaded
     */
  /*  private static Template loadResourceTemplate(final Class clazz, final String resourceFile) throws IOException {
        //Freemarker configuration object
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(clazz, "/");

        //Load template from source folder
        return cfg.getTemplate(resourceFile);
    }*/

    /**
     * Process a Freemarker template with the provided data and save the processed template to a file.
     *
     * @param template the Freemarker template to process
     * @param data     the data to parse
     * @param file     the file to save the processed template in
     * @throws IOException       when the template can not be saved
     * @throws TemplateException when te template can not be processed
     */
    private static void saveTemplateToFile( final Map<String, String> data, final File file) throws IOException, TemplateException {
        // TODO fix me
       /* Writer writer = new FileWriter(file);
        try {
            template.process(data, writer);
        } finally {
            IOUtils.closeQuietly(writer);
        }*/
    }

    /**
     * Retrieve the JCR session from the Wicket context.
     *
     * @return JCR session
     */
    private Session getJCRSession() {
        return getContext().getSession();
    }

    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    private void resetSession() {
        try {
            getJCRSession().refresh(false);
        } catch (RepositoryException e) {
            log.error("Error refreshing session", e);
        }
    }

    private void addError(final Serializable message, final AjaxRequestTarget target) {
        feedback.getFeedbackMessagesModel().setObject(new ArrayList<FeedbackMessage>());
        feedback.error(message);
        target.add(feedback);
    }

    private void addSuccess(final Serializable message, final AjaxRequestTarget target) {
        feedback.getFeedbackMessagesModel().setObject(new ArrayList<FeedbackMessage>());
        feedback.success(message);
        target.add(feedback);
    }


    private class SubmitButton extends AjaxButton {
        private static final long serialVersionUID = 1L;

        public SubmitButton(final String id) {
            super(id);
        }

        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            onFormSubmit(target);
        }
    }
}
