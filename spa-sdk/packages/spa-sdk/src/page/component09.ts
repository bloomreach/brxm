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

import { inject, injectable } from 'inversify';
import { ComponentChildrenToken, ComponentModelToken, ComponentMeta, Component } from './component';
import { Link } from './link';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection } from './meta-collection';
import { UrlBuilderService, UrlBuilder } from '../url';

/**
 * Generic component type.
 */
export const TYPE_COMPONENT = 'COMPONENT';

/**
 * Container item type.
 */
export const TYPE_COMPONENT_CONTAINER_ITEM = 'CONTAINER_ITEM_COMPONENT';

/**
 * Container type.
 */
export const TYPE_COMPONENT_CONTAINER = 'CONTAINER_COMPONENT';

export type ComponentType = typeof TYPE_COMPONENT
  | typeof TYPE_COMPONENT_CONTAINER_ITEM
  | typeof TYPE_COMPONENT_CONTAINER;

type ComponentLinks = 'componentRendering';

type ComponentModels = Record<string, any>;

/**
 * Model of a component.
 */
export interface ComponentModel {
  _links: Record<ComponentLinks, Link>;
  _meta: ComponentMeta;
  id: string;
  models?: ComponentModels;
  name?: string;
  type: ComponentType;
  components?: ComponentModel[];
}

@injectable()
export class ComponentImpl implements Component {
  protected meta: MetaCollection;

  constructor(
    @inject(ComponentModelToken) protected model: ComponentModel,
    @inject(ComponentChildrenToken) protected children: Component[],
    @inject(MetaCollectionFactory) metaFactory: MetaCollectionFactory,
    @inject(UrlBuilderService) private urlBuilder: UrlBuilder,
  ) {
    this.meta = metaFactory(this.model._meta);
  }

  getId() {
    return this.model.id;
  }

  getMeta() {
    return this.meta;
  }

  getModels<T extends ComponentModels>(): T;
  getModels() {
    return this.model.models || {};
  }

  getUrl() {
    return this.urlBuilder.getApiUrl(this.model._links.componentRendering.href!);
  }

  getName() {
    return this.model.name || '';
  }

  getParameters<T>(): T {
    return (this.model._meta.params ?? {}) as T;
  }

  getChildren() {
    return this.children;
  }

  getComponent(): this;
  getComponent<U extends Component>(...componentNames: string[]): U | undefined;
  getComponent(...componentNames: string[]) {
    // tslint:disable-next-line:no-this-assignment
    let component: Component | undefined = this;

    while (componentNames.length && component) {
      const name = componentNames.shift()!;
      component = component.getChildren().find(component => component.getName() === name);
    }

    return component;
  }

  getComponentById<U extends Component>(id: string): U | this | undefined;
  getComponentById(id: string) {
    const queue = [this as Component];

    while (queue.length) {
      const component = queue.shift()!;

      if (component.getId() === id) {
        return component;
      }

      queue.push(...component.getChildren());
    }
  }
}

/**
 * Checks whether a value is a page component.
 * @param value The value to check.
 */
export function isComponent(value: any): value is Component {
  return value instanceof ComponentImpl;
}
