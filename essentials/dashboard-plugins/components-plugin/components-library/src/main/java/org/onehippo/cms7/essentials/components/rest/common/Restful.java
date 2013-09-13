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