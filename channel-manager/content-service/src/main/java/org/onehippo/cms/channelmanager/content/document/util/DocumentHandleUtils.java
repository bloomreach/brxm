/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.DocumentUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;

public class DocumentHandleUtils {

    public static Node getHandle(final String uuid, final Session session) throws ErrorWithPayloadException {
        return DocumentUtils.getHandle(uuid, session)
                .filter(DocumentHandleUtils::isValidHandle)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST)));
    }

    private static boolean isValidHandle(final Node handle) {
        return DocumentUtils.getVariantNodeType(handle)
                .filter(type -> !type.equals(HippoNodeType.NT_DELETED))
                .isPresent();
    }

    private DocumentHandleUtils() {}
}
