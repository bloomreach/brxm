/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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

import { ContainerItem, TYPE_CONTAINER_ITEM_UNDEFINED } from '@bloomreach/spa-sdk';
import { BrContainerItemUndefined } from '../cms';
import { BrNodeComponent } from './BrNodeComponent';
import { BrProps } from './BrProps';

export class BrNodeContainerItem extends BrNodeComponent<ContainerItem> {
  constructor(props: BrProps<ContainerItem>) {
    super(props);

    this.onUpdate = this.onUpdate.bind(this);
  }

  componentDidMount() {
    this.props.component.on('update', this.onUpdate);
  }

  componentDidUpdate(prevProps: BrProps<ContainerItem>) {
    if (this.props.component !== prevProps.component) {
      prevProps.component.off('update', this.onUpdate);
      this.props.component.on('update', this.onUpdate);
    }
  }

  componentWillUnmount() {
    this.props.component.off('update', this.onUpdate);
  }

  protected getMapping(): React.ComponentType<BrProps> {
    const type = this.props.component.getType();

    if (type && type in this.context) {
      return this.context[type] as React.ComponentType<BrProps>;
    }

    return this.context[TYPE_CONTAINER_ITEM_UNDEFINED as any] as React.ComponentType<BrProps>
      ?? BrContainerItemUndefined;
  }

  protected onUpdate() {
    this.forceUpdate(() => this.props.page.sync());
  }
}
