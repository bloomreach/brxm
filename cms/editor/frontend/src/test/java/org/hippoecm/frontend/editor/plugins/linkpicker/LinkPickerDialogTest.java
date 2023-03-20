/*
 *  Copyright 2023 Bloomreach
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "com.sun.org.apache.xalan.*",
        "com.sun.org.apache.xerces.*",
        "javax.activation.*",
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.xml.*",
        "org.apache.logging.log4j.*",
        "org.w3c.dom.*",
        "org.xml.*",
})
@PrepareForTest({LinkPickerDialog.class, WorkflowUtils.class})
public class LinkPickerDialogTest {

    // WORKAROUND CMS-14946 :trigger log4j initialization to avoid power mock triggered deadlock in log4j.
    // After CMS-14948 has been done this workaround can be removed again
    private static final Logger ignore = LoggerFactory.getLogger(Object.class);
    private static final String DOCUMENT_TYPE_ID_CONTENT = "myproject:contentdocument";
    private static final String DOCUMENT_TYPE_ID_BANNER = "myproject:bannerdocument";
    private static final String QUERY_GENERIC_DOCUMENT = "new-document";
    private static final String QUERY_GENERIC_FOLDER = "new-folder";
    private static final String QUERY_CONTENT_DOCUMENT = "new-content-document";
    private static final String QUERY_CONTENT_FOLDER = "new-content-folder";
    private static final String QUERY_BANNER_DOCUMENT = "new-banner-document";
    private static final String QUERY_BANNER_FOLDER = "new-banner-folder";

    @Before
    public void setUp() {
        PowerMock.mockStatic(WorkflowUtils.class);
    }

    // The test table
    //  folder         | operationType | xPageSelection | documentTypeId            | is OK button enabled?
    // --------------------------------|------------------------------------------------------------------
    //        readonly |            "" |          false |                        "" | true
    //        readonly |            "" |          false | myproject:contentdocument | true
    //        readonly |            "" |          false |  myproject:bannerdocument | true
    //        readonly |            "" |           true |                        "" | false
    //        readonly |            "" |           true | myproject:contentdocument | false
    //        readonly |            "" |           true |  myproject:bannerdocument | false
    //
    //        readonly |        create |          false |                        "" | false
    //        readonly |        create |          false | myproject:contentdocument | false
    //        readonly |        create |          false |  myproject:bannerdocument | false
    //        readonly |        create |           true |                        "" | false
    //        readonly |        create |           true | myproject:contentdocument | false
    //        readonly |        create |           true |  myproject:bannerdocument | false
    //
    //         generic |            "" |          false |                        "" | true
    //         generic |            "" |          false | myproject:contentdocument | true
    //         generic |            "" |          false |  myproject:bannerdocument | true
    //         generic |            "" |           true |                        "" | false
    //         generic |            "" |           true | myproject:contentdocument | false
    //         generic |            "" |           true |  myproject:bannerdocument | false
    //         generic |        create |          false |                        "" | true
    //         generic |        create |          false | myproject:contentdocument | true
    //         generic |        create |          false |  myproject:bannerdocument | true
    //         generic |        create |           true |                        "" | false
    //         generic |        create |           true | myproject:contentdocument | false
    //         generic |        create |           true |  myproject:bannerdocument | false
    //
    //         content |            "" |          false |                        "" | true
    //         content |            "" |          false | myproject:contentdocument | true
    //         content |            "" |          false |  myproject:bannerdocument | false
    //         content |            "" |           true |                        "" | false
    //         content |            "" |           true | myproject:contentdocument | false
    //         content |            "" |           true |  myproject:bannerdocument | false
    //         content |        create |          false |                        "" | true
    //         content |        create |          false | myproject:contentdocument | true
    //         content |        create |          false |  myproject:bannerdocument | false
    //         content |        create |           true |                        "" | false
    //         content |        create |           true | myproject:contentdocument | false
    //         content |        create |           true |  myproject:bannerdocument | false
    //
    //         banners |            "" |          false |                        "" | true
    //         banners |            "" |          false | myproject:contentdocument | false
    //         banners |            "" |          false |  myproject:bannerdocument | true
    //         banners |            "" |           true |                        "" | false
    //         banners |            "" |           true | myproject:contentdocument | false
    //         banners |            "" |           true |  myproject:bannerdocument | false
    //         banners |        create |          false |                        "" | true
    //         banners |        create |          false | myproject:contentdocument | false
    //         banners |        create |          false |  myproject:bannerdocument | true
    //         banners |        create |           true |                        "" | false
    //         banners |        create |           true | myproject:contentdocument | false
    //         banners |        create |           true |  myproject:bannerdocument | false
    //
    //           xpage |            "" |          false |                        "" | false
    //           xpage |            "" |          false | myproject:contentdocument | false
    //           xpage |            "" |          false |  myproject:bannerdocument | false
    //           xpage |            "" |           true |                        "" | true
    //           xpage |            "" |           true | myproject:contentdocument | true
    //           xpage |            "" |           true |  myproject:bannerdocument | true
    //           xpage |        create |          false |                        "" | false
    //           xpage |        create |          false | myproject:contentdocument | false
    //           xpage |        create |          false |  myproject:bannerdocument | false
    //           xpage |        create |           true |                        "" | true
    //           xpage |        create |           true | myproject:contentdocument | true
    //           xpage |        create |           true |  myproject:bannerdocument | true
    //
    //  xpage-readonly |            "" |          false |                        "" | false
    //  xpage-readonly |            "" |          false | myproject:contentdocument | false
    //  xpage-readonly |            "" |          false |  myproject:bannerdocument | false
    //  xpage-readonly |            "" |           true |                        "" | true
    //  xpage-readonly |            "" |           true | myproject:contentdocument | true
    //  xpage-readonly |            "" |           true |  myproject:bannerdocument | true
    //  xpage-readonly |        create |          false |                        "" | false
    //  xpage-readonly |        create |          false | myproject:contentdocument | false
    //  xpage-readonly |        create |          false |  myproject:bannerdocument | false
    //  xpage-readonly |        create |           true |                        "" | false
    //  xpage-readonly |        create |           true | myproject:contentdocument | false
    //  xpage-readonly |        create |           true |  myproject:bannerdocument | false
    //
    //   xpage-banners |            "" |          false |                        "" | false
    //   xpage-banners |            "" |          false | myproject:contentdocument | false
    //   xpage-banners |            "" |          false |  myproject:bannerdocument | false
    //   xpage-banners |            "" |           true |                        "" | true
    //   xpage-banners |            "" |           true | myproject:contentdocument | false
    //   xpage-banners |            "" |           true |  myproject:bannerdocument | true
    //   xpage-banners |        create |          false |                        "" | false
    //   xpage-banners |        create |          false | myproject:contentdocument | false
    //   xpage-banners |        create |          false |  myproject:bannerdocument | false
    //   xpage-banners |        create |           true |                        "" | true
    //   xpage-banners |        create |           true | myproject:contentdocument | false
    //   xpage-banners |        create |           true |  myproject:bannerdocument | true

    // readonly folder

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentTypeAnd_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentTypeAnd_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // generic folder

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentTypeAndXPageSelection_notOk() throws Throwable {

        final MockNode folder = createGenericDocumentsFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // content folder

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectContentFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createContentFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // banners folder

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // xpage folder

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectGenericXPageFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createGenericXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // xpage-readonly folder

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectReadOnlyXPageFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_notOk() throws Throwable {

        final MockNode folder = createReadOnlyXPageFolder();
        final Map<String, Serializable> hints = Map.of(); // No Add permission

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints)))
                .andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER, DOCUMENT_TYPE_ID_CONTENT));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    // xpage-banners folder

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndNoXPageSelectionAndBannerDocumentType__notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndNoOperationTypeAndXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, ""));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndNoDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndNoXPageSelectionAndBannerDocumentType__notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, false, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndXPageSelectionAndNoDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, "", true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndXPageSelectionAndContentDocumentType_notOk() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertFalse(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_CONTENT, true, "create"));
    }

    @Test
    public void testOnFolderSelected_selectBannersXPageFolderAndCreateOperationTypeAndXPageSelectionAndBannerDocumentType_ok() throws Throwable {

        final MockNode folder = createBannersXPageFolder();
        final Map<String, Serializable> hints = Map.of(FolderWorkflow.HINT_ADD, "");

        final FolderWorkflow folderWorkflow = createMock(FolderWorkflow.class);
        expect(folderWorkflow.hints()).andReturn(hints);

        expect(WorkflowUtils.getFolderWorkflow(eq(folder), eq(LinkPickerDialog.DEFAULT_FOLDER_WORKFLOW_CATEGORY)))
                .andReturn(Optional.of(folderWorkflow));
        expect(WorkflowUtils.getFolderPrototypes(eq(hints))).andReturn(Set.of(DOCUMENT_TYPE_ID_BANNER));

        replay(folderWorkflow);
        PowerMock.replay(WorkflowUtils.class);

        assertTrue(LinkPickerDialog.isDocumentAllowedInFolder(folder, DOCUMENT_TYPE_ID_BANNER, true, "create"));
    }

    private @NotNull MockNode createReadOnlyFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("readonly", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, new String[]{QUERY_GENERIC_DOCUMENT,
                QUERY_GENERIC_FOLDER});

        return node;
    }

    private @NotNull MockNode createGenericDocumentsFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("generic", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, new String[]{QUERY_GENERIC_DOCUMENT,
                QUERY_GENERIC_FOLDER});

        return node;
    }

    private @NotNull MockNode createContentFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("content", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, new String[]{QUERY_CONTENT_DOCUMENT,
                QUERY_CONTENT_FOLDER});

        return node;
    }

    private @NotNull MockNode createBannersFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("banners", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, new String[]{"new-banner-document", "new-banner-folder"
        });

        return node;
    }

    private @NotNull MockNode createGenericXPageFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("xpage", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoNodeType.HIPPO_NAME, "xpage");
        node.addMixin(HippoNodeType.NT_NAMED);
        node.addMixin(HippoStdNodeType.NT_CM_XPAGE_FOLDER);
        node.addMixin(HippoStdNodeType.NT_XPAGE_FOLDER);

        return node;
    }

    private @NotNull MockNode createReadOnlyXPageFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("xpage-readonly", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoNodeType.HIPPO_NAME, "xpage-readonly");
        node.addMixin(HippoNodeType.NT_NAMED);
        node.addMixin(HippoStdNodeType.NT_CM_XPAGE_FOLDER);
        node.addMixin(HippoStdNodeType.NT_XPAGE_FOLDER);

        return node;
    }

    private @NotNull MockNode createBannersXPageFolder() throws Throwable {

        final MockNode root = MockNode.root();

        final MockNode node = root.addNode("xpage-banners", HippoStdNodeType.NT_FOLDER);
        node.setProperty(HippoNodeType.HIPPO_NAME, "xpage-banners");
        node.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, new String[]{QUERY_BANNER_DOCUMENT,
                QUERY_BANNER_FOLDER});
        node.addMixin(HippoNodeType.NT_NAMED);
        node.addMixin(HippoStdNodeType.NT_CM_XPAGE_FOLDER);
        node.addMixin(HippoStdNodeType.NT_XPAGE_FOLDER);

        return node;
    }
}