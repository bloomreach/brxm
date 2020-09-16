/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../services/ng1/channel.ng1service';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../services/ng1/config.ng1.service';
import { Variant } from '../models/variant.model';

export interface ApiResponseBody {
  data: any;
  errorCode: string | null;
  message: string;
  reloadRequired: boolean;
  success: boolean;
}

export const hstApiPrefix = '_rp/';

@Injectable({
  providedIn: 'root',
})
export class VariantsService {
  constructor(
    @Inject(NG1_CONFIG_SERVICE) private readonly configService: Ng1ConfigService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly channelService: Ng1ChannelService,
    private readonly http: HttpClient,
  ) {}

  getVariantIds(componentId: string): Observable<string[]> {
    const channelContextPath = this.channelService.getChannel().contextPath;
    const cmsContextPath = this.configService.getCmsContextPath();
    const url = `${cmsContextPath}${hstApiPrefix}${componentId}`;

    return this.http.get<ApiResponseBody>(url, {
      headers: {
        contextPath: channelContextPath,
      },
    }).pipe(
      map(response => response.data),
    );
  }

  getVariants(variantIds: string[]): Observable<Variant[]> {
    const channelContextPath = this.channelService.getChannel().contextPath;
    const cmsContextPath = this.configService.getCmsContextPath();
    const variantsUuid = this.configService.variantsUuid;
    const locale = this.configService.locale;
    const url = `${cmsContextPath}${hstApiPrefix}${variantsUuid}./componentvariants?locale=${locale}`;

    return this.http.post<ApiResponseBody>(url, variantIds, {
      headers: {
        contextPath: channelContextPath,
      },
    }).pipe(
      map(response => response.data),
    );
  }
}
