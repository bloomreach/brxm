package org.onehippo.cms7.essentials.dashboard.relateddocs.template;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TemplateTest {

    private static Logger log = LoggerFactory.getLogger(TemplateTest.class);

    private final String TEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" sv:name=\"related-doc-updater\">\n" +
            "  <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n" +
            "    <sv:value>hipposys:updaterinfo</sv:value>\n" +
            "  </sv:property>\n" +
            "  <sv:property sv:name=\"hipposys:batchsize\" sv:type=\"Long\">\n" +
            "    <sv:value>10</sv:value>\n" +
            "  </sv:property>\n" +
            "  <sv:property sv:name=\"hipposys:dryrun\" sv:type=\"Boolean\">\n" +
            "    <sv:value>false</sv:value>\n" +
            "  </sv:property>\n" +
            "  <sv:property sv:name=\"hipposys:query\" sv:type=\"String\">\n" +
            "    <sv:value>Hello World!</sv:value>\n" +
            "  </sv:property>\n" +
            "  <sv:property sv:name=\"hipposys:script\" sv:type=\"String\">\n" +
            "    <sv:value>package org.hippoecm.frontend.plugins.cms.dev.updater\n" +
            "\n" +
            "import org.onehippo.repository.update.BaseNodeUpdateVisitor\n" +
            "import javax.jcr.Node\n" +
            "\n" +
            "class UpdaterTemplate extends BaseNodeUpdateVisitor {\n" +
            "\n" +
            "  boolean doUpdate(Node node) {\n" +
            "      if (node.canAddMixin(\"relateddocs:relatabledocs\")) {\n" +
            "      log.debug \"updating node ${node.path}\"\n" +
            "      node.addMixin(\"relateddocs:relatabledocs\");\n" +
            "      return true;\n" +
            "      } else {\n" +
            "      log.debug \"Cannot updating node ${node.path}\"\n" +
            "      }\n" +
            "      return false;\n" +
            "  }\n" +
            "\n" +
            "  boolean undoUpdate(Node node) {\n" +
            "    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')\n" +
            "  }\n" +
            "\n" +
            "}</sv:value>\n" +
            "  </sv:property>\n" +
            "  <sv:property sv:name=\"hipposys:throttle\" sv:type=\"Long\">\n" +
            "    <sv:value>1000</sv:value>\n" +
            "  </sv:property>\n" +
            "</sv:node>";


    @Test
    public void testTemplatingWithFreemarkerOnXML() throws Exception {
       /* try {
            //Freemarker configuration object
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(getClass().getResource("/").getFile()));
            //Load template from source folder
            Template template = cfg.getTemplate("/related-doc-updater-test.xml");

            // Build the data-model
            Map<String, String> data = new HashMap<>();
            data.put("query", "Hello World!");

            // Console output
            Writer out = new StringWriter();
            template.process(data, out);
            assertXMLEqual("comparing test xml to control xml", TEST, out.toString());
            out.flush();
        } catch (IOException e) {
            //
        } catch (TemplateException e) {
            log.error("Template exception {}", e);
        }
*/
    }
}
