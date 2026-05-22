package com.training.uim;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.apache.xmlbeans.impl.values.XmlStringImpl;

import com.training.uim.helper.UIMHelper;

import oracle.communications.inventory.api.businessinteraction.BusinessInteractionManager;
import oracle.communications.inventory.api.common.EntityUtils;
import oracle.communications.inventory.api.configuration.BaseConfigurationManager;
import oracle.communications.inventory.api.entity.BusinessInteraction;
import oracle.communications.inventory.api.entity.LogicalDevice;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationItemCharacteristic;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.api.framework.security.UserEnvironmentFactory;
import oracle.communications.inventory.api.service.ServiceConfigurationManager;
import oracle.communications.inventory.extensibility.extension.util.ExtensionPointContext;
import oracle.communications.inventory.sfws.service.ServiceType;
import oracle.communications.inventory.xmlbeans.BusinessInteractionItemType;
import oracle.communications.inventory.xmlbeans.ParameterType;
import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;

public class DesignServiceAction {
	
	private static final Log log = LogFactory.getLog(DesignServiceAction.class);
	
	public void design(ExtensionPointContext context) throws Exception {
		
		log.info("", "Design Service Action - Started");
		ServiceConfigurationVersion cfsScv = (ServiceConfigurationVersion) context.getArguments()[0];
		BusinessInteractionItemType biItemType = (BusinessInteractionItemType)context.getArguments()[1];
		
		// in the case of Capture Interaction - Create request, Service Configuration should have been already created.
		if(cfsScv==null) {
			log.validationException("Service Configuration needs to be created first" , new java.lang.IllegalArgumentException());
		}
		
		// get the Service Action from the request
		String serviceAction = biItemType.getService().getAction();
		System.out.println("serviceAction is: "+serviceAction);
		
		if(serviceAction!=null && !serviceAction.isEmpty() && (serviceAction.equalsIgnoreCase("add") || serviceAction.equalsIgnoreCase("Create"))) {
			designAdd(cfsScv, biItemType);
		} else if(serviceAction!=null && !serviceAction.isEmpty() && (serviceAction.equalsIgnoreCase("modify") || serviceAction.equalsIgnoreCase("change") || serviceAction.equalsIgnoreCase("update"))) {
			String serviceId = biItemType.getService().getId();
			String serviceName = biItemType.getService().getName();
			String externalIdentityObject =null;
			if(null!=biItemType.getExternalIdentity()) {
				 externalIdentityObject = biItemType.getExternalIdentity().getExternalObjectId();
			}
			// check if the service Id or service Name or External Object Id is provided or not.
			System.out.println("Service Id:" +serviceId);
			System.out.println("Service Name:" +serviceName);
			System.out.println("externalIdentityObject:" +externalIdentityObject);
			if((serviceId == null || serviceId.isEmpty()) && (serviceName == null || serviceName.isEmpty()) && (externalIdentityObject == null || externalIdentityObject.isEmpty()) ) {
				throw new ValidationException("Service Id or Service Name or External Identity Object needs to be provided");
			}
			Service cfsService = null;
			if(serviceId!=null && !serviceId.isEmpty()) {
				cfsService= UIMHelper.findService(serviceId, "Id");
			}else if(serviceName != null && !serviceName.isEmpty()) {
				cfsService = UIMHelper.findService(serviceName,"Name" );
			}else if(externalIdentityObject != null && !externalIdentityObject.isEmpty()) {
				cfsService = UIMHelper.findService(externalIdentityObject,"externalIdentityObject");
			}else {
				throw new ValidationException("Service not found with the provided service details");
			}
			System.out.println("cfsService.getAdminState() is" +cfsService.getAdminState());
			
			
			//ServiceConfigurationVersion cfsscv = cfsService.getConfigurations().get(cfsService.getConfigurations().size()-1);
			ServiceConfigurationVersion cfsscv = UIMHelper.getLatestConfigurationVersion(cfsService);
			designChange(cfsscv, biItemType);
			/**
			if(null!= cfsService && cfsService.getAdminState().getValue().equalsIgnoreCase("IN_SERVICE")) {
				ServiceConfigurationVersion cfsscv = cfsService.getConfigurations().get(cfsService.getConfigurations().size()-1);
				designChange(cfsscv, biItemType);
			}else {
				throw new ValidationException("Service should be in In Service State for Update/Modify/Change Action");
			}
			**/
		}
		
		log.info("", "Design Service Action - End");
	}

