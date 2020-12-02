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

import React from 'react';
import axios from 'axios';
import { NextPageContext } from 'next';
// tslint:disable-next-line:import-name
import getConfig from 'next/config';
import { parseCookies, setCookie } from 'nookies'
import { BrComponent, BrPage, BrPageContext } from '@bloomreach/react-sdk';
import { Configuration, Page, initialize } from '@bloomreach/spa-sdk';
import { Banner, Content, Menu, NewsList } from '../components';
import routes from '../routes';

const VISITOR_COOKIE = '_v';
const VISITOR_COOKIE_MAX_AGE_IN_SECONDS = 365 * 24 * 60 * 60;

const { Link } = routes;
const { publicRuntimeConfig } = getConfig();

interface IndexProps {
  configuration: Configuration;
  page: Page;
}

export default class Index extends React.Component<IndexProps> {
  private static visitor?: Configuration['visitor'];

  static async getInitialProps(context: NextPageContext) {
    const cookies = parseCookies(context);
    const configuration = {
      baseUrl: publicRuntimeConfig.baseUrl,
      endpoint: publicRuntimeConfig.endpoint,
      endpointQueryParameter: 'endpoint',
      request: { path: context.asPath ?? '' },
      visitor:  cookies[VISITOR_COOKIE]
        ? JSON.parse(cookies[VISITOR_COOKIE])
        : Index.visitor,
    };
    const page = await initialize({
      ...configuration,
      httpClient: axios,
      request: {
        ...configuration.request,
        connection: context.req?.connection,
        headers: context.req?.headers['x-forwarded-for']
          ? { 'x-forwarded-for': context.req?.headers['x-forwarded-for'] }
          : undefined,
      },
    });
    configuration.visitor = page.getVisitor();

    if (context.res && configuration.visitor) {
      setCookie(
        context,
        VISITOR_COOKIE,
        JSON.stringify(configuration.visitor),
        { httpOnly: true, maxAge: VISITOR_COOKIE_MAX_AGE_IN_SECONDS },
      );
    }

    if (configuration.endpoint) {
      // Limit the number of hosts that are allowed to embed your application.
      // @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/frame-ancestors
      context?.res?.setHeader(
        'Content-Security-Policy',
        `frame-ancestors 'self' ${new URL(configuration.endpoint, `http://${context.req?.headers.host}`).host}`,
      );
    }

    return { configuration, page };
  }

  componentDidMount() {
    Index.visitor = this.props.configuration.visitor;
  }

  render() {
    const configuration = { ...this.props.configuration, httpClient: axios };
    const mapping = { Banner, Content, 'News List': NewsList, 'Simple Content': Content };

    return (
      <div  className="d-flex flex-column vh-100">
        <BrPage configuration={configuration} mapping={mapping} page={this.props.page}>
          <header>
            <nav className="navbar navbar-expand-sm navbar-dark sticky-top bg-dark" role="navigation">
              <div className="container">
                <BrPageContext.Consumer>
                  { page => (
                    <Link route={page!.getUrl('/')}>
                      <a className="navbar-brand">{ page!.getTitle() || 'brXM + Next.js = ♥️'}</a>
                    </Link>
                  ) }
                </BrPageContext.Consumer>
                <div className="collapse navbar-collapse">
                  <BrComponent path="menu">
                    <Menu />
                  </BrComponent>
                </div>
              </div>
            </nav>
          </header>
          <section className="container flex-fill pt-3">
            <BrComponent path="main" />
          </section>
          <footer className="bg-dark text-light py-3">
            <div className="container clearfix">
              <div className="float-left pr-3">&copy; Bloomreach</div>
              <div className="overflow-hidden">
                <BrComponent path="footer" />
              </div>
            </div>
          </footer>
        </BrPage>
      </div>
    );
  }
}
