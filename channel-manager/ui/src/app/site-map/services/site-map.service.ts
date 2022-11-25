/*
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { SiteMapItem, SiteMapResponse, SiteMapState } from '../models/site-map-item.model';
import { StateService } from '../../services/state.service';
import { NG1_CHANNEL_SERVICE, Ng1ChannelService } from '../../services/ng1/channel.ng1.service';
import { NG1_CONFIG_SERVICE, Ng1ConfigService } from '../../services/ng1/config.ng1.service';
import { SnackBarService } from '../../services/snack-bar.service';

const initialState: SiteMapState = {
  items: [],
  search: [],
  renderPathInfo: '',
  loading: false,
};

@Injectable({
  providedIn: 'root',
})
export class SiteMapService extends StateService<SiteMapState> {
  items$: Observable<SiteMapItem[]>;
  search$: Observable<SiteMapItem[]>;
  renderPathInfo$: Observable<string>;
  loading$: Observable<boolean>;

  constructor(
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_CONFIG_SERVICE) private readonly ng1ConfigService: Ng1ConfigService,
    private readonly http: HttpClient,
    private readonly snackBarService: SnackBarService,
  ) {
    super(initialState);

    this.items$ = this.select((state) => state.items);
    this.search$ = this.select((state) => state.search);
    this.loading$ = this.select((state) => state.loading);
    this.renderPathInfo$ = this.select((state) => state.renderPathInfo);
  }

  baseUrl = Location.joinWithSlash(
    this.ng1ConfigService.getCmsContextPath(), `_rp/${this.ng1ChannelService.getSiteMapId()}./`,
  );

  headers = {
    'CMS-User': this.ng1ConfigService.cmsUser,
    contextPath: this.ng1ChannelService.getChannel().contextPath,
    hostGroup: this.ng1ChannelService.getChannel().hostGroup,
  }

  set renderPathInfo(path: string) {
    this.setState({ renderPathInfo: path });
  }

  get items(): SiteMapItem[] {
    return this.state.items;
  }

  search(query: string): void {
    const url = Location.joinWithSlash(this.baseUrl, 'search');
    this.http.get<SiteMapResponse>(url, {
      headers: this.headers,
      params: {
        fq: query,
      },
    }).subscribe((res) => {
      this.setState({ search: [res.data] });
    },
    this.onError.bind(this),
    this.onComplete.bind(this));
  }

  load(): void {
    const url = Location.joinWithSlash(this.baseUrl, 'sitemapitem');
    this.http.get<SiteMapResponse>(url, {
      headers: this.headers,
    }).subscribe((res) => {
      this.setState({ items: [res.data] });
    },
    this.onError.bind(this),
    this.onComplete.bind(this));
  }

  loadItem(path: string, isSearchMode: boolean, ancestry = false): void {
    const url = Location.joinWithSlash(this.baseUrl, `sitemapitem/${path}`);
    this.http.get<SiteMapResponse>(url, {
      headers: this.headers,
      params: {
        ancestry: ancestry.toString(),
      },
    }).subscribe((res) => {
      const prop = isSearchMode ? 'search' : 'items';
      if (ancestry) {
        this.setState({ [prop]: [res.data] });
      } else {
        const tmp = { ...this.state[prop][0] };
        this.deepMergeItems(path.split('/'), res.data, tmp);
        this.setState({ [prop]: [tmp] });
      }
    },
    this.onError.bind(this),
    this.onComplete.bind(this));
  }

  deepMergeItems(keys: string[], item1: SiteMapItem, item2?: SiteMapItem): void {
    if (keys.length !== 0 && item2) {
      const el = item2.children.find((child: SiteMapItem) => child.id === keys[0]);
      this.deepMergeItems(keys.slice(1), item1, el);
    } else if (item2) {
      item2.children = [...item1.children];
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
