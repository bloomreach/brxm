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

@Component({
  selector: 'brna-client-app',
  templateUrl: './client-app.component.html',
  styleUrls: ['./client-app.component.scss'],
})
export class ClientAppComponent implements OnInit, AfterViewInit {
  @Input()
  appURL: string;

  @ViewChild('iframe')
  iframe: ElementRef;

  url: SafeResourceUrl;

  constructor(
    private domSanitizer: DomSanitizer,
    private connectionService: ConnectionService,
    private communicationsService: CommunicationsService,
  ) {}

  ngOnInit(): void {
    this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(this.appURL);
  }

  ngAfterViewInit(): void {
    const connection = connectToChild({
      iframe: this.iframe.nativeElement,
      methods: this.communicationsService.parentApiMethods,
    });

    this.connectionService.addConnection(this.appURL, connection);
  }
}
