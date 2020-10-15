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

import { injectable, inject } from 'inversify';
import { PaginationItemFactory, PaginationItemModel, PaginationItem } from './pagination-item';
import { Reference } from './reference';

export const PaginationModelToken = Symbol.for('PaginationModelToken');

export const TYPE_PAGINATION = 'pagination';

/**
 * Pagination model.
 */
export interface PaginationModel {
  current: PaginationItemModel;
  enabled: boolean;
  first: PaginationItemModel;
  items: Reference[];
  last: PaginationItemModel;
  next?: PaginationItemModel;
  offset: number;
  pages: PaginationItemModel[];
  previous?: PaginationItemModel;
  size: number;
  total: number;
  type: typeof TYPE_PAGINATION;
}

export interface Pagination {
  /**
   * @return The current page.
   */
  getCurrent(): PaginationItem;

  /**
   * @return The first page.
   */
  getFirst(): PaginationItem;

  /**
   * @return The current page items.
   */
  getItems(): Reference[];

  /**
   * @return The last page.
   */
  getLast(): PaginationItem;

  /**
   * @return The next page.
   */
  getNext(): PaginationItem | undefined;

  /**
   * @return The number of items before the current page.
   */
  getOffset(): number;

  /**
   * @return Currently listed pages.
   */
  getPages(): PaginationItem[];

  /**
   * @return The previous page.
   */
  getPrevious(): PaginationItem | undefined;

  /**
   * @return The number of items listed on the current page.
   */
  getSize(): number;

  /**
   * @return The total number of items.
   */
  getTotal(): number;

  /**
   * @return Whether the pagination is enabled.
   */
  isEnabled(): boolean;
}

@injectable()
export class PaginationImpl implements Pagination {
  private current: PaginationItem;

  private first: PaginationItem;

  private last: PaginationItem;

  private next?: PaginationItem;

  private pages: PaginationItem[];

  private previous?: PaginationItem;

  constructor(
    @inject(PaginationModelToken) protected model: PaginationModel,
    @inject(PaginationItemFactory) paginationItemFactory: PaginationItemFactory,
  ) {
    this.current = paginationItemFactory(model.current);
    this.first = paginationItemFactory(model.first);
    this.last = paginationItemFactory(model.last);
    this.next = model.next ? paginationItemFactory(model.next) : undefined;
    this.previous = model.previous ? paginationItemFactory(model.previous) : undefined;
    this.pages = model.pages.map(paginationItemFactory);
  }

  getCurrent(): PaginationItem {
    return this.current;
  }

  getFirst(): PaginationItem {
    return this.first;
  }

  getItems(): Reference[] {
    return this.model.items;
  }

  getLast(): PaginationItem {
    return this.last;
  }

  getNext(): PaginationItem | undefined {
    return this.next;
  }

  getOffset(): number {
    return this.model.offset;
  }

  getPages(): PaginationItem[] {
    return this.pages;
  }

  getPrevious(): PaginationItem | undefined {
    return this.previous;
  }

  getSize(): number {
    return this.model.size;
  }

  getTotal(): number {
    return this.model.total;
  }

  isEnabled(): boolean {
    return this.model.enabled;
  }
}

/**
 * Checks whether a value is a pagination.
 * @param value The value to check.
 */
export function isPagination(value: unknown): value is Pagination {
  return value instanceof PaginationImpl;
}
