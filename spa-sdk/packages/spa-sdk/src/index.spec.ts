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

import { default as model } from './index.fixture.json';
import { initialize, Container, Page, TYPE_CONTAINER_BOX } from './index';

describe('initialize', () => {
  let page: Page;

  beforeEach(async () => {
    page = await initialize({
      request: { path: '/' },
      options: {
        live: {
          pageModelBaseUrl: 'http://localhost:8080/site/my-spa',
        },
        preview: {
          pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
        },
      },
      httpClient: jest.fn(() => Promise.resolve(model)),
    });
  });

  it('should be a page entity', async () => {
    expect(page.getTitle()).toBe('Homepage');
  });

  it('should contain a root component', async () => {
    const root = page.getComponent();
    expect(root!.getName()).toBe('test');
    expect(root!.getParameters()).toEqual({});
  });

  it('should contain a main component', async () => {
    const main = page.getComponent<Container>('main');

    expect(main).toBeDefined();
    expect(main!.getName()).toBe('main');
    expect(main!.getType()).toBe(TYPE_CONTAINER_BOX);
    expect(main!.getParameters()).toEqual({});
  });

  it('should contain two banners', async () => {
    const main = page.getComponent<Container>('main');
    const children = main!.getChildren();

    expect(children.length).toBe(2);

    const [banner0, banner1] = children;

    expect(banner0.getName()).toBe('banner');
    expect(banner0.getType()).toBe('Banner');
    expect(banner0.isHidden()).toBe(false);
    expect(banner0.getParameters()).toEqual({ document: 'banners/banner1' });

    expect(banner1.getName()).toBe('banner1');
    expect(banner1.getType()).toBe('Banner');
    expect(banner1.isHidden()).toBe(true);
    expect(banner1.getParameters()).toEqual({ document: 'banners/banner2' });

    expect(page.getComponent('main', 'banner')).toBe(banner0);
    expect(page.getComponent('main', 'banner1')).toBe(banner1);
  });

  it('should resolve content references', async () => {
    const banner0 = page.getComponent('main', 'banner');
    const document0 = page.getContent(banner0!.getModels().document);

    const banner1 = page.getComponent('main', 'banner1');
    const document1 = page.getContent(banner1!.getModels().document);

    expect(document0).toBeDefined();
    expect(document0!.getName()).toBe('banner1');
    expect(document1).toBeDefined();
    expect(document1!.getName()).toBe('banner2');
  });
});
