/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
  SecurityContext,
  ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Connection } from '../../../models/connection.model';
import { FailedConnection } from '../../../models/failed-connection.model';
import { ConnectionService } from '../../../services/connection.service';
import { ClientAppService } from '../../services/client-app.service';

@Component({
  selector: 'brna-client-app',
  templateUrl: 'client-app.component.html',
  styleUrls: ['client-app.component.scss'],
})
export class ClientAppComponent implements OnInit, AfterViewInit {
  @Input()
  url: string;

  @ViewChild('iframe', { static: true })
  iframe: ElementRef<HTMLIFrameElement>;

  safeUrl: SafeResourceUrl;

  constructor(
    private readonly domSanitizer: DomSanitizer,
    private readonly connectionService: ConnectionService,
    private readonly clientAppService: ClientAppService,
  ) { }

  ngOnInit(): void {
    const sanitizedUrl = this.domSanitizer.sanitize(SecurityContext.URL, this.url);
    this.safeUrl = this.domSanitizer.bypassSecurityTrustResourceUrl(sanitizedUrl);
  }

  ngAfterViewInit(): void {
    this.connect();
  }

  private connect(): void {
    const nativeIFrame = this.iframe.nativeElement;
    const url = nativeIFrame.src;

    this.connectionService
      .connectToIframe(nativeIFrame)
      .then(
        api => this.clientAppService.addConnection(new Connection(url, api)),
        error => this.clientAppService.handleFailedConnection(new FailedConnection(url, error)),
      );
  }
}
