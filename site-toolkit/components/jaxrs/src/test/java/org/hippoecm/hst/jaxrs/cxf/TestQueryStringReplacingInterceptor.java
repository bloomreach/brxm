/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Test;

public class TestQueryStringReplacingInterceptor {
    
    @Test
    public void testQueryStringReplacements() throws Exception {
        QueryStringReplacingInterceptor interceptor = new QueryStringReplacingInterceptor();
        Map<String, String> paramNameReplaces = new HashMap<String, String>();
        paramNameReplaces.put("b", "newb");
        
        interceptor.setParamNameReplaces(paramNameReplaces);
        
        MessageImpl message = new MessageImpl();
        message.put(Message.QUERY_STRING, "a=1&b=2&c=3");
        interceptor.handleMessage(message);
        assertEquals("a=1&newb=2&c=3", message.get(Message.QUERY_STRING));
        
        message.put(Message.QUERY_STRING, "b=1&c=2");
        interceptor.handleMessage(message);
        assertEquals("newb=1&c=2", message.get(Message.QUERY_STRING));

        message.put(Message.QUERY_STRING, "a=1&c=2&b=3");
        interceptor.handleMessage(message);
        assertEquals("a=1&c=2&newb=3", message.get(Message.QUERY_STRING));

        message.put(Message.QUERY_STRING, "a=1&c=2&b");
        interceptor.handleMessage(message);
        assertEquals("a=1&c=2&newb", message.get(Message.QUERY_STRING));

        message.put(Message.QUERY_STRING, "a=1&b=2&c=3&b=22&c=33");
        interceptor.handleMessage(message);
        assertEquals("a=1&newb=2&c=3&newb=22&c=33", message.get(Message.QUERY_STRING));
    }
    
    @Test
    public void testAdditionalParameters() throws Exception {
        QueryStringReplacingInterceptor interceptor = new QueryStringReplacingInterceptor();
        interceptor.setAdditionalQueryString("_type=json");
        
        MessageImpl message = new MessageImpl();
        interceptor.handleMessage(message);
        assertEquals("_type=json", message.get(Message.QUERY_STRING));
        
        message.put(Message.QUERY_STRING, "a=1&b=2&c=3");
        interceptor.handleMessage(message);
        assertEquals("a=1&b=2&c=3&_type=json", message.get(Message.QUERY_STRING));
        
        message.put(Message.QUERY_STRING, "");
        interceptor.handleMessage(message);
        assertEquals("_type=json", message.get(Message.QUERY_STRING));
    }
}
