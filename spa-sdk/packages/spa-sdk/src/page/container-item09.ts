/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { inject, injectable, optional } from 'inversify';
import {
  ComponentImpl,
  ComponentModel,
  TYPE_COMPONENT_CONTAINER_ITEM,
} from './component09';
import { ComponentModelToken } from './component';
import { ContainerItemEvents, ContainerItemMeta, ContainerItem } from './container-item';
import { EmitterMixin } from '../emitter';
import { EventBusService } from './events';
import { EventBus, PageUpdateEvent } from './events09';
import { Logger } from '../logger';
import { MetaCollectionFactory } from './meta-collection-factory';
import { PageModel } from './page09';
import { UrlBuilderService, UrlBuilder } from '../url';

const PARAMETER_HIDDEN = 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide';

/**
 * Model of a container item.
 */
export interface ContainerItemModel extends ComponentModel {
  _meta: ContainerItemMeta;
  ctype?: string;
  label?: string;
  type: typeof TYPE_COMPONENT_CONTAINER_ITEM;
}

@injectable()
export class ContainerItemImpl
  extends EmitterMixin<typeof ComponentImpl, ContainerItemEvents>(ComponentImpl)
  implements ContainerItem
{
  constructor(
    @inject(ComponentModelToken) protected model: ContainerItemModel,
    @inject(MetaCollectionFactory) private metaFactory: MetaCollectionFactory,
    @inject(UrlBuilderService) urlBuilder: UrlBuilder,
    @inject(EventBusService) @optional() eventBus?: EventBus,
    @inject(Logger) @optional() private logger?: Logger,
  ) {
    super(model, [], metaFactory, urlBuilder);

    eventBus?.on('page.update', this.onPageUpdate.bind(this));
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    const { page: model } = event.page as PageModel;
    if (model.id !== this.getId()) {
      return;
    }

    this.logger?.debug('Received container item update event.');
    this.logger?.debug('Event:', event);

    this.model = model as ContainerItemModel;
    this.meta = this.metaFactory(model._meta);
    this.emit('update', {});
  }

  getLabel() {
    return this.model.label;
  }

  getType() {
    return this.model.ctype ?? this.model.label;
  }

  isHidden() {
    return this.model._meta.params?.[PARAMETER_HIDDEN] === 'on';
  }

  getParameters<T>(): T {
    return (this.model._meta.paramsInfo ?? {}) as T;
  }
}

/**
 * Checks whether a value is a page container item.
 * @param value The value to check.
 */
export function isContainerItem(value: any): value is ContainerItem {
  return value instanceof ContainerItemImpl;
}
