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

package org.onehippo.cms7.essentials.components.rest.common;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.cms7.essentials.components.rest.ctx.RestContext;


/**
 * @version "$Id: Restful.java 174709 2013-08-22 13:39:41Z mmilicevic $"
 */
public interface Restful<T extends HippoBean> {


    /**
     * Pumps data from given HippoBean to itself
     *
     * @param bean    hippo bean
     * @param context rest context instance
     * @return populated instance of "this" bean
     */

    Restful<T> fromHippoBean(T bean, final RestContext context);
}