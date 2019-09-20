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

import { Typed } from 'emittery';
import { ComponentImpl, ComponentMeta, ComponentModel, ComponentParameters, Component } from './component';
import { EmitterMixin, Emitter } from '../emitter';
import { Events, PageUpdateEvent } from '../events';
import { Meta } from './meta';

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
  params?: ContainerItemParameters;
  paramsInfo?: ComponentParameters;
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
 * Container item update event.
 */
export interface ContainerItemUpdateEvent {}

interface ContainerItemEvents {
  update: ContainerItemUpdateEvent;
}

/**
 * A component that can be configured in the UI.
 */
export interface ContainerItem extends Component, Emitter<ContainerItemEvents> {
  /**
   * Returns the type of a container item. The available types depend on which
   * container items have been configured in the backend.
   *
   * @return The type of a container item (e.g. "Banner").
   */
  getType(): string | undefined;

  /**
   * Returns whether the component should not render anything.
   * Hiding components is only possible with the Relevance feature.
   *
   * @return Whether the component is hidden or not.
   */
  isHidden(): boolean;
}

export class ContainerItemImpl
  extends EmitterMixin<typeof ComponentImpl, ContainerItemEvents>(ComponentImpl)
  implements ContainerItem
{
  constructor(protected model: ContainerItemModel, eventBus: Typed<Events>, meta: Meta[] = []) {
    super(model, [], meta);
    eventBus.on('page.update', this.onPageUpdate.bind(this));
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    const component = event.page.getComponent();
    if (!(component instanceof ContainerItemImpl) || component === this || component.getId() !== this.getId()) {
      return;
    }

    this.meta = component.meta;
    this.model = component.model;
    this.emit('update', {});
  }

  getType() {
    return this.model.label;
  }

  isHidden() {
    return !!(this.model._meta && this.model._meta.params && this.model._meta.params[PARAMETER_HIDDEN] === 'on');
  }

  getParameters(): ContainerItemParameters {
    return this.model._meta && this.model._meta.paramsInfo || {};
  }
}

/**
 * Checks whether a value is a container item.
 * @param value The value to check.
 */
export function isContainerItem(value: any): value is ContainerItem {
  return value instanceof ContainerItemImpl;
}
