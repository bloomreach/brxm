/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.demo.components;

import java.util.Collection;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.crisp.demo.model.Product;
import org.onehippo.cms7.crisp.demo.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Spring managed component to auto-wire {@link ProductService} bean in Spring ApplicationContext.
 * <P>
 * Note: Since Hippo CMS v12.1, you don't need to set the FQCN of this class to the {@link Service} annotation any more.
 * If you skip the value of the annotation, then Spring will register this component bean as camel-cased simple class name.
 * i.e, "productListComponent".
 * </P>
 * <P>
 * Therefore, as long as you set <code>@hst:componentclassname</code> property to the logical bean name (e.g, "productListComponent"),
 * it will work fine.
 * Also, in this case, note that if this spring-managed component needs to provide a {@link ParametersInfo} type,
 * please set <code>@hst:parametersinfoclassname</code> additionally in the HST Component configuration node.
 * </P>
 *
 * @see <a href="https://www.onehippo.org/library/concepts/web-application/spring-managed-hst-components.html">https://www.onehippo.org/library/concepts/web-application/spring-managed-hst-components.html</a>
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProductListComponent extends BaseHstComponent {

    private static Logger log = LoggerFactory.getLogger(ProductListComponent.class);

    @Autowired
    private ProductService productService;

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        Collection<Product> products = productService.getProductCollection();
        request.setAttribute("products", products);
    }

}
