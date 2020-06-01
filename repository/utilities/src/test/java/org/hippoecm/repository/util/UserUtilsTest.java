/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import org.junit.Test;
import org.onehippo.repository.security.User;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class UserUtilsTest {

    @Test
    public void getUserNameNoFirstName() throws Exception {
        final User user = createMock(User.class);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(" Doe ");
        replay(user);

        assertEquals(UserUtils.getUserName(user).get(),"Doe");
        verify(user);
    }

    @Test
    public void getUserNameNoLastName() throws Exception {
        final User user = createMock(User.class);

        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(null);
        replay(user);

        assertEquals(UserUtils.getUserName(user).get(),"John");
        verify(user);
    }

    @Test
    public void getUserNameNoFirstAndLastName() throws Exception {
        final User user = createMock(User.class);
        expect(user.getFirstName()).andReturn(null);
        expect(user.getLastName()).andReturn(null);
        expect(user.getId()).andReturn("admin");
        replay(user);

        assertEquals(UserUtils.getUserName(user).get(),"admin");

        verify(user);
    }
}
