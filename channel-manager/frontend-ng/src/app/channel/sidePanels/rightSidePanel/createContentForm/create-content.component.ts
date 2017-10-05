import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import './create-content.scss';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent implements OnInit {
  @Input() form: any;
  @Input() document: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  constructor() {
    console.log('createContent init');
  }

  ngOnInit() {
    console.log(this);
  }

  close() {
    console.log('closerino');
    this.onClose.emit();
  }

  continue() {
    console.log('life is a cycle');
    this.onContinue.emit();
  }
}
