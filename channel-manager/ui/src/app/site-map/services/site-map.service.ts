/*!
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../services/ng1/channel.ng1.service';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../services/ng1/config.ng1.service';
import { SnackBarService } from '../../services/snack-bar.service';
import { StateService } from '../../services/state.service';
import { SiteMapItem, SiteMapResponse, SiteMapState } from '../models/site-map-item.model';

const initialState: SiteMapState = {
  items: [],
  search: [],
  loading: false,
};

@Injectable({
  providedIn: 'root',
})
export class SiteMapService extends StateService<SiteMapState> {

  get items(): SiteMapItem[] {
    return this.state.items;
  }

  items$: Observable<SiteMapItem[]>;
  search$: Observable<SiteMapItem[]>;
  loading$: Observable<boolean>;

  baseUrl = Location.joinWithSlash(
    this.ng1ConfigService.getCmsContextPath(), `_rp`,
  );

  constructor(
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_CONFIG_SERVICE) private readonly ng1ConfigService: Ng1ConfigService,
    private readonly http: HttpClient,
    private readonly snackBarService: SnackBarService,
  ) {
    super(initialState);

    this.items$ = this.select(state => state.items);
    this.search$ = this.select(state => state.search);
    this.loading$ = this.select(state => state.loading);
  }

  getHeaders(): Record<string, string> {
    return {
      'CMS-User': this.ng1ConfigService.cmsUser,
      contextPath: this.ng1ChannelService.getChannel().contextPath,
      hostGroup: this.ng1ChannelService.getChannel().hostGroup,
    };
  }

  search(siteMapId: string, query: string): void {
    const url = Location.joinWithSlash(this.baseUrl, `/${siteMapId}./search`);
    this.http.get<SiteMapResponse>(url, {
      headers: this.getHeaders(),
      params: {
        fq: query,
      },
    }).subscribe(res => {
        this.setState({ search: [res.data] });
      },
      this.onError.bind(this),
      this.onComplete.bind(this));
  }

  load(siteMapId: string): void {
    const url = Location.joinWithSlash(this.baseUrl, `/${siteMapId}./sitemapitem`);
    this.http.get<SiteMapResponse>(url, {
      headers: this.getHeaders(),
    }).subscribe(res => {
        this.setState({ items: [res.data] });
      },
      this.onError.bind(this),
      this.onComplete.bind(this));
  }

  loadItem(
    siteMapId: string,
    path: string,
    isSearchMode: boolean,
    ancestry = false,
    noMerge = false): void {
    const url = Location.joinWithSlash(this.baseUrl, `/${siteMapId}./sitemapitem/${path}`);
    this.http.get<SiteMapResponse>(url, {
      headers: this.getHeaders(),
      params: {
        ancestry: ancestry.toString(),
      },
    }).subscribe(res => {
        const prop = isSearchMode ? 'search' : 'items';
        let tmp = { ...this.state[prop][0] };
        const pathKeys = path.split('/').filter(key => key !== '');
        if (noMerge) {
          tmp = res.data;
        } else if (ancestry) {
          tmp = this.deepMergeAncestryItems(pathKeys, res.data, tmp);
        } else {
          tmp = this.deepMergeChildrenItems(pathKeys, res.data, tmp);
        }
        this.setState({ [prop]: [tmp] });
      },
      this.onError.bind(this),
      this.onComplete.bind(this));
  }

  deepMergeChildrenItems(keys: string[], apiResponseItem: SiteMapItem, stateItem: SiteMapItem): SiteMapItem {
    const el = stateItem?.children?.find((child: SiteMapItem) => child.id === keys[0]);
    if (el) {
      const foundIndex = stateItem.children.findIndex(x => x.id === keys[0]);
      const childrenArr = [...stateItem.children];
      childrenArr[foundIndex] = this.deepMergeChildrenItems(keys.slice(1), apiResponseItem, el);
      return {...stateItem, children: childrenArr };
    } else {
      return apiResponseItem;
    }
  }

  deepMergeAncestryItems(keys: string[], apiResponseItem: SiteMapItem, stateItem: SiteMapItem): SiteMapItem {
    const el1 = apiResponseItem?.children?.find((child: SiteMapItem) => child.id === keys[0]);
    const el2 = stateItem?.children?.find((child: SiteMapItem) => child.id === keys[0]);
    if (el1 && el2) {
      const foundIndex = stateItem.children.findIndex(x => x.id === keys[0]);
      const childrenArr = [...stateItem.children];
      childrenArr[foundIndex] = this.deepMergeAncestryItems(keys.slice(1), el1, el2);
      return {...stateItem, children: childrenArr };
    } else {
      return apiResponseItem;
    }
  }

  onError({ message, status }: HttpErrorResponse): void {
    this.snackBarService.warning(`${message} (${status})`);
  }

  onComplete(): void {
    this.setState({
      loading: false,
    });
  }
}
