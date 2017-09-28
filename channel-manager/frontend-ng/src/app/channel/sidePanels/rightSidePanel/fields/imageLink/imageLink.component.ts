import { Component, Input, OnInit } from '@angular/core';
import './imageLink.scss';

@Component({
  selector: 'hippo-image-link',
  templateUrl: './imageLink.html'
})
export class ImageLinkComponent implements OnInit {
  @Input('displayName') displayName: string;

  constructor() {
  }

  ngOnInit() {
    console.log(this.displayName);
  }
}
