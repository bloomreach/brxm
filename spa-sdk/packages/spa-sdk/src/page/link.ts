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

/**
 * Link to a page outside the current application.
 */
export const TYPE_LINK_EXTERNAL = 'external';

/**
 * Link to a page inside the current application.
 */
export const TYPE_LINK_INTERNAL = 'internal';

/**
 * Link to a CMS resource.
 */
export const TYPE_LINK_RESOURCE = 'resource';

/**
 * Unresolved link.
 */
export const TYPE_LINK_UNKNOWN = 'unknown';

export type LinkType = typeof TYPE_LINK_EXTERNAL
  | typeof TYPE_LINK_INTERNAL
  | typeof TYPE_LINK_RESOURCE
  | typeof TYPE_LINK_UNKNOWN;

/**
 * A link to a resource or a page.
 */
export interface Link {
  href?: string;
  type?: LinkType;
}

/**
 * Checks whether a value is a link.
 * @param value The value to check.
 */
export function isLink(value: any): value is Link {
  return !!value && (
    Object.prototype.hasOwnProperty.call(value, 'href')
      || Object.prototype.hasOwnProperty.call(value, 'type')
      && [TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE, TYPE_LINK_UNKNOWN].includes(value.type)
  );
}
