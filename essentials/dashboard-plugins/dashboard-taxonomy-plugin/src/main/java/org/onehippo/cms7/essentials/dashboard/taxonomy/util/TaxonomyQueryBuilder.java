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

import javax.security.auth.login.Configuration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.text.templates.TemplateException;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        InputStream is = null;
        try {

            // Build the data-model
            Map<String, Object> data = new HashMap<>();
            data.put("query", getQuery());
            final String parsed  = TemplateUtils.injectTemplate("taxonomy-doc-updater.xml",data,getClass());
            is = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

            UpdateUtils.addToRegistry(context, is);
        } catch (IOException e) {
            log.error("IO exception while trying to add related doc updater to registry");
        } finally {

            IOUtils.closeQuietly(is);
        }
    }
}
