/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.editor.type;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.MockPluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

public class JcrFieldDescriptorTest extends MockPluginTest {

    @Test
    public void getValidators() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required", "email" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);

        assertThat(jcrFieldDescriptor.getValidators(), contains("required", "email"));
    }

    @Test
    public void addFirstValidator() {
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.addValidator("required");

        assertThat(jcrFieldDescriptor.getValidators(), contains("required"));
    }

    @Test
    public void addSecondValidator() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.addValidator("email");

        assertThat(jcrFieldDescriptor.getValidators(), contains("required", "email"));
    }

    @Test
    public void removeOnlyValidator() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.removeValidator("required");

        assertThat(jcrFieldDescriptor.getValidators(), empty());
    }

    @Test
    public void removeFirstValidator() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required", "email" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.removeValidator("required");

        assertThat(jcrFieldDescriptor.getValidators(), contains("email"));
    }

    @Test
    public void removeLastValidator() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required", "email" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.removeValidator("email");

        assertThat(jcrFieldDescriptor.getValidators(), contains("required"));
    }

    @Test
    public void removeUnknownValidator() throws RepositoryException {
        root.setProperty("hipposysedit:validators", new String[] { "required", "email" });
        final JcrFieldDescriptor jcrFieldDescriptor = new JcrFieldDescriptor(new JcrNodeModel(root), null);
        jcrFieldDescriptor.removeValidator("unknown");

        assertThat(jcrFieldDescriptor.getValidators(), contains("required", "email"));
    }
}