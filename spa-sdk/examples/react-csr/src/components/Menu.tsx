/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { BrComponentContext, BrManageMenuButton, BrPageContext } from '@bloomreach/react-sdk';

export function Menu() {
  const component = React.useContext(BrComponentContext);
  const page = React.useContext(BrPageContext);
  if (!component || !page) {
    return null;
  }

  const { menu } = component.getModels<MenuModels>();

  return (
    <ul className={`navbar-nav col-12 ${page.isPreview() ? 'has-edit-button' : ''}`}>
      <BrManageMenuButton menu={menu} />
      { menu.siteMenuItems.map((item, index) => (
        <li key={index} className={`nav-item ${item.selected ? 'active' : ''}`}>
          <Link to={page.getUrl(item._links.site)} className="nav-link text-capitalize">{item.name}</Link>
        </li>
      )) }
    </ul>
  );
}
