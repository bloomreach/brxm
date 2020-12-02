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

import { NgModule } from '@angular/core';

import { ExperimentsModule } from './experiments/experiments.module';
import { NotificationBarModule } from './notification-bar/notification-bar.module';
import { Ng1ServicesModule } from './services/ng1/ng1-services.module';
import { SharedModule } from './shared/shared.module';
import { SiteMapModule } from './site-map/site-map.module';
import { VariantsModule } from './variants/variants.module';
import { VersionsModule } from './versions/versions.module';

@NgModule({
  imports: [
    SharedModule,
    NotificationBarModule,
    VersionsModule,
    VariantsModule,
    ExperimentsModule,
    Ng1ServicesModule,
    SiteMapModule,
  ],
  providers: [
  ],
})
export class AppModule {
  ngDoBootstrap(): void { }
}
