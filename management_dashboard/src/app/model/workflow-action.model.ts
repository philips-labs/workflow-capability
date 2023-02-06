export interface WorkflowTaskModel {
  id?: any;
  title?: string;
  taskType?: string;
}

export interface WorkflowModel {
  id?: number,
  title?: string,
}

export interface TaskCountModel {
  title?: string,
  count?: number,
}
export interface CompletedCarePlanModel {
  planDefID?: any,
  carePlanID?: any,
  startDate?: any,
  endDate?:any,
  tasks:TaskModel[];
}
export interface TaskModel {
  id?: number,
  title?: string,
}
