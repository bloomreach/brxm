/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { inject, injectable } from 'inversify';
import {
  ComponentImpl,
  ComponentMeta,
  ComponentModel,
  ComponentModelToken,
  Component,
  TYPE_COMPONENT_CONTAINER_ITEM,
} from './component';
import { EmitterMixin, Emitter } from '../emitter';
import { EventBusService, EventBus, PageUpdateEvent } from '../events';
import { LinkFactory } from './link-factory';
import { MetaCollectionFactory } from './meta-collection-factory';
import { PageModel } from './page';
import { resolve } from './reference';

const PARAMETER_HIDDEN = 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide';

interface ContainerItemParameters {
  [PARAMETER_HIDDEN]?: 'on' | 'off';
  [parameter: string]: string | undefined;
}

/**
 * Meta-data of a container item.
 */
export interface ContainerItemMeta extends ComponentMeta {
  params?: ContainerItemParameters;
  paramsInfo?: ComponentMeta['params'];
}

/**
 * Model of a container item.
 */
export interface ContainerItemModel extends ComponentModel {
  meta: ContainerItemMeta;
  label?: string;
  type: typeof TYPE_COMPONENT_CONTAINER_ITEM;
}

/**
 * Container item update event.
 */
export interface ContainerItemUpdateEvent {}

export interface ContainerItemEvents {
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

@injectable()
export class ContainerItemImpl
  extends EmitterMixin<typeof ComponentImpl, ContainerItemEvents>(ComponentImpl)
  implements ContainerItem
{
  constructor(
    @inject(ComponentModelToken) protected model: ContainerItemModel,
    @inject(EventBusService) eventBus: EventBus,
    @inject(LinkFactory) linkFactory: LinkFactory,
    @inject(MetaCollectionFactory) private metaFactory: MetaCollectionFactory,
  ) {
    super(model, [], linkFactory, metaFactory);

    eventBus.on('page.update', this.onPageUpdate.bind(this));
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    const page = event.page as PageModel;
    const model = resolve<ContainerItemModel>(page, page.root);
    if (model?.id !== this.getId()) {
      return;
    }

    this.model = model;
    this.meta = this.metaFactory(model.meta);
    this.emit('update', {});
  }

  getType() {
    return this.model.label;
  }

  isHidden() {
    return this.model.meta.params?.[PARAMETER_HIDDEN] === 'on';
  }

  getParameters(): ContainerItemParameters {
    return this.model.meta.paramsInfo || {};
  }
}

/**
 * Checks whether a value is a page container item.
 * @param value The value to check.
 */
export function isContainerItem(value: any): value is ContainerItem {
  return value instanceof ContainerItemImpl;
}
