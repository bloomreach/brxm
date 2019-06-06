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
    private communicationsService: CommunicationsService,
  ) {}

  ngOnInit(): void {
    this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(this.app.url);
  }

  ngAfterViewInit(): void {
    connectToChild({
      iframe: this.iframe.nativeElement,
      methods: this.communicationsService.parentApiMethods,
    }).then(childApi =>
      this.clientAppService.addConnection(this.app.id, childApi),
    );
  }
}
