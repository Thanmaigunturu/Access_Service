package com.training.uim;

import java.util.List;
import java.util.Set;

import com.training.uim.helper.UIMHelper;

import oracle.communications.inventory.api.common.EntityUtils;
import oracle.communications.inventory.api.entity.ConfigurationAction;
import oracle.communications.inventory.api.entity.ConfigurationItemAction;
import oracle.communications.inventory.api.entity.ConfigurationType;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.extensibility.extension.util.ExtensionPointContext;
import oracle.communications.inventory.rest.model.ConfigurationStateEnumType;

public class SuspendAccessCapture {
	
	private static final Log log = LogFactory.getLog(SuspendAccessCapture.class);
	
public void suspendAccessCapture(ExtensionPointContext context) throws Exception {
		
		log.info("", "Suspend Action - Started");
		Service cfsService = (Service) context.getArguments()[0];
		System.out.println("cfsScv in Service Action is:" +cfsService.getAdminState());
		System.out.println("cfsScv in Service Action is:" +cfsService.getName());
		System.out.println("cfsScv in Service Action is:" +cfsService.getSpecification());
		
		
		ServiceConfigurationVersion cfsSvc = UIMHelper.getLatestConfigurationVersion(cfsService);
		Service rfsService =UIMHelper.findRFSService(cfsSvc);
		
		ServiceConfigurationVersion rfsScv = UIMHelper.getLatestConfigurationVersion(rfsService);
		List<ServiceConfigurationItem> rfsConfigList = rfsScv.getConfigItems();
		System.out.println("rfsConfigList is:" +rfsConfigList);
		for(ServiceConfigurationItem rfsParentConfigItem : rfsConfigList) {
			System.out.println("rfsParentConfigItem is:" +rfsParentConfigItem);
			if(rfsParentConfigItem!=null && rfsParentConfigItem.getName()!=null && rfsParentConfigItem.getName().equalsIgnoreCase("Device_Access")) {
				System.out.println("Am inside Device_Access");
				EntityUtils.setValue(rfsParentConfigItem, "serviceAction", "SUSPEND");
			}
			
		}
		
		log.info("", "Suspend Action - End");
	}

public void suspendAccessCaptureRequest(ExtensionPointContext context) throws Exception {
	
	log.info("", "Suspend Action - Started");
	Service cfsService = (Service) context.getArguments()[0];
	System.out.println("cfsScv in Service Action is:" +cfsService.getAdminState());
	System.out.println("cfsScv in Service Action is:" +cfsService.getName());
	System.out.println("cfsScv in Service Action is:" +cfsService.getSpecification());
	
	if(cfsService!=null) {
		ServiceConfigurationVersion cfsSvc = UIMHelper.getLatestConfigurationVersion(cfsService);
		if(null!=cfsSvc) {
			ServiceConfigurationVersion newCfsSvc = UIMHelper.createServiceConfiguration(cfsService,"Access_CFS_Configuration");
			
		}
	}
	
	log.info("", "Suspend Action - End");
}



}
