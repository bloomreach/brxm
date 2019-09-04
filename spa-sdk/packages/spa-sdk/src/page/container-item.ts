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

import { Component, ComponentMeta, ComponentModel, ComponentParameters } from './component';

/**
 * Container item type.
 */
export const TYPE_COMPONENT_CONTAINER_ITEM = 'CONTAINER_ITEM_COMPONENT';
const PARAMETER_HIDDEN = 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide';

/**
 * Parameters of a container item.
 */
export interface ContainerItemParameters extends ComponentParameters {
  [PARAMETER_HIDDEN]?: 'on' | 'off';
}

/**
 * Meta-data of a container item.
 */
export interface ContainerItemMeta extends ComponentMeta {
  paramsInfo?: ContainerItemParameters;
}

/**
 * Model of a container item.
 */
export interface ContainerItemModel extends ComponentModel {
  _meta?: ContainerItemMeta;
  label?: string;
  type: typeof TYPE_COMPONENT_CONTAINER_ITEM;
}

/**
 * A component that can be configured in the UI.
 */
export interface ContainerItem extends Component {
  /**
   * Returns the type of a container item. The available types depend on which
   * container items have been configured in the backend.
   *
   * @return the type of a container item (e.g. "Banner").
   */
  getType(): string | undefined;

  /**
   * Returns whether the component should not render anything.
   * Hiding components is only possible with the Relevance feature.
   *
   * @return whether the component is hidden or not.
   */
  isHidden(): boolean;
}

export class ContainerItem extends Component implements ContainerItem {
  constructor(protected model: ContainerItemModel) {
    super(model);
  }

  getType() {
    return this.model.label;
  }

  isHidden() {
    return this.getParameters()[PARAMETER_HIDDEN] === 'on';
  }

  getParameters(): ContainerItemParameters {
    return this.model._meta && this.model._meta.paramsInfo || {};
  }
}
