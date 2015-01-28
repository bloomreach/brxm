/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.vault;

import java.io.File;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.BASE_NAME_COMPARATOR;
import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.FILE_BASE_NAME_COMPARATOR;

public class FileNameComparatorUtilsTest {


    @Test
    public void string_base_name_comparator_test() {
        File file1 = EasyMock.createNiceMock(File.class);
        File file2 = EasyMock.createNiceMock(File.class);
        File file3 = EasyMock.createNiceMock(File.class);

        expect(file1.getName()).andReturn("style.css").anyTimes();
        expect(file2.getName()).andReturn("style.ftl").anyTimes();
        expect(file3.getName()).andReturn("style-extra.css").anyTimes();

        replay(file1, file2, file3);

        assertEquals(0, BASE_NAME_COMPARATOR.compare(file1.getName(), file1.getName()));
        // style.css has to come before style.ftl
        assertTrue(BASE_NAME_COMPARATOR.compare(file1.getName(), file2.getName()) < 0);
        assertTrue(BASE_NAME_COMPARATOR.compare(file2.getName(), file1.getName()) > 0);

        // style.css has to come before style-extra.css although lexically style-extra.css is earlier
        assertTrue(BASE_NAME_COMPARATOR.compare(file1.getName(), file3.getName()) < 0);
        assertTrue(BASE_NAME_COMPARATOR.compare(file3.getName(), file1.getName()) > 0);
    }

    @Test
    public void file_base_name_comparator_test() {
        File file1 = EasyMock.createNiceMock(File.class);
        File file2 = EasyMock.createNiceMock(File.class);
        File file3 = EasyMock.createNiceMock(File.class);

        expect(file1.getName()).andReturn("style.css").anyTimes();
        expect(file2.getName()).andReturn("style.ftl").anyTimes();
        expect(file3.getName()).andReturn("style-extra.css").anyTimes();

        replay(file1, file2, file3);

        assertEquals(0, FILE_BASE_NAME_COMPARATOR.compare(file1, file1));
        // style.css has to come before style.ftl
        assertTrue(FILE_BASE_NAME_COMPARATOR.compare(file1, file2) < 0);
        assertTrue(FILE_BASE_NAME_COMPARATOR.compare(file2, file1) > 0);

        // style.css has to come before style-extra.css although lexically style-extra.css is earlier
        assertTrue(FILE_BASE_NAME_COMPARATOR.compare(file1, file3) < 0);
        assertTrue(FILE_BASE_NAME_COMPARATOR.compare(file3, file1) > 0);
    }

}
