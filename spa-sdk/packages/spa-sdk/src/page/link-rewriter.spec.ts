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

import { DOMParser, XMLSerializer } from 'xmldom';
import { Factory } from './factory';
import { LinkRewriterImpl, LinkRewriter } from './link-rewriter';
import { Link, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE } from './link';

describe('LinkRewriterImpl', () => {
  let domParser: DOMParser;
  let linkFactory: jest.Mocked<Factory<[Link], string>>;
  let linkRewriter: LinkRewriter;
  let xmlSerializer: XMLSerializer;

  beforeEach(() => {
    domParser = new DOMParser();
    linkFactory = { create: jest.fn() };
    xmlSerializer = new XMLSerializer();

    linkRewriter = new LinkRewriterImpl(linkFactory, domParser, xmlSerializer);
  });

  describe('rewrite', () => {
    it('should ignore anchors without href attribute', () => {
      const html = '<a name="something" data-type="internal">something</a>';

      expect(linkRewriter.rewrite(html)).toBe(html);
      expect(linkFactory.create).not.toBeCalled();
    });

    it('should ignore anchors without data-type attribute', () => {
      const html = '<a href="http://example.com">something</a>';

      expect(linkRewriter.rewrite(html)).toBe(html);
      expect(linkFactory.create).not.toBeCalled();
    });

    it('should rewrite anchor links', () => {
      linkFactory.create.mockReturnValueOnce('url');

      expect(linkRewriter.rewrite('<a href="/some/path" data-type="internal">something</a>'))
        .toBe('<a href="url" data-type="internal">something</a>');
      expect(linkFactory.create).toBeCalledWith({ href: '/some/path', type: TYPE_LINK_INTERNAL });
    });

    it('should ignore images without src attribute', () => {
      const html = '<img alt="something"/>';

      expect(linkRewriter.rewrite(html)).toBe(html);
      expect(linkFactory.create).not.toBeCalled();
    });

    it('should rewrite images links', () => {
      linkFactory.create.mockReturnValueOnce('url');

      expect(linkRewriter.rewrite('<img src="/some/path" alt="something"/>'))
        .toBe('<img src="url" alt="something"/>');
      expect(linkFactory.create).toBeCalledWith({ href: '/some/path', type: TYPE_LINK_RESOURCE });
    });

    it('should pass a content type to the DOM parser', () => {
      spyOn(domParser, 'parseFromString').and.callThrough();
      linkRewriter.rewrite('something', 'text/html');

      expect(domParser.parseFromString).toBeCalledWith(expect.stringContaining('something'), 'text/html');
    });
  });
});
