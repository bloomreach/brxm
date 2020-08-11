/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This is an annotation to be used for org.hippoecm.hst.core.component.HstComponent's. This way, an org.hippoecm.hst.core.component.HstComponent's parameters can be <i>bridged</i> to
 * the actual parameters to be expected in the org.hippoecm.hst.configuration.components.HstComponentConfiguration. Also, this annotation can be used to inform YUI tools about
 * which parameters can be configured for the org.hippoecm.hst.core.component.HstComponent that has this annotation.
 * </p>
 * <p>
 * For example you can write your org.hippoecm.hst.core.component.HstComponent for a search as follows:
 * <blockquote>
 * <pre>
 * <code>
 * &#64;ParametersInfo(type = SearchInfo.class)
 * public class Search extends BaseHstComponent {
 *    public static final Logger log = LoggerFactory.getLogger(SearchNoParameterInfo.class);
 *    &#64;Override
 *    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
 *      SearchInfo info = getParametersInfo(request);
 *    }
 * }
 * </code>
 * </pre>
 * </blockquote>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)  
@Target({ElementType.TYPE})
public @interface ParametersInfo {
    
    Class<?> type();
    
}
