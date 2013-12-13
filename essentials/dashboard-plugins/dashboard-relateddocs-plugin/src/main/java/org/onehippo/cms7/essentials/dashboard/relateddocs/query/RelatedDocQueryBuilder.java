package org.onehippo.cms7.essentials.dashboard.relateddocs.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id$"
 */
public class RelatedDocQueryBuilder {

    private static Logger log = LoggerFactory.getLogger(RelatedDocQueryBuilder.class);

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
        final String query = String.format(TEMPLATE, StringUtils.join(builder.getDocumentTypes(), " or "));
        return query;
    }

    public void addToRegistry(PluginContext context) {
     /*   Writer out = null;
        InputStream is = null;

        try {
            //Freemarker configuration object
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(getClass().getResource("/related-doc-updater.xml").getFile()).getParentFile());
            //Load template from source folder
            Template template = cfg.getTemplate("related-doc-updater.xml");

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
        }*/
    }
}
