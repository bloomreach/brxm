import { Inject } from '@angular/core';
import { Component } from '@angular/core';
import { MD_DIALOG_DATA, MdDialogRef } from '@angular/material';

@Component({
  selector: 'name-url-fields-dialog',
  templateUrl: 'name-url-fields-dialog.html',
})
export class NameUrlFieldsDialog {

  constructor(public dialogRef: MdDialogRef<NameUrlFieldsDialog>, @Inject(MD_DIALOG_DATA) public data: any) { }
  name: string = this.data.name;
  url: string = this.data.url;

  submitDialog(resolve: boolean = false): void {
    if (resolve) {
      return this.dialogRef.close({
        name: this.name,
        url: this.url,
      });
    }
    return this.dialogRef.close(resolve);
  }
}
