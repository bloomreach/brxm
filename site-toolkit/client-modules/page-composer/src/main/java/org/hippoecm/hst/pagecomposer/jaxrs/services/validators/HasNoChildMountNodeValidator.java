/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

/**
 * Validate that the given hst:mount node does not have any children hst:mount nodes
 */
public class HasNoChildMountNodeValidator implements Validator {

    private final List<Mount> mounts;

    public HasNoChildMountNodeValidator(final List<Mount> mounts) {
        this.mounts = mounts;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {

        final boolean hasMountWithExplicitChildren = mounts.stream()
                .anyMatch(mount -> mount.getChildMounts().stream().anyMatch(Mount::isExplicit));

        if (hasMountWithExplicitChildren) {
            final Map<String, String> parameterMap = new HashMap<>();
            final Set<String> mountChildren = new LinkedHashSet<>();

            mounts.stream().forEach(mount -> {
                for (Mount child : mount.getChildMounts()) {
                    mountChildren.add(child.getMountPath());
                }
            });
            parameterMap.put("childMountList", StringUtils.join(mountChildren, ", "));
            parameterMap.put("channel", mounts.get(0).getChannel().getName());

            throw new ClientException("Child mount exists", ClientError.CHILD_MOUNT_EXISTS, parameterMap);
        }
    }
}
