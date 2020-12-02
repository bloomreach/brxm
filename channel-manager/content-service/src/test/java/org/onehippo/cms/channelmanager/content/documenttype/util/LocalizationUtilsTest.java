/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest(HippoServiceRegistry.class)
public class LocalizationUtilsTest {

    @Test
    public void getResourceBundle() {
        final Locale locale = new Locale("en");
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);
        PowerMock.mockStaticPartial(HippoServiceRegistry.class, "getService");

        expect(localizationService.getResourceBundle("hippo:types.ns:testdocument", locale)).andReturn(resourceBundle);
        replay(localizationService);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        PowerMock.replayAll();

        assertThat(LocalizationUtils.getResourceBundleForDocument("ns:testdocument", locale).get(), equalTo(resourceBundle));
    }

    @Test(expected = NoSuchElementException.class)
    public void getResourceBundleNull() {
        final Locale locale = new Locale("en");
        final LocalizationService localizationService = createMock(LocalizationService.class);
        PowerMock.mockStaticPartial(HippoServiceRegistry.class, "getService");

        expect(localizationService.getResourceBundle("hippo:types.ns:testdocument", locale)).andReturn(null);
        replay(localizationService);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        PowerMock.replayAll();

        LocalizationUtils.getResourceBundleForDocument("ns:testdocument", locale).get();
    }

    @Test
    public void getLocalizedDocumentDisplayName() {
        final String displayName = "displayName";
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("jcr:name")).andReturn(displayName);
        replay(resourceBundle);

        assertThat(LocalizationUtils.determineDocumentDisplayName("anything", Optional.of(resourceBundle)).get(), equalTo(displayName));
    }

    @Test
    public void getIdBasedDocumentDisplayName() {
        final String displayName = "displayName";
        final String id = "namespace:" + displayName;
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("jcr:name")).andReturn(null);
        replay(resourceBundle);

        assertThat(LocalizationUtils.determineDocumentDisplayName(id, Optional.of(resourceBundle)).get(), equalTo(displayName));
    }

    @Test
    public void getDocumentDisplayNameWithoutResourceBundle() {
        final String displayName = "displayName";
        final String id = "namespace:" + displayName;

        assertThat(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty()).get(), equalTo(displayName));
    }

    @Test(expected = NoSuchElementException.class)
    public void failToGetDocumentDisplayName() {
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("jcr:name")).andReturn(null);
        replay(resourceBundle);

        LocalizationUtils.determineDocumentDisplayName("anything", Optional.of(resourceBundle)).get();
    }

    @Test
    public void getLocalizedFieldDisplayName() {
        final String displayName = "displayName";
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("ns:title")).andReturn(displayName);
        replay(resourceBundle);

        assertThat(LocalizationUtils.determineFieldDisplayName("ns:title", Optional.of(resourceBundle), null).get(), equalTo(displayName));
    }

    @Test
    public void getConfigBasedFieldDisplayName() throws Exception {
        final String displayName = "displayName";
        final Node editorFieldNode = createMock(Node.class);
        final Property property = createMock(Property.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("ns:title")).andReturn(null);
        expect(editorFieldNode.hasProperty("caption")).andReturn(true);
        expect(editorFieldNode.getProperty("caption")).andReturn(property);
        expect(property.getString()).andReturn(displayName);
        replay(resourceBundle, editorFieldNode, property);

        assertThat(LocalizationUtils.determineFieldDisplayName("ns:title", Optional.of(resourceBundle),
                Optional.of(editorFieldNode)).get(), equalTo(displayName));
    }

    @Test
    public void getFieldNameAsDisplayNameForMissingTranslationAndCaption() throws Exception {
        final Node editorFieldNode = createMock(Node.class);
        final Property captionProperty = createMock(Property.class);
        final Property fieldProperty = createMock(Property.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("ns:title")).andReturn(null);
        expect(editorFieldNode.hasProperty("caption")).andReturn(false);
        expect(editorFieldNode.hasProperty("field")).andReturn(true);
        expect(editorFieldNode.getProperty("field")).andReturn(fieldProperty);
        expect(fieldProperty.getString()).andReturn("title");

        replay(resourceBundle, editorFieldNode, captionProperty, fieldProperty);

        assertThat(LocalizationUtils.determineFieldDisplayName("ns:title", Optional.of(resourceBundle),
                Optional.of(editorFieldNode)).get(), equalTo("Title"));
    }
    
    @Test(expected = NoSuchElementException.class)
    public void getConfigBasedFieldDisplayNameWithRepositoryException() throws Exception {
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("caption")).andThrow(new RepositoryException());
        expect(editorFieldNode.hasProperty("field")).andThrow(new RepositoryException());
        replay(editorFieldNode);

        LocalizationUtils.determineFieldDisplayName("ns:title", Optional.empty(), Optional.of(editorFieldNode)).get();
    }

    @Test
    public void getLocalizedHint() {
        final String hint = "hint";
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(resourceBundle.getString("ns:title#hint")).andReturn(hint);
        replay(resourceBundle);

        assertThat(LocalizationUtils.determineFieldHint("ns:title", Optional.of(resourceBundle), null).get(), equalTo(hint));
    }

    @Test
    public void getConfigBasedHint() throws Exception {
        final String hint = "hint";
        final Node editorFieldNode = createMock(Node.class);
        final Property property = createMock(Property.class);

        expect(editorFieldNode.hasProperty("hint")).andReturn(true);
        expect(editorFieldNode.getProperty("hint")).andReturn(property);
        expect(property.getString()).andReturn(hint);
        replay(editorFieldNode, property);

        assertThat(LocalizationUtils.determineFieldHint("ns:title", Optional.empty(),
                Optional.of(editorFieldNode)).get(), equalTo(hint));
    }
}