	private void designChange(ServiceConfigurationVersion cfsscv, BusinessInteractionItemType biItemType) throws Exception {
		log.info("", "designChange method - START"); 
		
		String mediaType =null, peDeviceName = null, switchHostName = null, oltDeviceName=null, mwType = null, modifyType=null, effectiveDate = null;
		String biId = null;
		
		System.out.println("Business Id:" +cfsscv.getId());
		
		if(biItemType!=null && biItemType.getParameterList()!=null) {
			List<ParameterType> paramTypeList = biItemType.getParameterList();
			if(paramTypeList!=null && !paramTypeList.isEmpty()) {
				for(ParameterType paramType : paramTypeList) {
					if(paramType!=null && paramType.getName()!=null) {
						
						if(paramType.getName().equals("mediaType")) {
							mediaType = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}else if(paramType.getName().equals("peDeviceName")) {
							peDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}else if(paramType.getName().equals("switchHostName")) {
							switchHostName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						} else if (paramType.getName().equals("oltDeviceName")){
							oltDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						} else if (paramType.getName().equals("mwType")){
							mwType = ((XmlObjectBase) paramType.getValue()).getStringValue();
						} else if (paramType.getName().equals("modifyType")){
							modifyType = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}
					}
				}
				
			}	
		}
		
		if(modifyType!=null && !modifyType.isEmpty() && modifyType.equalsIgnoreCase("MediaChange")) {
			BusinessInteraction currentBI = (BusinessInteraction)UserEnvironmentFactory.getBusinessInteraction();
			Finder f = PersistenceHelper.makeFinder();
			if(currentBI == null){
				System.out.println("current Bi is null");
				BusinessInteraction bi = (BusinessInteraction)f.findById(BusinessInteraction.class, "biId").iterator().next();
				currentBI = bi;
			}
			
			BusinessInteractionManager biMgr =  PersistenceHelper.makeBusinessInteractionManager();
			biMgr.switchContext(currentBI, null);
			
			Service rfsService =UIMHelper.findRFSService(cfsscv);
			ServiceConfigurationVersion rfsScv = UIMHelper.getLatestConfigurationVersion(rfsService);
			System.out.println("Existing eff Date: " +rfsScv.getEffDate());
			// create a new configuration version in case the configuration is completed.
			ServiceConfigurationVersion rfsnewScv = UIMHelper.createNewServiceConfiguration(rfsService);
			System.out.println("Existing eff Date: " +rfsnewScv.getEffDate());
			// get the characteristics under rfs service
			List<ServiceConfigurationItem> rfsConfigList = rfsnewScv.getConfigItems();
			BaseConfigurationManager configManager = PersistenceHelper.makeConfigurationManager(rfsnewScv.getClass());
			String mediaTypeExisting = null;
			for(ServiceConfigurationItem rfsParentConfigItem : rfsConfigList) {
				if(rfsParentConfigItem!=null && rfsParentConfigItem.getName()!=null && rfsParentConfigItem.getName().equalsIgnoreCase("Device_Access")) {
					
					Set<ServiceConfigurationItemCharacteristic> existingMediaTypeSet = rfsParentConfigItem.getCharacteristics();
					for(ServiceConfigurationItemCharacteristic existingMediaType: existingMediaTypeSet) {
						if(existingMediaType.getName()!=null && existingMediaType.getName().equalsIgnoreCase("mediaType")){
							mediaTypeExisting = existingMediaType.getValue();
						}
					}
					if(peDeviceName!=null && !peDeviceName.isEmpty()) {
						System.out.println("It is Device_Access Config Item");
						ServiceConfigurationItem rfsChildPeDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentConfigItem, "PE_Device");
						
						if(rfsChildPeDeviceItem!=null) {
							System.out.println("after null check rfsChildPeDeviceItem");
								System.out.println("peDeviceName in designChange is:"+peDeviceName);
								boolean isPeDeviceSame = UIMHelper.findLogicalDeviceBySpec(peDeviceName, "peDevice");
								if(!isPeDeviceSame) {
									
									List<ServiceConfigurationItem> peDeviceChildConfigItemList = new ArrayList<>();
									peDeviceChildConfigItemList.add(rfsChildPeDeviceItem);
									
									configManager.unallocateInventoryConfigurationItems(peDeviceChildConfigItemList);
									LogicalDevice ldPeDevice = UIMHelper.findOrCreateLogicalDevice(peDeviceName, "peDevice");
									configManager.assignResource(rfsChildPeDeviceItem, ldPeDevice, null, null);
									EntityUtils.setValue(rfsChildPeDeviceItem, "peDeviceName", peDeviceName);
								}
						}
					}
					
					ServiceConfigurationItem rfsChildSwitchDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentConfigItem, "Switch_Device");
					ServiceConfigurationItem rfsChildOltDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentConfigItem, "OLT_Device");
					
					if(mediaType!=null && !mediaType.isEmpty()) {
						
						if(mediaTypeExisting!=null && !mediaType.equalsIgnoreCase(mediaTypeExisting)) {
						
							// unallocating the resources - starts
							if(mediaTypeExisting.equalsIgnoreCase("Fiber") || mediaTypeExisting.equalsIgnoreCase("Microwave")) {
								if(rfsChildSwitchDeviceItem!=null) {
									List<ServiceConfigurationItem> peDeviceChildConfigItemList = new ArrayList<>();
									peDeviceChildConfigItemList.add(rfsChildSwitchDeviceItem);
									configManager.unallocateInventoryConfigurationItems(peDeviceChildConfigItemList);
								}
							}else if(mediaTypeExisting.equalsIgnoreCase("GPON")) {
								if(rfsChildOltDeviceItem!=null) {
									List<ServiceConfigurationItem> peDeviceChildConfigItemList = new ArrayList<>();
									peDeviceChildConfigItemList.add(rfsChildOltDeviceItem);
									configManager.unallocateInventoryConfigurationItems(peDeviceChildConfigItemList);
									
								}
							}
						}
						// unallocating the resources - Ends
						// for assigning the resource - Starts
						if(mediaType.equalsIgnoreCase("Fiber") || mediaType.equalsIgnoreCase("Microwave")) {
							// create switch ld and assign switch
							if(switchHostName!=null && !switchHostName.isEmpty()) {
								rfsChildSwitchDeviceItem = UIMHelper.createconfigItem(rfsnewScv, "Switch_Device");
								if(rfsChildSwitchDeviceItem!=null) {
									LogicalDevice ldDevice = UIMHelper.findOrCreateLogicalDevice(switchHostName, "accessDevice");
									configManager.assignResource(rfsChildSwitchDeviceItem, ldDevice, null, null);
									EntityUtils.setValue(rfsChildSwitchDeviceItem, "switchHostName", switchHostName);
								}
							}
							if(mediaType.equalsIgnoreCase("Microwave") && mwType!=null && !mwType.isEmpty()) {
								EntityUtils.setValue(rfsParentConfigItem, "mwType", mwType);
							}
						} else if(mediaType.equalsIgnoreCase("GPON")) {
							if(oltDeviceName!=null && !oltDeviceName.isEmpty()) {
								rfsChildOltDeviceItem = UIMHelper.findOrCreateChildConfigItem(rfsParentConfigItem, "OLT_Device", "OLT_Device");
								if(rfsChildOltDeviceItem!=null) {
									LogicalDevice ldDevice = UIMHelper.findOrCreateLogicalDevice(oltDeviceName, "oltDevice");
									configManager.assignResource(rfsChildOltDeviceItem, ldDevice, null, null);
									EntityUtils.setValue(rfsChildOltDeviceItem, "oltDeviceName", oltDeviceName);
										
								}
							}
						}
						// for assigning the resource - Ends
						
					}
					
					
				}
			}
			currentBI = null;
			biMgr.switchContext(currentBI, null);
		}
		
		log.info("", "designChange method - END"); 
	}

	private void designAdd(ServiceConfigurationVersion cfsScv, BusinessInteractionItemType biItemType) throws Exception {
		
		String mediaType =null, peDeviceName = null, switchHostName = null, oltDeviceName=null, mwType = null;
		
		if(biItemType!=null && biItemType.getParameterList()!=null) {
			List<ParameterType> paramTypeList = biItemType.getParameterList();
			if(paramTypeList!=null && !paramTypeList.isEmpty()) {
				for(ParameterType paramType : paramTypeList) {
					if(paramType!=null && paramType.getName()!=null) {
						
						if(paramType.getName().equals("mediaType")) {
							mediaType = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}else if(paramType.getName().equals("peDeviceName")) {
							peDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}else if(paramType.getName().equals("switchHostName")) {
							switchHostName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						} else if (paramType.getName().equals("oltDeviceName")){
							oltDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
						} else if (paramType.getName().equals("mwType")){
							mwType = ((XmlObjectBase) paramType.getValue()).getStringValue();
						}
					}
				}
				
			}	
		}
		
		// Step 1: Create the RFS Service since CFS and CFS Config are already in place.
		Service rfsService = UIMHelper.createRFSService(cfsScv);
		
		// Step 2: Create the RFS Service Configuration
		ServiceConfigurationVersion rfsScv = UIMHelper.createRFSServiceConfiguration(rfsService);
		
		// Step 3: Get the configuration Items of CFS and retrieve the Access_CFS_SCI Config Item from the list
		ServiceConfigurationItem cfsConfigItem = null;
		List<ServiceConfigurationItem> cfsConfigItemList =  cfsScv.getConfigItems();
		if(cfsConfigItemList!=null && !cfsConfigItemList.isEmpty()) {
			for(ServiceConfigurationItem configItem: cfsConfigItemList) {
				if(configItem!=null && configItem.getName()!=null && configItem.getName().equalsIgnoreCase("Access_CFS_SCI")) {
					cfsConfigItem = configItem;
					break;
				}
			}
		}
		
		//Step 4: Assigning/Referencing RFS Service with CFS Service Configuration
		ServiceConfigurationManager rfsSvcMgr = PersistenceHelper.makeServiceConfigurationManager();
		if(cfsConfigItem!=null) {
			rfsSvcMgr.assignResource(cfsConfigItem, rfsService, null, null);
		}
		
		// Step 5: Create the Parent and Child Characteristics for RFS Service Configuration and Assign/Reference the resources to RFS.
		if(rfsScv!=null) {
			
			// Step 5.1: Find the RFS Service Configuration Item
			//ServiceConfigurationItem rfsParentDeviceAccessItem = UIMHelper.findServiceConfigurationItem(rfsScv, "Device_Access");
			ServiceConfigurationItem rfsParentDeviceAccessItem = UIMHelper.createconfigItem(rfsScv, "Device_Access");
			EntityUtils.setValue(rfsParentDeviceAccessItem, "mediaType", mediaType);

			
			// Step 5.2: Find the Child Configuration Item for the above Parent Configuration Item for RFS SC
			if(rfsParentDeviceAccessItem!=null) {
				ServiceConfigurationItem rfsChildPeDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentDeviceAccessItem, "PE_Device");
				System.out.println("after rfsChildPeDeviceItem");
				// Step 5.3: Create the Child Characteristics and Devices
				if(rfsChildPeDeviceItem!=null) {
					// Step 5.3.1 Create the logical device "peDevice" as it is required in all the cases and assign the device to the RFS
					System.out.println("after null check rfsChildPeDeviceItem");
					if(peDeviceName!= null) {
						System.out.println("peDeviceName in designAdd is:"+peDeviceName);
						LogicalDevice ldPeDevice = UIMHelper.findOrCreateLogicalDevice(peDeviceName, "peDevice");
						rfsSvcMgr.assignResource(rfsChildPeDeviceItem, ldPeDevice, null, null);
						EntityUtils.setValue(rfsChildPeDeviceItem, "peDeviceName", peDeviceName);
					}
				}
				
				System.out.println("mediaType is:" +mediaType);
				if(null!= mediaType) {
					if(mediaType.equalsIgnoreCase("Fiber") || mediaType.equalsIgnoreCase("Microwave")) {
						// As per the requirement, if mediaType is Fiber then Switch device is also mandatory.
						// Step 5.3.2 Create the logical device "Switch_Device
						System.out.println("Am in Fiber or Microwave");
						//ServiceConfigurationItem rfsChildSwitchDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentDeviceAccessItem, "Switch_Device");
						ServiceConfigurationItem rfsChildSwitchDeviceItem = UIMHelper.findOrCreateChildConfigItem(rfsParentDeviceAccessItem, "Switch_Device", "Switch_Device");
						
						if(rfsChildSwitchDeviceItem!=null) {
							if(switchHostName!= null) {
								LogicalDevice ldSwitchDevice = UIMHelper.findOrCreateLogicalDevice(switchHostName, "accessDevice");
								rfsSvcMgr.assignResource(rfsChildSwitchDeviceItem, ldSwitchDevice, null, null);
								EntityUtils.setValue(rfsChildSwitchDeviceItem, "switchHostName", switchHostName);
							}
						}
					}
					if(mediaType.equalsIgnoreCase("Microwave")) {
						EntityUtils.setValue(rfsParentDeviceAccessItem, "mwType", mwType);
					}
				if(mediaType.equalsIgnoreCase("GPON")) {
						// As per the requirement, if mediaType is GPON then OLT device is also mandatory.
						// Step 5.3.3 Create the logical device "OLT_Device
						//ServiceConfigurationItem rfsChildOltDeviceItem = UIMHelper.findChildServiceConfigurationItem(rfsParentDeviceAccessItem, "OLT_Device");
						ServiceConfigurationItem rfsChildOltDeviceItem = UIMHelper.findOrCreateChildConfigItem(rfsParentDeviceAccessItem, "OLT_Device", "OLT_Device");
						
						if(rfsChildOltDeviceItem!=null) {
							if(oltDeviceName!= null) {
								LogicalDevice ldOltDevice = UIMHelper.findOrCreateLogicalDevice(oltDeviceName, "oltDevice");
								rfsSvcMgr.assignResource(rfsChildOltDeviceItem, ldOltDevice, null, null);
								EntityUtils.setValue(rfsChildOltDeviceItem, "oltDeviceName", oltDeviceName);
							}
						}
					}
				}
			}	
		}
		
	}

}
