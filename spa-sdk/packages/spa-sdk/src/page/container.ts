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

import { injectable } from 'inversify';
import { ComponentImpl, ComponentModel, Component, TYPE_COMPONENT_CONTAINER } from './component';
import { ContainerItem } from './container-item';

/**
 * A blocked container with blocked items.
 */
export const TYPE_CONTAINER_BOX = 'hst.vbox';

/**
 * An unordered list container.
 */
export const TYPE_CONTAINER_UNORDERED_LIST = 'hst.unorderedlist';

/**
 * An ordered list container.
 */
export const TYPE_CONTAINER_ORDERED_LIST = 'hst.orderedlist';

/**
 * A blocked container with inline items.
 */
export const TYPE_CONTAINER_INLINE = 'hst.span';

/**
 * A container without surrounding markup.
 */
export const TYPE_CONTAINER_NO_MARKUP = 'hst.nomarkup';

/**
 * Container Type.
 * @see https://documentation.bloomreach.com/library/concepts/template-composer/channel-editor-containers.html
 */
export type ContainerType = typeof TYPE_CONTAINER_BOX
  | typeof TYPE_CONTAINER_UNORDERED_LIST
  | typeof TYPE_CONTAINER_ORDERED_LIST
  | typeof TYPE_CONTAINER_INLINE
  | typeof TYPE_CONTAINER_NO_MARKUP;

/**
 * Model of a container item.
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

@injectable()
export class ContainerImpl extends ComponentImpl implements Container {
  protected model!: ContainerModel;

  protected children!: ContainerItem[];

  getChildren() {
    return this.children;
  }

  getType() {
    return this.model.xtype?.toLowerCase() as ContainerType;
  }
}

/**
 * Checks whether a value is a page container.
 * @param value The value to check.
 */
export function isContainer(value: any): value is Container {
  return value instanceof ContainerImpl;
}
