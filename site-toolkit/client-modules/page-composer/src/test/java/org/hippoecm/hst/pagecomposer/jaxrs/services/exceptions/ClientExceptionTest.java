/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions;

import java.util.Date;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ClientExceptionTest {

    @Test
    public void test_get_error() {
        final ClientException e = new ClientException(null, ClientError.ITEM_NOT_FOUND);
        assertThat(e.getError(), is(ClientError.ITEM_NOT_FOUND));
        assertThat(e.getMessage(), nullValue());
    }

    @Test
    public void test_get_message() {
        final ClientException e = new ClientException("item not found", ClientError.ITEM_NOT_FOUND);
        assertThat(e.getError(), is(ClientError.ITEM_NOT_FOUND));
        assertThat(e.getMessage(), is("item not found"));
    }

    @Test
    public void test_get_message_parameters() {
        final ClientException e = new ClientException("item not found", null);
        assertThat(e.getParameterMap().isEmpty(), is(true));
    }

    @Test
    public void test_get_formatted_message() {
        final Date now = new Date();
        final ClientException e = new ClientException(String.format("%s not found at %s", "Something", now), null);
        assertThat(e.getMessage(), is("Something not found at " + now));
    }
}
