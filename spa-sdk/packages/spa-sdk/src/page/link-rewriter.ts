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

import { DOMParser, XMLSerializer } from 'xmldom';
import { injectable, inject } from 'inversify';
import { LinkFactory } from './link-factory';
import { Link, TYPE_LINK_RESOURCE } from './link';

export const DomParserService = Symbol.for('DomParserService');
export const LinkRewriterService = Symbol.for('LinkRewriterService');
export const XmlSerializerService = Symbol.for('XmlSerializerService');

const BODY_CONTENTS = /^<body.*?>(.*)<\/body>$/;

export interface LinkRewriter {
  /**
   * Rewrite links to pages and resources in the HTML content.
   * @param content The HTML content to rewrite links.
   * @param type The content type.
   */
  rewrite(content: string, type?: string): string;
}

@injectable()
export class LinkRewriterImpl implements LinkRewriter {
  constructor(
    @inject(LinkFactory) private linkFactory: LinkFactory,
    @inject(DomParserService) private domParser: DOMParser,
    @inject(XmlSerializerService) private xmlSerializer: XMLSerializer,
  ) {}

  rewrite(content: string, type = 'text/html') {
    const document = this.domParser.parseFromString(`<body>${content}</body>`, type);

    this.rewriteAnchors(document);
    this.rewriteImages(document);

    const body = this.xmlSerializer.serializeToString(document);

    return body.replace(BODY_CONTENTS, '$1');
  }

  private rewriteAnchors(document: Document) {
    Array.from(document.getElementsByTagName('a'))
      .filter(element => element.hasAttribute('href') && element.hasAttribute('data-type'))
      .forEach((element) => {
        const url = this.linkFactory.create({
          href: element.getAttribute('href') ?? undefined,
          type: element.getAttribute('data-type'),
        } as Link);

        if (url) {
          element.setAttribute('href', url);
        }
      });
  }

  private rewriteImages(document: Document) {
    Array.from(document.getElementsByTagName('img'))
      .filter(element => element.hasAttribute('src'))
      .forEach((element) => {
        const url = this.linkFactory.create({
          href: element.getAttribute('src') ?? undefined,
          type: TYPE_LINK_RESOURCE,
        });

        if (url) {
          element.setAttribute('src', url);
        }
      });
  }
}
