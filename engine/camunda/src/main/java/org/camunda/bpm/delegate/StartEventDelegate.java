package org.camunda.bpm.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class StartEventDelegate implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) {
		System.out.println("Start Event Log");
	}

}
