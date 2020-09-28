/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean.dynamic;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * An HST component implementation that provides dynamic behavior on top of the {@link BaseHstComponent}.
 *
 * <p>
 * Any subclass of this class must include the annotation {@link ParametersInfo}, which must specify the interface
 * {@link DynamicComponentInfo} or an extension of it as its {@link ParametersInfo#type()}.
 * </p>
 * <p>
 * This component exposes all its params:
 * <pre>
 *      In Page Model API it's handled by the API itself.
 *      For FTL, the component sets the parameters in the request, using the attribute
 *          "org.hippoecm.hst.utils.ParameterUtils.parametersInfo".
 * </pre>
 * </p>
 * <p>
 * The component also finds all the residual (defined in JCR) parameters of type JcrPath, resolves the beans that are referenced and
 * sets those beans as separate models in the request, each by the name of its parameter.
 * </p>
 * <p>
 * In Page Model API, these will appear as top level {parametername - ref} entries under attribute "models", for example:
 * <pre>
 *     models: {
 *         document1: {ref: "xxx" },
 *         document2: {ref: "yyy" }
 *     }
 * </pre>
 * </p>
 * <p>
 * In FTL, they are accessible via FTL's expression language, for example:
 * <code>${document1.title?html}</code>
 *</p>
 * @version $Id$
 */
@ParametersInfo(type = DynamicComponentInfo.class)
public class BaseHstDynamicComponent extends BaseHstComponent {

    private static Logger log = LoggerFactory.getLogger(BaseHstDynamicComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        DynamicComponentInfo componentParametersInfo = getComponentParametersInfo(request);

        processParams(componentParametersInfo, request);
    }

    /**
     * Process the component parameters
     *
     * Resolves beans of all residual (defined in JCR) parameters of type JcrPath and sets them
     * as separate models into the request.
     *
     * @param componentParametersInfo The configuration of the current component
     * @param request      HstRequest
     */
    protected void processParams(final DynamicComponentInfo componentParametersInfo, final HstRequest request) {
        for (DynamicParameter param: componentParametersInfo.getDynamicComponentParameters()) {
            try {
                DynamicParameterConfig componentParameterConfig = param.getComponentParameterConfig();
                if (componentParameterConfig instanceof JcrPathParameterConfig) {
                    // do not use BaseHstComponent#getComponentParameter since this does not take 'targeting' neither
                    // POST query params into account, see HstParameterInfoProxyFactoryImpl#ParameterInfoInvocationHandler#getParameterValue
                    final Object o = componentParametersInfo.getResidualParameterValues().get(param.getName());
                    if (o == null) {
                        log.debug("No residual value for '{}' found. If it is non-residual, it means there is a subclass which " +
                                "should set the model for the explicit interface method itself.", param.getName());
                        continue;
                    }
                    if (o instanceof String) {
                        HippoBean bean = getContentBeanForPath((String)o, request,
                                ((JcrPathParameterConfig)componentParameterConfig).isRelative());
                        request.setModel(param.getName(), bean);
                    } else {
                        // never expected actually
                        log.warn("Unexpected value type for jcr path param '{}'. Type was '{}', but String is expected",
                                param.getName(), o.getClass());
                    }

                }
            } catch (ObjectBeanManagerException obme) {
                log.error("Problem fetching or converting bean", obme);
            }
        }
    }

    /**
     * Finds a HippoBean for a given path. If the path is null or empty, null will be returned
     *
     * @param documentPath relative document (content) path
     * @param request      HstRequest
     * @return bean for the specified path
     */
    protected HippoBean getContentBeanForPath(final String documentPath, HstRequest request, boolean relative)
        throws ObjectBeanManagerException {
        if (!Strings.isNullOrEmpty(documentPath)) {
            if (relative) {
                final HstRequestContext context = request.getRequestContext();
                final HippoBean root = context.getSiteContentBaseBean();
                return root.getBean(documentPath);
            } else {
                ObjectBeanManager objectBeanManager = request.getRequestContext().getObjectBeanManager();
                return (HippoBean) objectBeanManager.getObject(documentPath);
            }
        }
        return null;
    }
}
