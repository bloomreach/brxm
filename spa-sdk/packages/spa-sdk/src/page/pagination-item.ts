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
import { Builder } from './factory';
import { LinkFactory } from './link-factory';
import { Link } from './link';

export const PaginationItemFactory = Symbol.for('PaginationItemFactory');
export const PaginationItemModelToken = Symbol.for('PaginationItemModelToken');

export type PaginationItemFactory = Builder<[PaginationItemModel], PaginationItem>;

type PaginationItemLinks = 'self' | 'site';

export interface PaginationItemModel {
  number: number;
  links: Record<PaginationItemLinks, Link>;
}

export interface PaginationItem {
  /**
   * @return The page number.
   */
  getNumber(): number;

  /**
   * @return The page URL.
   */
  getUrl(): string | undefined;
}

@injectable()
export class PaginationItemImpl implements PaginationItem {
  constructor(
    @inject(PaginationItemModelToken) protected model: PaginationItemModel,
    @inject(LinkFactory) private linkFactory: LinkFactory,
  ) {}

  getNumber(): number {
    return this.model.number;
  }

  getUrl(): string | undefined {
    return this.linkFactory.create(this.model.links.site);
  }
}
