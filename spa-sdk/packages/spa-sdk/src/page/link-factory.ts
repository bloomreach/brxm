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

import { MultipleTypeFactory } from './factory';
import { LinkType, Link, TYPE_LINK_INTERNAL, isLink } from './link';

type LinkBuilder = (link: string) => string;

export class LinkFactory extends MultipleTypeFactory<LinkType, LinkBuilder> {
  create(link: Link): string;
  create(path: string): string;
  create(link: Link | string) {
    if (isLink(link)) {
      return this.createLink(link);
    }

    return this.createPath(link);
  }

  private createLink(link: Link) {
    if (!link.type || !this.mapping.has(link.type)) {
      return link.href;
    }

    const builder = this.mapping.get(link.type)!;

    return builder(link.href);
  }

  private createPath(path: string) {
    return this.createLink({ href: path, type: TYPE_LINK_INTERNAL });
  }
}
