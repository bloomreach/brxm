/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.permissions;

import java.util.SortedMap;

import javax.jcr.security.Privilege;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.onehippo.repository.security.DomainInfoPrivilege;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class PermissionsDialogTest {

    @Test
    public void test_privilegesToSortedMap() {

        final Privilege delegatee1 = createNiceMock(Privilege.class);
        expect(delegatee1.getName()).andStubReturn("name1");

        final Privilege delegatee2 = createNiceMock(Privilege.class);
        expect(delegatee2.getName()).andStubReturn("name2");

        final Privilege delegatee3 = createNiceMock(Privilege.class);
        expect(delegatee3.getName()).andStubReturn("name3");

        final Privilege delegateeSameName = createNiceMock(Privilege.class);
        expect(delegateeSameName.getName()).andStubReturn("name1");

        replay(delegatee1, delegatee2, delegatee3, delegateeSameName);

        final DomainInfoPrivilege priv1 = new DomainInfoPrivilege(delegatee1);
        final DomainInfoPrivilege priv2 = new DomainInfoPrivilege(delegatee2);
        final DomainInfoPrivilege priv3 = new DomainInfoPrivilege(delegatee3);
        final DomainInfoPrivilege privSameName = new DomainInfoPrivilege(delegateeSameName);

        final DomainInfoPrivilege[] privileges = new DomainInfoPrivilege[]{priv2, priv1, priv3};

        final SortedMap<String, DomainInfoPrivilege> sortedMap = PermissionsDialog.privilegesToSortedMap(privileges);

        assertThat(sortedMap.values())
                .as("Privileges should be sorted on name")
                .containsExactly(priv1, priv2, priv3);


        // not allowed to have two privs with same name
        final DomainInfoPrivilege[] invalidCombinationPrivs = new DomainInfoPrivilege[]{privSameName, priv1};

        final Throwable throwable = catchThrowable(() -> PermissionsDialog.privilegesToSortedMap(invalidCombinationPrivs));

        Assertions.assertThat(throwable).isInstanceOf(IllegalStateException.class)
                .hasNoCause().hasMessageStartingWith("Found two DomainInfoPrivilege objects with same " +
                "name 'name1' which should never be possible from HippoAccessManager#getPrivileges()");

    }

}
