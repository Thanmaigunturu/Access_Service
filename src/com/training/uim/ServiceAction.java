package com.training.uim;

import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.extensibility.extension.util.ExtensionPointRuleContext;

public class ServiceAction {
	
	public String mapCustomServiceAction_getEntityAction(ExtensionPointRuleContext context, Log log) {
		
		String serviceAction = (String)context.getArguments()[2];
		String convertedServiceAction = null;
		
		log.info("", "mapCustomServiceAction_getEntityAction starts");
		System.out.println("serviceAction in mapCustomServiceAction_getEntityAction is:" +serviceAction);
		if(serviceAction!=null && !serviceAction.isEmpty()) {
			if(serviceAction.equalsIgnoreCase("ADD") || serviceAction.equalsIgnoreCase("CREATE")) {
				convertedServiceAction = "create";
			}else if(serviceAction.equalsIgnoreCase("MODIFY") || serviceAction.equalsIgnoreCase("CHANGE") || serviceAction.equalsIgnoreCase("Update")) {
				convertedServiceAction = "change";
			}else if(serviceAction.equalsIgnoreCase("SUSPEND")) {
				convertedServiceAction = "suspendWithConfiguration";
			}else if(serviceAction.equalsIgnoreCase("Resume")) {
				convertedServiceAction = "resumeWithConfiguration";
			}else if(serviceAction.equalsIgnoreCase("Disconnect") || serviceAction.equalsIgnoreCase("Delete")) {
				convertedServiceAction = "disconnect";
			}else {
				convertedServiceAction = "not null";
			}
		}
		System.out.println("serviceAction in mapCustomServiceAction_getEntityAction is:" +convertedServiceAction);
		log.info("", "mapCustomServiceAction_getEntityAction Ends");
		return convertedServiceAction;
		
	}

}
