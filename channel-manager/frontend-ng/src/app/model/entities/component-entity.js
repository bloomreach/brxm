/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as HstConstants from '../constants';
import { Entity } from './entity';

export class ComponentEntity extends Entity {
  constructor(meta) {
    super(meta);

    this._contributions = [];
    this._links = [];
  }

  getId() {
    return this._meta.uuid;
  }

  getLabel() {
    const label = this._meta[HstConstants.LABEL];
    if (label === 'null') {
      return this.getType(); // no label available, fallback to type.
    }

    return label;
  }

  getLastModified() {
    const lastModified = this._meta[HstConstants.LAST_MODIFIED];

    return lastModified ? parseInt(lastModified, 10) : 0;
  }

  getLockedBy() {
    return this._meta[HstConstants.LOCKED_BY];
  }

  getRenderUrl() {
    return this._meta[HstConstants.RENDER_URL];
  }

  hasLabel() {
    return true;
  }

  hasNoIFrameDomElement() {
    return this._meta[HstConstants.HAS_NO_DOM];
  }

  isLocked() {
    return angular.isDefined(this.getLockedBy());
  }

  isLockedByCurrentUser() {
    return this._meta[HstConstants.LOCKED_BY_CURRENT_USER] === 'true';
  }

  isShared() {
    return this._meta[HstConstants.SHARED] === 'true';
  }

  isXPageComponent() {
    return this._meta[HstConstants.EXPERIENCE_PAGE_COMPONENT] === 'true';
  }

  addHeadContributions(contributions) {
    this._contributions.push(contributions);
  }

  getHeadContributions() {
    return this._contributions;
  }

  addLink(link) {
    this._links.push(link);
  }

  getLinks() {
    return this._links;
  }
}
