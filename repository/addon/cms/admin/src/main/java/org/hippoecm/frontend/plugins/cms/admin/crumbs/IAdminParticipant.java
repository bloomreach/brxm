/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.crumbs;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.model.IModel;

public interface IAdminParticipant extends IBreadCrumbParticipant {
    final static String SVN_ID = "$Id$";

    IModel getTitle(Component component);
}
