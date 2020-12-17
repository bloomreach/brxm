/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.upload;

import java.io.File;

import javax.jcr.Node;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class FileUploadPreProcessorTest extends PluginTest {

    @Before
    /**
     * This method creates the following configuration.
     * <pre>
     *  /hippo:configuration/hippo:frontend/cms/cms-services/fileUploadPreProcessorService:
     *      jcr:primaryType: frontend:plugin
     *      plugin.class: org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin
     *      pre.processor.id: service.upload.pre.processor
     *      /preProcessors:
     *          jcr:primaryType: frontend:pluginconfig
     *          /author:
     *              jcr:primaryType: frontend:pluginconfig
     *              className: org.hippoecm.frontend.plugins.yui.upload.preprocessors.AuthorFileUploadPreProcessor
     *          /title:
     *              jcr:primaryType: frontend:pluginconfig
     *              className: org.hippoecm.frontend.plugins.yui.upload.preprocessors.TitleFileUploadPreProcessor
     * </pre>
     */
    public void setUp() throws Exception {
        super.setUp();
        Node configuration = session.getRootNode().getNode("hippo:configuration/hippo:frontend");
        configuration = configuration.addNode("cms");
        configuration = configuration.addNode("cms-services");
        Node service = configuration.addNode("fileUploadPreProcessorService", "frontend:plugin");
        service.setProperty("pre.processor.id", "service.upload.pre.processor");
        service.setProperty("plugin.class", "org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin");
        service.setProperty("pre.processor.id", "service.upload.pre.processor");
        Node preProcessors = service.addNode("preProcessors", "frontend:pluginconfig");
        preProcessors.addNode("author","frontend:pluginconfig").setProperty("className",
                "org.hippoecm.frontend.plugins.yui.upload.preprocessors.AuthorFileUploadPreProcessor");
        preProcessors.addNode("title","frontend:pluginconfig").setProperty("className",
                "org.hippoecm.frontend.plugins.yui.upload.preprocessors.TitleFileUploadPreProcessor");
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode(
                "hippo:configuration/hippo:frontend/cms")) {
            session.getRootNode().getNode(
                    "hippo:configuration/hippo:frontend/cms").remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void test_file_upload_pre_processors() throws Exception {

        // Instantiate the plugin, so the default instance of the service will be registered using the preprocessors
        // specified in the setup section
        DefaultFileUploadPreProcessorPlugin defaultFileUploadPreProcessorPlugin =
                new DefaultFileUploadPreProcessorPlugin(context, new JcrPluginConfig(new JcrNodeModel("/hippo" +
                        ":configuration/hippo:frontend/cms/cms-services/fileUploadPreProcessorService")));

        // Get the default instance from the plugin
        FileUploadPreProcessorService preProcessorService = DefaultFileUploadPreProcessorService.getPreProcessorService(
                context, new JcrPluginConfig(new JcrNodeModel("/hippo:configuration/hippo:frontend/cms/cms-services")));

        File file = null;
        PDDocument pdDocument = null;
        try {
            // Generate a fileItem
            FileItem fileItem = new DiskFileItem("fieldName", "application/pdf", false, "name", 0, null);

            pdDocument = new PDDocument();
            PDDocumentInformation info = pdDocument.getDocumentInformation();
            // save the blank PDF in our fileItem
            pdDocument.save(fileItem.getOutputStream());
            pdDocument.close();

            // Assert that there is no metadata in our blank PDF
            assertThat("There is no author yet", info.getAuthor(), IsNull.nullValue());
            assertThat("There is no title yet", info.getTitle(), IsNull.nullValue());

            // Execute our processor service
            FileUpload newFileUpload = preProcessorService.process(fileItem, new FileUpload(fileItem));

            // Assert that each processor has set a property in the metadata of the PDF
            pdDocument = PDDocument.load(newFileUpload.getInputStream());
            info = pdDocument.getDocumentInformation();
            assertThat("The pre processor has set an author", info.getAuthor(), IsEqual.equalTo("Processed by BRXM " +
                    "author"));
            assertThat("The pre processor has set a title", info.getTitle(), IsEqual.equalTo("Processed by BRXM " +
                    "title"));

        } finally {
            if (file != null) {
                file.delete();
            }
            if (pdDocument != null) {
                pdDocument.close();
            }
        }
    }
}
