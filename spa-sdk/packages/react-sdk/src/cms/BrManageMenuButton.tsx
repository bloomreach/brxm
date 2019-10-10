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
import { Menu } from '@bloomreach/spa-sdk';
import { BrPageContext } from '../page';
import { BrMetaWrapper } from '../meta';

interface BrManageMenuButtonProps {
  menu: Menu;
}

export class BrManageMenuButton extends React.Component<BrManageMenuButtonProps, {}> {
  static contextType = BrPageContext;
  context: React.ContextType<typeof BrPageContext>;

  constructor(props: BrManageMenuButtonProps) {
    super(props);
  }

  render() {
    if (!this.context!.isPreview()) {
      return null;
    }

    if (!this.props.menu._meta) {
      return null;
    }

    const meta = this.context!.getMeta(this.props.menu._meta);
    return <BrMetaWrapper meta={meta} />;
  }
}
