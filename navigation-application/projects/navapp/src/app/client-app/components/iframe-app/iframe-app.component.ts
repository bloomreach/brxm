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

import { CommunicationsService } from '../../../services';
import { ConnectionService } from '../../../services/connection.service';
import { ClientApplicationConfiguration } from '../../models';

@Component({
  selector: 'brna-iframe-app',
  templateUrl: './iframe-app.component.html',
  styleUrls: ['./iframe-app.component.scss'],
})
export class IframeAppComponent implements OnInit, AfterViewInit {
  @Input()
  appConfig: ClientApplicationConfiguration;

  @ViewChild('iframe')
  iframe: ElementRef;

  url: SafeResourceUrl;

  constructor(
    private domSanitizer: DomSanitizer,
    private connectionService: ConnectionService,
    private communicationsService: CommunicationsService,
  ) {}

  ngOnInit(): void {
    this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(
      this.appConfig.url,
    );
  }

  ngAfterViewInit(): void {
    connectToChild({
      iframe: this.iframe.nativeElement,
      methods: this.communicationsService.parentApiMethods,
    }).then(child => {
      this.connectionService.addConnection(this.appConfig, child);
    });
  }
}
