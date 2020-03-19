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

import {
  Component,
  Container,
  EndMarker,
  HeadContributions,
  LinkEntity,
  PageMeta,
  Page,
} from './entities';
import * as HstConstants from './constants';
import { ComponentEntity } from './entities/component-entity';

export default class ModelFactoryService {
  constructor($log) {
    'ngInject';

    this.$log = $log;
    this._builders = new Map();
  }

  createPage(metaCollection) {
    if (!metaCollection || !metaCollection.length) {
      return;
    }

    const { children, links, meta: pageMeta } = this._parseMeta(metaCollection);
    const meta = pageMeta.length
      ? pageMeta.reduce((result, item) => result.addMeta(item))
      : new PageMeta({});

    // eslint-disable-next-line consistent-return
    return new Page(meta, children, links);
  }

  createComponent(meta) {
    const { children } = this._parseMeta(meta);
    const [component] = children.slice(-1);

    if (!(component instanceof Component)) {
      throw new Error('Failed to create a new component.');
    }

    return component;
  }

  createContainer(meta) {
    const { children } = this._parseMeta(meta);
    const [container] = children.slice(-1);

    if (!(container instanceof Container)) {
      throw new Error('Failed to create a new container.');
    }

    return container;
  }

  register(type, callback) {
    this._builders.set(type, callback);

    return this;
  }

  transform(callback) {
    this._transform = callback;

    return this;
  }

  _parseMeta(collection) {
    const children = [];
    const components = [];
    const links = [];
    const meta = [];

    collection.forEach((item) => {
      const hstMeta = this._transform
        ? this._transform(item)
        : item;
      const type = this._getType(hstMeta);
      let entity;
      try {
        entity = this._build(type, item);
      } catch (e) {
        this.$log.warn(`Ignoring unknown page structure element '${type}'.`);

        return;
      }

      if (entity instanceof EndMarker) {
        components.pop();
      }

      if (entity instanceof PageMeta) {
        meta.push(entity);
      }

      if (entity instanceof ComponentEntity) {
        if (components.length) {
          components[components.length - 1].addComponent(entity);
        } else {
          children.push(entity);
        }

        components.push(entity);
      }

      if (entity instanceof LinkEntity) {
        if (components.length) {
          const [component] = components.slice(-1);
          component.addLink(entity);
          entity.setComponent(component);
        } else {
          links.push(entity);
        }
      }

      if (entity instanceof HeadContributions) {
        if (components.length) {
          components[components.length - 1].addHeadContributions(entity);
        }
      }
    });

    return { children, links, meta };
  }

  _getType(meta) {
    return meta[HstConstants.END_MARKER] === 'true'
      ? HstConstants.END_MARKER
      : meta[HstConstants.TYPE];
  }

  _build(type, ...args) {
    if (!this._builders.has(type)) {
      throw new Error(`Unknown entity type '${type}'.`);
    }

    const builder = this._builders.get(type);

    return builder(...args);
  }
}
