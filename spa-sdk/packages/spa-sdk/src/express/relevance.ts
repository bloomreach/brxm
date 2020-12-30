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

import { default as cookie, CookieSerializeOptions } from 'cookie';
import { IncomingMessage, OutgoingMessage } from 'http';
import { Configuration, Page } from '..';

declare module 'http' {
  interface IncomingMessage {
    visitor: Required<Configuration>['request']['visitor'];
  }
}

const DEFAULT_COOKIE_NAME = '_v';
const DEFAULT_COOKIE_MAX_AGE_IN_SECONDS = 365 * 24 * 60 * 60;

interface HandlerRequest extends Partial<Pick<IncomingMessage, 'headers' | 'visitor'>> {
  once?(event: string | symbol, listener: (...args: any[]) => void): any;
}

type HandlerResponse = Partial<Pick<OutgoingMessage, 'getHeader' | 'setHeader'>>;

interface Handler {
  /**
   * Express middleware for seamless integration with the Relevance Module.
   * @param request Incoming HTTP request.
   * @param response Outgoing HTTP response.
   * @param next
   */
  (request: HandlerRequest, response: HandlerResponse, next?: () => void): void;
}

interface Options extends CookieSerializeOptions {
  name?: string;
}

interface HandlerWithOptions extends Handler {
  /**
   * Customizes Express middleware for the Relevance Module integration.
   * @see https://www.npmjs.com/package/cookie#options
   * @param options Options for the cookie serializer.
   * @return Customed middlware.
   */
  withOptions(options?: Options): Handler;
}

function withOptions({
  httpOnly = true,
  name = DEFAULT_COOKIE_NAME,
  maxAge = DEFAULT_COOKIE_MAX_AGE_IN_SECONDS,
  ...options
}: Options = {}) {
  const handler: Handler = (request, response, next) => {
    const { [name]: value } = cookie.parse(request.headers?.cookie ?? '');

    if (value) {
      try {
        request.visitor = JSON.parse(value);
      } catch {}
    }

    request.once?.('br:spa:initialized', (page: Page) => {
      const visitor = page.getVisitor();
      if (!visitor) {
        return;
      }

      const { new: _, ...value } = visitor;
      const serialized = cookie.serialize(name, JSON.stringify(value), { ...options, httpOnly, maxAge });
      const cookies = response.getHeader?.('set-cookie') ?? [];

      response.setHeader?.('Set-Cookie', [...(Array.isArray(cookies) ? cookies : [cookies]), serialized] as string[]);
    });

    next?.();
  };

  return handler;
}

const relevance: HandlerWithOptions = Object.assign(withOptions(), { withOptions });

export default relevance;
