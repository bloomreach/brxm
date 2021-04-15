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

import { inject, injectable, optional } from 'inversify';
import {
  ComponentImpl,
  ComponentMeta,
  ComponentModel,
  ComponentModelToken,
  Component,
  TYPE_COMPONENT_CONTAINER_ITEM,
} from './component';
import { EmitterMixin, Emitter } from '../emitter';
import { EventBusService, EventBus, PageUpdateEvent } from './events';
import { LinkFactory } from './link-factory';
import { Logger } from '../logger';
import { MetaCollectionFactory } from './meta-collection-factory';
import { PageModel } from './page';
import { resolve } from './reference';

/**
 * A container item without mapping.
 */
export const TYPE_CONTAINER_ITEM_UNDEFINED: symbol = Symbol.for('ContainerItemUndefined');

interface ContainerItemParameters {
  [parameter: string]: string | undefined;
}

/**
 * Meta-data of a container item.
 */
export interface ContainerItemMeta extends ComponentMeta {
  hidden?: boolean;
  params?: ContainerItemParameters;
  paramsInfo?: ComponentMeta['params'];
}

/**
 * Model of a container item.
 */
export interface ContainerItemModel extends ComponentModel {
  ctype?: string;
  label?: string;
  meta: ContainerItemMeta;
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
   * Returns the label of a container item catalogue component.
   *
   * @return The label of a catalogue component (e.g. "News List").
   */
  getLabel(): string | undefined;

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
    @inject(LinkFactory) linkFactory: LinkFactory,
    @inject(MetaCollectionFactory) private metaFactory: MetaCollectionFactory,
    @inject(EventBusService) @optional() eventBus?: EventBus,
    @inject(Logger) @optional() private logger?: Logger,
  ) {
    super(model, [], linkFactory, metaFactory);

    eventBus?.on('page.update', this.onPageUpdate.bind(this));
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    const page = event.page as PageModel;
    const model = resolve<ContainerItemModel>(page, page.root);
    if (model?.id !== this.getId()) {
      return;
    }

    this.logger?.debug('Received container item update event.');
    this.logger?.debug('Event:', event);

    this.model = model;
    this.meta = this.metaFactory(model.meta);
    this.emit('update', {});
  }

  getLabel() {
    return this.model.label;
  }

  getType() {
    return this.model.ctype ?? this.model.label;
  }

  isHidden() {
    return !!this.model.meta.hidden;
  }

  getParameters<T>(): T {
    return (this.model.meta.paramsInfo ?? {}) as T;
  }
}

/**
 * Checks whether a value is a page container item.
 * @param value The value to check.
 */
export function isContainerItem(value: any): value is ContainerItem {
  return value instanceof ContainerItemImpl;
}
