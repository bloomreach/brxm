/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.editor.plugins.linkpicker;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.MockPluginTest;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockProperty;

import static org.junit.Assert.*;

public class LinkPickerDialogConfigTest extends MockPluginTest {

    private Node documents;
    private Node rootFolder;
    private JcrPropertyValueModel<String> docbaseValueModel;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node content = root.addNode("content", "hippostd:folder");
        documents = content.addNode("documents", "hippostd:folder");
    }

    // Fixtures

    private Node createDutchRootFolder() throws RepositoryException {
        rootFolder = addTranslatedNode(documents, "rootFolder", "hippostd:folder", "nl");
        Node englishFolder = addTranslatedNode(rootFolder, "englishFolder", "hippostd:folder", "en");
        Node dutchDocument = addTranslatedNode(englishFolder, "dutchDocument", "hippo:document", "nl");
        docbaseValueModel = createLinkField(dutchDocument);

        return rootFolder;
    }

    private Node createUntranslatedRootFolder() throws RepositoryException {
        rootFolder = documents.addNode("untranslatedFolder", "hippostd:folder");
        Node untranslatedDocument = rootFolder.addNode("untranslatedDocument", "hippo:document");
        docbaseValueModel = createLinkField(untranslatedDocument);

        return rootFolder;
    }

    private Node addTranslatedNode(Node parent, String relPath, String type, String language) throws RepositoryException {
        Node child = parent.addNode(relPath, type);
        child.addMixin("hippotranslation:translated");
        child.setProperty("hippotranslation:locale", language);
        return child;
    }

    private JcrPropertyValueModel<String> createLinkField(Node document) throws RepositoryException {
        Node linkCompound = document.addNode("linkCompound", "hippo:mirror");
        Property docbase = linkCompound.setProperty("hippo:docbase", "42");

        JcrPropertyModel<MockProperty> docbaseModel = new JcrPropertyModel<>(docbase);
        return new JcrPropertyValueModel<>(docbaseModel);
    }

    // Tests

    @Test
    public void defaultConfig_getsBaseUuidOfTranslatedRootFolder() throws RepositoryException {
        createDutchRootFolder();
        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(new JavaPluginConfig(), docbaseValueModel);
        assertEquals(rootFolder.getIdentifier(), config.getString("base.uuid"));
    }

    @Test
    public void languageContextAwareConfig_getsBaseUuidOfTranslatedRootFolder() throws RepositoryException {
        createDutchRootFolder();

        final JavaPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put("language.context.aware", "true");

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(pluginConfig, docbaseValueModel);
        assertEquals(rootFolder.getIdentifier(), config.getString("base.uuid"));
    }

    @Test
    public void languageContextUnawareConfig_doesNotSetBaseUuid() throws RepositoryException {
        createDutchRootFolder();

        final JavaPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put("language.context.aware", "false");

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(pluginConfig, docbaseValueModel);

        assertNull(config.getString("base.uuid"));
    }

    @Test
    public void configWithBaseUuid_usesConfiguredBaseUuid() throws RepositoryException {
        createDutchRootFolder();

        JavaPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put("base.uuid", "42");

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(pluginConfig, docbaseValueModel);

        assertEquals("42", config.getString("base.uuid"));
    }

    @Test
    public void englishDocument_usesEnglishRootFolder() throws RepositoryException {
        createDutchRootFolder();
        Node englishFolder = rootFolder.getNode("englishFolder");
        Node englishDocument = addTranslatedNode(englishFolder, "englishDocument", "hippo:document", "en");
        docbaseValueModel = createLinkField(englishDocument);

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(new JavaPluginConfig(), docbaseValueModel);

        String baseUuid = config.getString("base.uuid");
        assertEquals("an English document in an English folder under the Dutch root folder should use the UUID of" +
                        " the English folder, since that's the matching locale",
                englishFolder.getIdentifier(), config.getString("base.uuid"));
    }

    @Test
    public void documentInUntranslatedFolder_doesNotSetBaseUuid() throws RepositoryException {
        createUntranslatedRootFolder();

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(new JavaPluginConfig(), docbaseValueModel);

        assertNull(config.getString("base.uuid"));
    }

    @Test
    public void germanDocument_doesNotSetBaseUuid() throws RepositoryException {
        createDutchRootFolder();

        Node germanDocument = addTranslatedNode(rootFolder, "germanDocument", "hippo:document", "de");
        docbaseValueModel = createLinkField(germanDocument);

        IPluginConfig config = LinkPickerDialogConfig.fromPluginConfig(new JavaPluginConfig(), docbaseValueModel);

        assertNull("The fixture does not contain a German base folder, so no specific root folder UUID should be set for a German document",
                config.getString("base.uuid"));
    }

}