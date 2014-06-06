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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class LineEndingsModelTest {

    @Test
    public void getModelReturnsCRLF() {
        assertGetModel("a\r\nb", "a\nb");
        assertGetModel("a\r\nb\r\n", "a\nb\n");
        assertGetModel("a\r\n\r\nb", "a\n\nb");
        assertGetModel(null, null);
    }

    private void assertGetModel(final String expectedGetValue, final String delegateValue) {
        final LineEndingsModel model = new LineEndingsModel(Model.of(delegateValue));
        assertEquals(expectedGetValue, model.getObject());
    }

    @Test
    public void setModelDelegatesLF() {
        assertSetModel("a\nb", "a\r\nb");
        assertSetModel("a\nb\n", "a\r\nb\r\n");
        assertSetModel("a\n\nb", "a\r\n\r\nb");
        assertSetModel(null, null);
    }

    private void assertSetModel(final String expectedDelegateValue, final String setValue) {
        final IModel<String> delegate = Model.of("");
        final LineEndingsModel model = new LineEndingsModel(delegate);
        model.setObject(setValue);
        assertEquals(expectedDelegateValue, delegate.getObject());
    }

    @Test
    public void delegateIsDetached() {
        IModel<String> delegate = EasyMock.createMock(IModel.class);
        delegate.detach();
        EasyMock.expectLastCall();
        replay(delegate);

        final LineEndingsModel model = new LineEndingsModel(delegate);
        model.detach();

        verify(delegate);
    }

}