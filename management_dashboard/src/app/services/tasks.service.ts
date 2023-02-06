import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {url} from "../../environments/environment";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class TasksService {

  constructor(private http: HttpClient) { }

  getTasks(): Observable<any> {
    return this.http.get(url.Task);
  }
}
