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

import { injectable } from 'inversify';
import { ComponentImpl, ComponentModel, TYPE_COMPONENT_CONTAINER } from './component09';
import { ContainerItem } from './container-item';
import { ContainerType, Container } from './container';

/**
 * Model of a container item.
 */
export interface ContainerModel extends ComponentModel {
  type: typeof TYPE_COMPONENT_CONTAINER;
  xtype?: ContainerType;
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
