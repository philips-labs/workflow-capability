package org.camunda.bpm.delegate;

import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class StartEventDelegate implements JavaDelegate {
	private static final Logger logger = Logger.getLogger(StartEventDelegate.class.getName());
	@Override
	public void execute(DelegateExecution execution) {
		logger.info("Start Event Log");
	}

}
