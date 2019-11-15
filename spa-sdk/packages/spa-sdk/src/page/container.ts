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

import { ComponentImpl, ComponentModel, Component, TYPE_COMPONENT_CONTAINER } from './component';
import { ContainerItem } from './container-item';
import { Factory } from './factory';
import { Link } from './link';
import { MetaCollectionModel, Meta } from './meta';

export const TYPE_CONTAINER_BOX = 'hst.vbox';
export const TYPE_CONTAINER_UNORDERED_LIST = 'hst.unorderedlist';
export const TYPE_CONTAINER_ORDERED_LIST = 'hst.orderedlist';
export const TYPE_CONTAINER_INLINE = 'hst.span';
export const TYPE_CONTAINER_NO_MARKUP = 'hst.nomarkup';

export type ContainerType = typeof TYPE_CONTAINER_BOX
  | typeof TYPE_CONTAINER_UNORDERED_LIST
  | typeof TYPE_CONTAINER_ORDERED_LIST
  | typeof TYPE_CONTAINER_INLINE
  | typeof TYPE_CONTAINER_NO_MARKUP;

/**
 * Model of a container item.
 * @hidden
 */
export interface ContainerModel extends ComponentModel {
  type: typeof TYPE_COMPONENT_CONTAINER;
  xtype?: ContainerType;
}

/**
 * A component that holds an ordered list of container item components.
 */
export interface Container extends Component {
  /**
   * Returns the type of a container.
   *
   * @see https://documentation.bloomreach.com/library/concepts/template-composer/channel-editor-containers.html
   * @return The type of a container (e.g. `TYPE_CONTAINER_BOX`).
   */
  getType(): ContainerType | undefined;

  /**
   * @return The children of a container.
   */
  getChildren(): ContainerItem[];
}

export class ContainerImpl extends ComponentImpl implements Container {
  constructor(
    protected model: ContainerModel,
    protected children: ContainerItem[],
    linkFactory: Factory<[Link], string>,
    metaFactory: Factory<[MetaCollectionModel], Meta[]>,
  ) {
    super(model, children, linkFactory, metaFactory);
  }

  getChildren() {
    return this.children;
  }

  getType() {
    return this.model.xtype && this.model.xtype.toLowerCase() as ContainerType;
  }
}

/**
 * Checks whether a value is a container.
 * @param value The value to check.
 */
export function isContainer(value: any): value is Container {
  return value instanceof ContainerImpl;
}
