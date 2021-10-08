/*
 *  Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.errors;

import org.apache.wicket.Application;
import org.apache.wicket.DefaultExceptionMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.handler.EmptyRequestHandler;
import org.apache.wicket.settings.ExceptionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwallowExceptionMapper extends DefaultExceptionMapper {
    private static final Logger log = LoggerFactory.getLogger(SwallowExceptionMapper.class);

    @Override
    protected IRequestHandler mapUnexpectedExceptions(final Exception e, final Application application) {
        final ExceptionSettings.UnexpectedExceptionDisplay unexpectedExceptionDisplay = application.getExceptionSettings()
                .getUnexpectedExceptionDisplay();

        if (ExceptionSettings.SHOW_EXCEPTION_PAGE.equals(unexpectedExceptionDisplay)) {
            log.error("Unexpected error occurred", e);
            return new EmptyRequestHandler();
        }

        return super.mapUnexpectedExceptions(e, application);
    }
}
