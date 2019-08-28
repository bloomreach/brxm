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

import { Container, ContainerItem, ContainerType } from './api';
import { createPage } from './page';

describe('createPage', () => {
  it('can interpret a single root component', () => {
    const page = createPage({
      page: {
        name: 'test',
        type: 'COMPONENT',
        _meta: {
          pageTitle: 'Test',
          params: {
            foo: 'bar',
          }
        },
      }
    });

    expect(page.getTitle()).toBe('Test');

    const root = page.getComponent();
    expect(root!.getName()).toBe('test');
    expect(root!.getParameters()).toEqual({ foo: 'bar' });
  });

  it('can interpret a root component with a "main" container with two container items', () => {
    const page = createPage({
      page: {
        name: 'test',
        type: 'COMPONENT',
        components: [
          {
            name: 'main',
            type: 'CONTAINER_COMPONENT',
            xtype: 'hst.vbox',
            components: [
              {
                name: 'banner',
                label: 'Banner',
                type: 'CONTAINER_ITEM_COMPONENT',
                _meta: {
                  paramsInfo: {
                    document: 'banners/banner1',
                  },
                  params: {
                    'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'off',
                  },
                },
              },
              {
                name: 'banner1',
                label: 'Banner',
                type: 'CONTAINER_ITEM_COMPONENT',
                _meta: {
                  paramsInfo: {
                    document: 'banners/banner2',
                  },
                  params: {
                    'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'on',
                  }
                }
              }
            ]
          }
        ]
      }
    });

    expect(page.getTitle()).toBe(undefined);

    const root = page.getComponent();
    expect(root!.getName()).toBe('test');
    expect(root!.getParameters()).toEqual({});

    const main = page.getComponent('main') as Container;
    expect(main.getName()).toBe('main');
    expect(main.getType()).toBe(ContainerType.DivDiv);
    expect(main.getParameters()).toEqual({});

    const children = main.getChildren() as ContainerItem[];
    expect(children.length).toBe(2);

    const banner0 = children[0];
    expect(banner0.getName()).toBe('banner');
    expect(banner0.getType()).toBe('Banner');
    expect(banner0.isHidden()).toBe(false);
    expect(banner0.getParameters()).toEqual({ document: 'banners/banner1' });

    const banner1 = children[1];
    expect(banner1.getName()).toBe('banner1');
    expect(banner1.getType()).toBe('Banner');
    expect(banner1.isHidden()).toBe(true);
    expect(banner1.getParameters()).toEqual({ document: 'banners/banner2' });

    expect(page.getComponent('main', 'banner')).toBe(banner0);
    expect(page.getComponent('main', 'banner1')).toBe(banner1);
  })
});
