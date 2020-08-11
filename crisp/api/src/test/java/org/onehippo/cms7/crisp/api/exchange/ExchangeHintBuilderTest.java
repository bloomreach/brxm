/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.exchange;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ExchangeHintBuilderTest {

    /**
     * @deprecated Keep this for now to test the deprecated <code>request(...)</code> methods.
     */
    @Deprecated
    @Test
    public void testDefaultExchangeBuilderWithDeprecatedRequest() {
        ExchangeHint exchangeHint = ExchangeHintBuilder.create()
                .build();
        assertEquals("GET", exchangeHint.getMethodName());
        assertNull(exchangeHint.getRequest());

        Object requestHttpEntity = new Object();
        exchangeHint = ExchangeHintBuilder.create()
                .methodName("POST")
                .request(requestHttpEntity)
                .build();
        assertEquals("POST", exchangeHint.getMethodName());
        assertSame(requestHttpEntity, exchangeHint.getRequest());
    }

    @Test
    public void testDefaultExchangeBuilder() {
        ExchangeHint exchangeHint = ExchangeHintBuilder.create()
                .build();
        assertEquals("GET", exchangeHint.getMethodName());
        assertNull(exchangeHint.getRequestBody());

        Object requestBody = new Object();
        exchangeHint = ExchangeHintBuilder.create()
                .methodName("POST")
                .requestBody(requestBody)
                .build();
        assertEquals("POST", exchangeHint.getMethodName());
        assertSame(requestBody, exchangeHint.getRequestBody());
    }

    @Test
    public void testExchangeBuilderWithHeadersAndBody() {
        String requestBody = "{ 'action' : 'something' }";
        ExchangeHint exchangeHint = ExchangeHintBuilder.create()
                .methodName("POST")
                .requestHeader("Content-Type", "application/json")
                .requestHeader("Accept", "application/json", "application/xml")
                .requestBody(requestBody)
                .build();
        assertEquals("POST", exchangeHint.getMethodName());
        assertNull(exchangeHint.getRequest());
        Map<String, List<String>> headers = exchangeHint.getRequestHeaders();
        assertEquals(2, headers.size());
        assertEquals(Arrays.asList("application/json"), headers.get("Content-Type"));
        assertEquals(Arrays.asList("application/json", "application/xml"), headers.get("Accept"));
        assertEquals(requestBody, exchangeHint.getRequestBody());
    }
}
