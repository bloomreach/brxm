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

import { Factory } from './factory';
import { Link } from './link';
import { MetaCollectionModel, MetaCollection } from './meta-collection';

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

/**
 * Parameters of a component.
 * @hidden
 */
export type ComponentParameters = Partial<Record<string, string>>;

/**
 * Meta-data of a component.
 * @hidden
 */
export interface ComponentMeta extends MetaCollectionModel {
  params?: ComponentParameters;
}

/**
 * @hidden
 */
type ComponentLinks = 'componentRendering';

/**
 * @hidden
 */
type ComponentModels = Record<string, any>;

/**
 * Model of a component.
 * @hidden
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

/**
 * A component in the current page.
 */
export interface Component {
  /**
   * @return The component id.
   */
  getId(): string;

  /**
   * @return The component meta-data collection.
   */
  getMeta(): MetaCollection;

  /**
   * @return The map of the component models.
   */
  getModels<T extends ComponentModels>(): T;

  /**
   * @return The link to the partial component model.
   */
  getUrl(): string | undefined;

  /**
   * @return The name of the component.
   */
  getName(): string;

  /**
   * @return The parameters of the component.
   */
  getParameters(): ComponentParameters;

  /**
   * @return The direct children of the component.
   */
  getChildren(): Component[];

  /**
   * Looks up for a nested component.
   * @param componentNames A lookup path.
   */
  getComponent<U extends Component>(...componentNames: string[]): U | undefined;
  getComponent(): this;

  /**
   * Looks up for a nested component by its id.
   * @param id A component id.
   */
  getComponentById<U extends Component>(id: string): U | this | undefined;
}

export class ComponentImpl implements Component {
  protected meta: MetaCollection;

  constructor(
    protected model: ComponentModel,
    protected children: Component[],
    private linkFactory: Factory<[Link], string>,
    metaFactory: Factory<[MetaCollectionModel], MetaCollection>,
  ) {
    this.meta = metaFactory.create(model._meta);
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
    return this.linkFactory.create(this.model._links.componentRendering);
  }

  getName() {
    return this.model.name || '';
  }

  getParameters() {
    return this.model._meta.params || {};
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
