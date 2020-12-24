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

import { default as model } from './index.fixture.json';
import {
  destroy,
  initialize,
  Container,
  ContainerItem,
  Page,
  META_POSITION_BEGIN,
  META_POSITION_END,
  TYPE_CONTAINER_BOX,
} from './index';
import { PageModel, TYPE_LINK_RESOURCE, TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL } from './page';

describe('initialize', () => {
  let page: Page;
  const httpClient = jest.fn(async () => ({ data: model as unknown as PageModel }));

  beforeEach(async () => {
    httpClient.mockClear();
    page = await initialize({
      httpClient,
      window,
      baseUrl: '//example.com',
      endpoint: 'http://localhost:8080/site/my-spa/resourceapi',
      path: '/?token=something',
    });
  });

  afterEach(() => {
    destroy(page);
  });

  it('should initialize using endpoint from the query string', async () => {
    httpClient.mockClear();

    const page = await initialize({
      httpClient,
      endpointQueryParameter: 'endpoint',
      request: { path: '/?endpoint=http://example.com/api' },
    });
    destroy(page);

    expect(httpClient).toBeCalled();
    expect(httpClient.mock.calls[0]).toMatchSnapshot();
  });

  it('should be a page entity', () => {
    expect(page.getTitle()).toBe('Homepage');
  });

  it('should contain a root component', () => {
    const root = page.getComponent();
    expect(root!.getName()).toBe('test');
    expect(root!.getParameters()).toEqual({});
  });

  it('should contain page meta-data', () => {
    const [meta1, meta2] = page.getComponent()!.getMeta();

    expect(meta1).toBeDefined();
    expect(meta1.getPosition()).toBe(META_POSITION_END);
    expect(JSON.parse(meta1.getData())).toMatchSnapshot();

    expect(meta2).toBeDefined();
    expect(meta2.getPosition()).toBe(META_POSITION_END);
    expect(JSON.parse(meta2.getData())).toMatchSnapshot();
  });

  it.each`
    link                   | expected
    ${''}                  | ${'//example.com/?token=something'}
    ${'/news'} | ${'//example.com/news?token=something'}
    ${{ href: 'http://127.0.0.1/news?a=b', type: TYPE_LINK_EXTERNAL }}     | ${'http://127.0.0.1/news?a=b'}
    ${{ href: '/news?a=b', type: TYPE_LINK_INTERNAL }}                     | ${'//example.com/news?a=b&token=something'}
    ${{ href: 'news#hash', type: TYPE_LINK_INTERNAL }}                     | ${'//example.com/news?token=something#hash'}
    ${{ href: 'http://127.0.0.1/resource.jpg', type: TYPE_LINK_RESOURCE }} | ${'http://127.0.0.1/resource.jpg'}
  `('should create a URL "$expected" for link "$link"', ({ link, expected }) => {
    expect(page.getUrl(link)).toBe(expected);
  });

  it('should contain a main component', () => {
    const main = page.getComponent<Container>('main');

    expect(main).toBeDefined();
    expect(main!.getName()).toBe('main');
    expect(main!.getType()).toBe(TYPE_CONTAINER_BOX);
    expect(main!.getParameters()).toEqual({});
  });

  it('should contain two banners', () => {
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

  it('should contain components meta-data', () => {
    const [meta1, meta2] = page.getComponent('main', 'banner')!.getMeta();

    expect(meta1).toBeDefined();
    expect(meta1.getPosition()).toBe(META_POSITION_BEGIN);
    expect(JSON.parse(meta1.getData())).toMatchSnapshot();

    expect(meta2).toBeDefined();
    expect(meta2.getPosition()).toBe(META_POSITION_END);
    expect(JSON.parse(meta2.getData())).toMatchSnapshot();
  });

  it('should resolve content references', () => {
    const banner0 = page.getComponent('main', 'banner');
    const document0 = page.getContent(banner0!.getModels().document);

    const banner1 = page.getComponent('main', 'banner1');
    const document1 = page.getContent(banner1!.getModels().document);

    expect(document0).toBeDefined();
    expect(document0!.getName()).toBe('banner1');
    expect(document1).toBeDefined();
    expect(document1!.getName()).toBe('banner2');
  });

  it('should contain content meta-data', () => {
    const banner0 = page.getComponent('main', 'banner');
    const document0 = page.getContent(banner0!.getModels().document);
    const [meta] = document0!.getMeta();

    expect(meta).toBeDefined();
    expect(meta.getPosition()).toBe(META_POSITION_BEGIN);
    expect(JSON.parse(meta.getData())).toMatchSnapshot();
  });

  it('should rewrite content links', () => {
    const banner0 = page.getComponent('main', 'banner');
    const document0 = page.getContent(banner0!.getModels().document);
    const banner1 = page.getComponent('main', 'banner1');
    const document1 = page.getContent(banner1!.getModels().document);

    expect(document0!.getUrl()).toBe('http://127.0.0.1/site/another-spa/banner1.html');
    expect(document1!.getUrl()).toBe('//example.com/banner2.html?token=something');
  });

  it('should rewrite links in the HTML blob', () => {
    const banner = page.getComponent('main', 'banner');
    const document = page.getContent(banner!.getModels().document);
    const { content } = document!.getData<{ content: any }>();

    expect(page.rewriteLinks(content.value)).toMatchSnapshot();
  });

  it('should react on a component rendering', async () => {
    const banner0 = page.getComponent('main', 'banner') as ContainerItem;
    const banner1 = page.getComponent('main', 'banner1') as ContainerItem;
    const listener0 = jest.fn();
    const listener1 = jest.fn();
    const [[id, containerItemModel]] = Object.entries(model.page).filter(([, { id }]: any) => id === 'r1_r1_r1');

    httpClient.mockClear();
    banner0.on('update', listener0);
    banner1.on('update', listener1);

    httpClient.mockImplementationOnce(async () => ({
      data: {
        ...model,
        page: { [id]: containerItemModel },
        root: { $ref: `/page/${id}` },
      } as unknown as PageModel,
    }));

    window.postMessage(
      {
        type: 'brxm:event',
        event: 'update',
        payload: {
          id: 'r1_r1_r1',
          properties: { some: 'value' },
        },
      },
      '*',
    );
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(httpClient).toBeCalled();
    expect(httpClient.mock.calls[0]).toMatchSnapshot();
    expect(listener0).toBeCalled();
    expect(listener1).not.toBeCalled();
  });

  it('should use an origin from the endpoint', async () => {
    const postMessageSpy = spyOn(window.parent, 'postMessage').and.callThrough();
    await page.sync();

    expect(postMessageSpy).toBeCalledWith(expect.anything(), 'http://localhost:8080');
  });

  it('should use an origin from the endpoint parameter', async () => {
    const page = await initialize({
      httpClient,
      endpointQueryParameter: 'endpoint',
      request: { path: '/?endpoint=https://api.example.com/api' },
    });
    const postMessageSpy = spyOn(window.parent, 'postMessage').and.callThrough();
    await page.sync();
    destroy(page);

    expect(postMessageSpy).toBeCalledWith(expect.anything(), 'https://api.example.com');
  });

  it('should use a custom origin', async () => {
    const page = await initialize({
      httpClient,
      window,
      endpoint: 'http://localhost:8080/site/my-spa/resourceapi',
      origin: 'http://localhost:12345',
      request: { path: '/?token=something' },
    });
    const postMessageSpy = spyOn(window.parent, 'postMessage').and.callThrough();
    await page.sync();
    destroy(page);

    expect(postMessageSpy).toBeCalledWith(expect.anything(), 'http://localhost:12345');
  });
});
