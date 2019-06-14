/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnInit,
  ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { connectToChild } from '@bloomreach/navapp-communication';

import { AppToNavAppService } from '../../../services/app-to-nav-app.service';
import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services';

@Component({
  selector: 'brna-client-app',
  templateUrl: './client-app.component.html',
  styleUrls: ['./client-app.component.scss'],
})
export class ClientAppComponent implements OnInit, AfterViewInit {
  @Input()
  app: ClientApp;

  @ViewChild('iframe')
  iframe: ElementRef<HTMLIFrameElement>;

  url: SafeResourceUrl;

  constructor(
    private domSanitizer: DomSanitizer,
    private clientAppService: ClientAppService,
    private appToNavAppService: AppToNavAppService,
  ) {}

  ngOnInit(): void {
    this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(this.app.url);
  }

  ngAfterViewInit(): void {
    connectToChild({
      iframe: this.iframe.nativeElement,
      methods: this.appToNavAppService.parentApiMethods,
    }).then(childApi =>
      this.clientAppService.addConnection(this.app.id, childApi),
    );
  }
}
