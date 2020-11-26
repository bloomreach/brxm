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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;
import org.hippoecm.frontend.plugins.yui.upload.preprocessors.AuthorFileUploadPreProcessor;
import org.hippoecm.frontend.plugins.yui.upload.preprocessors.TitleFileUploadPreProcessor;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin.CLASS_NAME_KEY;
import static org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorPlugin.PRE_PROCESSORS_NODE_KEY;

public class FileUploadPreProcessorPluginAndServiceTest {

    private IPluginContext context;
    private IPluginConfig pluginConfig;

    /**
     * This method set up te context and the config that we will use to initialize our plugin.
     * We are using mock data to simulate our configuration.
     */
    @Before
    public void setUp() {
        context = createNiceMock(IPluginContext.class);
        context.registerService(anyObject(), anyString());
        expectLastCall();
        createMockPluginConfig();
    }

    @Test
    public void test_file_upload_pre_processors() throws IOException {
        // Initialize the plugin
        DefaultFileUploadPreProcessorPlugin defaultFileUploadPreProcessorPlugin =
                new DefaultFileUploadPreProcessorPlugin(context, pluginConfig);

        // Get the service from the plugin
        FileUploadPreProcessorService preProcessorService =
                defaultFileUploadPreProcessorPlugin.getPreProcessorService();

        File file = null;
        PDDocument pdDocument = null;
        try {
            // Create a blank PDF as the uploaded file
            file = File.createTempFile("temp", null);

            pdDocument = new PDDocument();
            PDDocumentInformation info = pdDocument.getDocumentInformation();
            pdDocument.save(file);
            pdDocument.close();

            // Assert that there is no metadata in our blank PDF
            assertThat("There is no author yet", info.getAuthor(), IsNull.nullValue());
            assertThat("There is no title yet", info.getTitle(), IsNull.nullValue());

            // Generate our internal models which wrap the blank PDF
            FileItem fileItem = new DiskFileItem("fieldName", "application/pdf", false, "name", 0, null);
            UploadedFile uploadedFile = new UploadedFile(file, fileItem);

            // Execute our processor service
            preProcessorService.process(uploadedFile);

            // Assert that each processor has set a property in the metadata of the PDF
            pdDocument = PDDocument.load(uploadedFile.getFile());
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

    /**
     * This method mocks the following configuration.
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
     *          /creator:
     *              jcr:primaryType: frontend:pluginconfig
     *              className: org.hippoecm.frontend.plugins.yui.upload.preprocessors.CreatorFileUploadPreProcessor
     *          /title:
     *              jcr:primaryType: frontend:pluginconfig
     *              className: org.hippoecm.frontend.plugins.yui.upload.preprocessors.TitleFileUploadPreProcessor
     *          /subject:
     *              jcr:primaryType: frontend:pluginconfig
     *              className: org.hippoecm.frontend.plugins.yui.upload.preprocessors.SubjectFileUploadPreProcessor
     * </pre>
     */
    private void createMockPluginConfig() {
        pluginConfig = createNiceMock(IPluginConfig.class);
        final IPluginConfig preProcessorsConfig = createNiceMock(IPluginConfig.class);
        final IPluginConfig authorImplementationConfig = createNiceMock(IPluginConfig.class);
        final IPluginConfig titleImplementationConfig = createNiceMock(IPluginConfig.class);

        expect(pluginConfig.getPluginConfig(PRE_PROCESSORS_NODE_KEY)).andReturn(preProcessorsConfig).anyTimes();
        expect(preProcessorsConfig.getPluginConfigSet()).andReturn(
                new HashSet<>(Arrays.asList(authorImplementationConfig, titleImplementationConfig))).anyTimes();
        expect(authorImplementationConfig.getString(CLASS_NAME_KEY, null)).andReturn(
                AuthorFileUploadPreProcessor.class.getName()).anyTimes();
        expect(titleImplementationConfig.getString(CLASS_NAME_KEY, null)).andReturn(
                TitleFileUploadPreProcessor.class.getName()).anyTimes();

        replay(pluginConfig, preProcessorsConfig, authorImplementationConfig, titleImplementationConfig);
    }

}
