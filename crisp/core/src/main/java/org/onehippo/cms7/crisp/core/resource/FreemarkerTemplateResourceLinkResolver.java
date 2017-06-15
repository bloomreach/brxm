/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.core.resource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceLinkResolver;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.onehippo.cms7.crisp.api.resource.ResourceLinkResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * {@link ResourceLinkResolver} implementation enabling Freemarker templating in resource link generations.
 */
public class FreemarkerTemplateResourceLinkResolver extends AbstractResourceLinkResolver {

    private static Logger log = LoggerFactory.getLogger(FreemarkerTemplateResourceLinkResolver.class);

    /**
     * Freemarker configuration.
     */
    private Configuration configuration;

    /**
     * Link generating Freemarker template source in string.
     */
    private String templateSource;

    /**
     * Link generating Freemarker template instance.
     */
    private Template template;

    /**
     * Freemarker settings properties.
     */
    private Properties properties;

    /**
     * Default constructor.
     */
    public FreemarkerTemplateResourceLinkResolver() {
    }

    /**
     * Returns Freemarker settings properties.
     * @return
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets Freemarker settings properties.
     * @param properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Returns the link generating Freemarker template source in string.
     * @return the link generating Freemarker template source in string
     */
    public String getTemplateSource() {
        return templateSource;
    }

    /**
     * Sets the link generating Freemarker template source in string.
     * @param templateSource the link generating Freemarker template source in string
     */
    public void setTemplateSource(String templateSource) {
        this.templateSource = templateSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLink resolve(Resource resource, Map<String, Object> variables) throws ResourceException {
        Map<String, Object> context = new HashMap<String, Object>();

        if (variables != null) {
            context.putAll(variables);
        }

        context.put("resource", resource);
        StringWriter out = null;

        try {
            out = new StringWriter();
            getTemplate().process(context, out);
            out.flush();
            return new SimpleResourceLink(StringUtils.trim(out.toString()));
        } catch (Exception e) {
            throw new ResourceException("Resource link resolving failure.", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Returns the link generating Freemarker template.
     * @return the link generating Freemarker template
     */
    protected Template getTemplate() {
        if (template == null) {
            template = createTemplate();
        }

        return template;
    }

    /**
     * Creates the link generating Freemarker template.
     * @return the link generating Freemarker template
     */
    protected Template createTemplate() {
        try {
            return new Template(FreemarkerTemplateResourceLinkResolver.class.getSimpleName() + "-main",
                    new StringReader(templateSource), getConfiguration());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create template.");
        }
    }

    /**
     * Returns the Freemarker configuration used in template executions in this <code>ResourceLinkResolver</code>
     * implementation.
     * @return the Freemarker configuration used in template executions in this <code>ResourceLinkResolver</code>
     *         implementation
     */
    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = createConfiguration();
        }

        return configuration;
    }

    /**
     * Creates the Freemarker configuration to be used in template executions in this <code>ResourceLinkResolver</code>
     * implementation.
     * @return the Freemarker configuration to be used in template executions in this <code>ResourceLinkResolver</code>
     *         implementation
     * @return
     */
    protected Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));

        if (properties != null) {
            try {
                cfg.setSettings(properties);
            } catch (TemplateException e) {
                log.warn("Failed to load Freemarker configuration properties.", e);
            }
        }

        return cfg;
    }

}
