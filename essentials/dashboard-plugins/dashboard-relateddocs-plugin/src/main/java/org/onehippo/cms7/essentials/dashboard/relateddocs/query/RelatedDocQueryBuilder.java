package org.onehippo.cms7.essentials.dashboard.relateddocs.query;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id$"
 */
public class RelatedDocQueryBuilder {
    private static final Logger log = LoggerFactory.getLogger(RelatedDocQueryBuilder.class);

    public static class Builder {

        private static final String DOC_TEMPLATE = "jcr:primaryType='%s'";

        private List<String> documentTypes = new ArrayList<>();

        public Builder addDocumentType(String documentType) {
            documentTypes.add(String.format(DOC_TEMPLATE, documentType));
            return this;
        }

        public Builder setDocumentTypes(final List<String> documentTypes) {
            for (String type : documentTypes) {
                addDocumentType(type);
            }
            return this;
        }

        List<String> getDocumentTypes() {
            return documentTypes;
        }

        public RelatedDocQueryBuilder build() {
            return new RelatedDocQueryBuilder(this);
        }
    }

    private Builder builder;

    public RelatedDocQueryBuilder(final Builder builder) {
        this.builder = builder;
    }

    private static final String TEMPLATE = "//element(*,hippo:document)[%s and not(jcr:mixinTypes='relateddocs:relatabledocs')]";


    public String getQuery() {
        return String.format(TEMPLATE, StringUtils.join(builder.getDocumentTypes(), " or "));
    }

    public void addToRegistry(PluginContext context) {
        InputStream is = null;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("query", getQuery());
            String template = TemplateUtils.injectTemplate("related-doc-updater.xml", data, RelatedDocQueryBuilder.class);
            is = new ByteArrayInputStream(template.getBytes("UTF-8"));
            UpdateUtils.addToRegistry(context, is);
        } catch (IOException e) {
            log.error("IO exception while trying to add related doc updater to registry");
        } finally {

            IOUtils.closeQuietly(is);
        }
    }
}
