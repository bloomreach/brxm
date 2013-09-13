package org.onehippo.cms7.essentials.dashboard.taxonomy.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @version "$Id$"
 */
public class TaxonomyQueryBuilder {

    private static Logger log = LoggerFactory.getLogger(TaxonomyQueryBuilder.class);

    public static class Builder {

        private static final String DOC_TEMPLATE = "jcr:primaryType='%s'";

        private List<String> documentTypes = new ArrayList<>();

        public Builder addDocumentType(String documentType) {
            documentTypes.add(String.format(DOC_TEMPLATE, documentType));
            return this;
        }

        List<String> getDocumentTypes() {
            return documentTypes;
        }

        public Builder setDocumentTypes(final List<String> documentTypes) {
            for (String type : documentTypes) {
                addDocumentType(type);
            }
            return this;
        }

        public TaxonomyQueryBuilder build() {
            return new TaxonomyQueryBuilder(this);
        }
    }

    private Builder builder;

    public TaxonomyQueryBuilder(final Builder builder) {
        this.builder = builder;
    }

    private static final String TEMPLATE = "//element(*,hippo:document)[%s and not(jcr:mixinTypes='hippotaxonomy:classifiable')]";


    public String getQuery() {
        final String query = String.format(TEMPLATE, StringUtils.join(builder.getDocumentTypes(), " or "));
        return query;
    }

    public void addToRegistry(PluginContext context) {
        Writer out = null;
        InputStream is = null;

        try {
            //Freemarker configuration object
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(getClass().getResource("/taxonomy-doc-updater.xml").getFile()).getParentFile());
            //Load template from source folder
            Template template = cfg.getTemplate("taxonomy-doc-updater.xml");

            // Build the data-model
            Map<String, String> data = new HashMap<>();
            data.put("query", getQuery());

            out = new StringWriter();
            template.process(data, out);
            is = new ByteArrayInputStream(out.toString().getBytes("UTF-8"));

            UpdateUtils.addToRegistry(context, is);
        } catch (IOException e) {
            log.error("IO exception while trying to add related doc updater to registry");
        } catch (TemplateException e) {
            log.error("Freemarker template exception while trying to add related doc updater to registry");
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(is);
        }
    }
}
