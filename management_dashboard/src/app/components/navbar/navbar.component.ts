import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {PlanDefinitionService} from "../../services/plan-definition.service";
import {WorkflowModel} from "../../model/workflow-action.model";

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  id: number = 0;
  active: number = 0;
  completed: number = 0;
  workflows:WorkflowModel[] = [];
  @Output() msg = new EventEmitter<any>();

  constructor(private planDefinitionSrv: PlanDefinitionService) {
    this.planDefinitionSrv.getAllPlanDefinitions().subscribe(data => data.entry.forEach((i: any) => {
      this.workflows.push({id: i.resource.id, title:i.resource.name });
    }));
  }

  ngOnInit(): void {
  }

  onClick() {
    this.msg.emit(this.id);
  }
}
