import { Injectable } from '@angular/core';
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {url} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {

  constructor(private http: HttpClient) { }

  getAllCarePlans(id:number):Observable<any>{
    return this.http.get(url.CarePlanStatus + id);
  }
}
