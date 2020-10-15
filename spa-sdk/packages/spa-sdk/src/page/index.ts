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

import { Typed } from 'emittery';
import { isComponent as isComponent10, Component } from './component';
import { isComponent as isComponent09 } from './component09';
import { isContainer as isContainer10, Container } from './container';
import { isContainer as isContainer09 } from './container09';
import { isContainerItem as isContainerItem10, ContainerItem } from './container-item';
import { isContainerItem as isContainerItem09 } from './container-item09';
import { Menu as Menu10 } from './menu';
import { Menu as Menu09 } from './menu09';
import { PageModel as PageModel10, Page, isPage as isPage10 } from './page';
import { PageModel as PageModel09, isPage as isPage09 } from './page09';

/**
 * Checks whether a value is a page component.
 * @param value The value to check.
 */
export function isComponent(value: any): value is Component {
  return isComponent10(value) || isComponent09(value);
}

/**
 * Checks whether a value is a page container.
 * @param value The value to check.
 */
export function isContainer(value: any): value is Container {
  return isContainer10(value) || isContainer09(value);
}

/**
 * Checks whether a value is a page container item.
 * @param value The value to check.
 */
export function isContainerItem(value: any): value is ContainerItem {
  return isContainerItem10(value) || isContainerItem09(value);
}

/**
 * Checks whether a value is a page.
 * @param value The value to check.
 */
export function isPage(value: any): value is Page {
  return isPage10(value) || isPage09(value);
}

/**
 * Model of a page.
 */
export type PageModel = PageModel10 | PageModel09;

export type EventBus = Typed<{ 'page.update': { page: PageModel } }>;

/**
 * Menu content model.
 */
export type Menu = Menu09 | Menu09 & Menu10;

export {
  Component,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER,
  TYPE_COMPONENT_CONTAINER_ITEM,
} from './component';
export {
  TYPE_COMPONENT as TYPE_COMPONENT_09,
  TYPE_COMPONENT_CONTAINER as TYPE_COMPONENT_CONTAINER_09,
  TYPE_COMPONENT_CONTAINER_ITEM as TYPE_COMPONENT_CONTAINER_ITEM_09,
} from './component09';
export { ContainerItem } from './container-item';
export {
  Container,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from './container';
export { Content, isContent } from './content09';
export { Document, TYPE_DOCUMENT, isDocument } from './document';
export { EventBusService } from './events';
export { ImageSet, TYPE_IMAGE_SET, isImageSet } from './image-set';
export { Image } from './image';
export { Link, TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE, isLink } from './link';
export { MenuItem } from './menu-item';
export { TYPE_MENU, isMenu } from './menu';
export { MetaCollection } from './meta-collection';
export { MetaComment, isMetaComment } from './meta-comment';
export { Meta, META_POSITION_BEGIN, META_POSITION_END, isMeta } from './meta';
export { PageFactory } from './page-factory';
export { PageModule } from './module';
export { PageModule as PageModule09 } from './module09';
export { Page } from './page';
export { PaginationItem } from './pagination-item';
export { Pagination, isPagination } from './pagination';
export { Reference, isReference } from './reference';
export { Visitor, Visit } from './relevance';
