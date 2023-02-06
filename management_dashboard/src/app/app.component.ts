import {Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'management-dashboard';
  planDefinitionID: number = 0;

  active: number = 0;
  completed: number = 0;

  fwdMsgToContent(id: number) {
    this.planDefinitionID  = id;
  }


}
