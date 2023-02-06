import {Component, Input, OnChanges, OnInit, ViewChild} from '@angular/core';
import {ChartDataSets, ChartOptions, ChartType} from "chart.js";
import {Label, MultiDataSet} from "ng2-charts";
import {WorkflowService} from "../../services/workflow.service";
import {PlanDefinitionService} from "../../services/plan-definition.service";
import {CompletedCarePlanModel, TaskCountModel, TaskModel, WorkflowTaskModel} from "../../model/workflow-action.model";
import {PatientService} from "../../services/patient.service";
import {TasksService} from "../../services/tasks.service";
import {MatTableDataSource} from "@angular/material/table";
import {MatPaginator} from "@angular/material/paginator";
import {MatSort} from "@angular/material/sort";


@Component({
  selector: 'app-content',
  templateUrl: './content.component.html',
  styleUrls: ['./content.component.css']
})
export class ContentComponent implements OnInit, OnChanges {
  active: number = 0;
  completed: number = 0;
  totalPatientNumber: number = 0;
  totalWFPatientNumber: number = 0;
  @Input() planDefinitionID: number = 0;
  workflowTasks: WorkflowTaskModel[] = [];

  patientChartLabels: Label[] = ['InProgress', 'Completed'];
  patientChartData: MultiDataSet = [[this.active, this.completed]];
  patientChartType: ChartType = 'doughnut';

  barChartOptions: ChartOptions = {responsive: true,};
  barChartLabels: Label[] = [];
  barChartType: ChartType = 'bar';
  barChartLegend = true;
  barChartPlugins = [];
  barChartData: ChartDataSets[] = [{data: [], label: 'Patients'}];

  private allTasksMap = new Map<number, string>();
  completedCarePlan: CompletedCarePlanModel[] = [];
  completedTasks: TaskModel[] = [];
  tasksCount: TaskCountModel[] = [];


  displayedColumns = ['title','count']
  dataSource: MatTableDataSource<any>
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(private wfcService: WorkflowService, private planDefinitionSrv: PlanDefinitionService,
              private tasksService: TasksService, private patientService: PatientService) {
    this.getAllTasks();


  }

  ngOnInit(): void {
    this.completedPlans()
    this.totalPatient();

  }


  ngOnChanges() {
    this.completedPlans()
    this.updatePieChartData();
    this.totalPatient()
  }

  private updatePieChartData() {
    this.active = 0;
    this.completed = 0;
    this.totalWFPatientNumber = 0;

    this.wfcService.getAllCarePlans(this.planDefinitionID).subscribe(param => {

      if (param.entry) {
        this.totalWFPatientNumber = param.total;
        param.entry.forEach((i: any) => {
          if (i.resource.status == 'active') {
            this.active++;
          } else {
            this.completed++
          }
        })
      }
      this.patientChartData = [
        [this.active, this.completed]
      ];
    });
  }

  totalPatient() {
    this.patientService.getAllPatients().subscribe(data => {
      this.totalPatientNumber = data.total;
    })
  }

  private getAllTasks() {
    this.tasksService.getTasks().subscribe(
      taskItem => {
        taskItem.entry.forEach((entry: any) => {
          entry.resource.identifier.forEach((item: any) => {
              if (item.system == "taskName" && entry.resource.status == "completed") {
                this.allTasksMap.set(entry.resource.id, item.value)
              }
            }
          )
        })
      }
    )
  }

  completedPlans(): void {
    this.completedCarePlan = [];
    let taskName: string = ""
    let taskId: number = 0;
    let planDefId: number = 0;
    let carePId: number = 0

    this.workflowTasks = [];
    this.tasksCount = [];
    this.barChartLabels = [];
    this.barChartData[0].data = []
    this.dataSource = new MatTableDataSource<any>(this.tasksCount)

    if (this.planDefinitionID > 0) {
      this.wfcService.getAllCarePlans(this.planDefinitionID).subscribe(
        pd => {
          pd.entry.forEach((act: any) => {
              planDefId = act.resource.instantiatesCanonical[0]
                .substring(act.resource.instantiatesCanonical[0]
                  .indexOf("/") + 1)

              this.completedTasks = [];

              act.resource.activity.forEach((activity: any) => {
                carePId = act.resource.id
                taskId = activity.reference.reference.substring(activity.reference.reference.indexOf("/") + 1)
                taskName = <string>this.allTasksMap.get(taskId)
                this.completedTasks.push({id: taskId, title: taskName})
              })
              this.completedCarePlan.push({planDefID: planDefId, carePlanID: carePId, tasks: this.completedTasks})
            }
          );
          this.getPlanDefTasks();
        }
      )
    }
  }

  private getPlanDefTasks() {
    this.workflowTasks = [];
    this.tasksCount = [];
    this.barChartLabels = [];
    this.barChartData[0].data = []
    this.dataSource = new MatTableDataSource<any>(this.tasksCount)

    if (this.planDefinitionID > 0) {
      this.planDefinitionSrv.getPlanDefinitionsById(this.planDefinitionID).subscribe(
        pd => {
          pd.action.forEach((act: any) => {
            this.workflowTasks.push({
              id: act.id,
              title: act.title,
              taskType: act?.code[0]?.coding[0]?.code,
            } as WorkflowTaskModel);

            this.tasksCount.push({
              title: act.title,
              count: this.getCount(act.title)
            })
          });

          this.dataSource = new MatTableDataSource<any>(this.tasksCount)
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;


          this.tasksCount.forEach(ac => {
            if (ac.title) {
              this.barChartLabels.push(ac.title);
              this.barChartData[0].data?.push(<number>ac.count)
            }
          });
          this.barChartLabels.push("");
          this.barChartData[0].data?.push(0)


        }
      )
    }

  }
  private getCount(title: string): number {
    let count: number = 0;
    this.completedCarePlan.forEach((element) => {
      element.tasks.forEach((item) => {
        if (title == item.title)
          count++
      })
    })
    return count;
  }


  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}
