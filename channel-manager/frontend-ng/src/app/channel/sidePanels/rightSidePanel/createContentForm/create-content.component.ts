import { Component, EventEmitter, Input, Output } from '@angular/core';
import './create-content.scss';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent {
  @Input() document: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  constructor() {}

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    this.onContinue.emit(form.value);
  }
}
