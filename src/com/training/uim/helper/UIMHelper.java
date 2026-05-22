package com.training.uim.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.training.uim.DesignServiceAction;

import oracle.communications.inventory.api.common.EntityUtils;
import oracle.communications.inventory.api.configuration.BaseConfigurationManager;
import oracle.communications.inventory.api.configuration.ConfigurationManager;
import oracle.communications.inventory.api.consumer.AssignmentManager;
import oracle.communications.inventory.api.entity.ConfigurationStatus;
import oracle.communications.inventory.api.entity.DeviceInterface;
import oracle.communications.inventory.api.entity.InventoryConfigurationSpec;
import oracle.communications.inventory.api.entity.LogicalDevice;
import oracle.communications.inventory.api.entity.LogicalDeviceSpecification;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.entity.ServiceSpecification;
import oracle.communications.inventory.api.entity.Specification;
import oracle.communications.inventory.api.entity.SpecificationRel;
import oracle.communications.inventory.api.entity.common.InventoryConfigurationVersion;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.api.logicaldevice.LogicalDeviceManager;
import oracle.communications.inventory.api.logicaldevice.LogicalDeviceSearchCriteria;
import oracle.communications.inventory.api.service.ServiceConfigurationManager;
import oracle.communications.inventory.api.service.ServiceManager;
import oracle.communications.inventory.api.service.ServiceSearchCriteria;
import oracle.communications.inventory.api.specification.SpecManager;
import oracle.communications.platform.entity.impl.ServiceAssignmentDAO;
import oracle.communications.platform.persistence.CriteriaItem;
import oracle.communications.platform.persistence.CriteriaOperator;
import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;
import oracle.communications.platform.persistence.Persistent;
import oracle.communications.platform.persistence.cartridgemanagement.entity.OperationType;
import oracle.communications.platform.util.Utils;

public class UIMHelper {
	
	private static final Log log = LogFactory.getLog(UIMHelper.class);
	
	// Create RFS Service 
	public static Service createRFSService(ServiceConfigurationVersion cfsSvc) throws ValidationException {
		
		ServiceManager srcMgr = PersistenceHelper.makeServiceManager();
		Service serviceRfs = srcMgr.makeService(Service.class);
		
		ServiceSpecification srcSpec = (ServiceSpecification)findSpec("Access_RFS");
		serviceRfs.setName(cfsSvc.getService().getName()+"_RFS");
		//serviceRfs.setName("Sample_RFS");
		serviceRfs.setSpecification(srcSpec);
		
		Collection<Service> servCollection = new ArrayList<Service>();
		servCollection.add(serviceRfs);
		
		List<Service> servList = srcMgr.createService(servCollection);
		if(servList!=null && !servList.isEmpty())
			return servList.get(0);		
		else
			return null;
	}
	
	// Common method to find the specification based on the Specification Name
	
	public static Specification findSpec(String specName) {
		Finder finder = PersistenceHelper.makeFinder();
		Collection<Specification> specList = finder.findByName(Specification.class, specName);
		if(specList!=null && !specList.isEmpty()) {
			return specList.iterator().next();
		}
		return null;	
	}
	
	public static ServiceConfigurationVersion createRFSServiceConfiguration(Service rfsService) throws ValidationException {
		ServiceConfigurationManager scMgr = PersistenceHelper.makeServiceConfigurationManager();
		ServiceConfigurationVersion scv = scMgr.makeConfigurationVersion(rfsService);
		
		InventoryConfigurationSpec invSpec = (InventoryConfigurationSpec) findSpec("Access_RFS_Configuration");
		scv.setName(invSpec.getName()+"_RFS");
		System.out.println("Inv Spec is:" +scv.getName());
		scv.setEffDate(new Date());
		
		InventoryConfigurationVersion invConfigVersion = scMgr.createConfigurationVersion(rfsService, scv, invSpec);
		return (ServiceConfigurationVersion) invConfigVersion;
		
	}
	
	public static ServiceConfigurationVersion createNewServiceConfiguration(Service rfsService) throws ValidationException {
		ServiceConfigurationManager scMgr = PersistenceHelper.makeServiceConfigurationManager();
		ServiceConfigurationVersion scv = scMgr.makeConfigurationVersion(rfsService);
		scv.setEffDate(new Date());
		System.out.println("createNewServiceConfiguration: "+scv);
		InventoryConfigurationVersion invConfigVersion = scMgr.createConfigurationVersion(rfsService, scv);
		return (ServiceConfigurationVersion) invConfigVersion;		
	}
	
	public static ServiceConfigurationVersion createServiceConfiguration(Service service, String configSpecName)
			throws ValidationException {
		ServiceConfigurationVersion serviceVersion = null;
		try {
			BaseConfigurationManager configurationManager = PersistenceHelper.makeConfigurationManager(ServiceConfigurationVersion.class);
			InventoryConfigurationVersion configuration = configurationManager.makeConfigurationVersion(service);

			Specification configSpec = null;
			if (!Utils.checkNull(configSpecName)) {
				configSpec = PersistenceHelper.makeSpecManager().findSpecification(InventoryConfigurationSpec.class,
						configSpecName);
			} else {
				ConfigurationManager cm = PersistenceHelper.makeConfigurationManager();
				List<InventoryConfigurationSpec> specs = cm.getConfigSpecTypeConfig(service.getSpecification(), true);
				configSpec = specs.get(0);
			}

			if (service.getConfigurations().size() == 0) {
				configuration.setVersionNumber(0);
			} else {
				configuration.setVersionNumber(service.getConfigurations().size() + 1);
			}

			if (configSpec instanceof InventoryConfigurationSpec) { // No need to check for Null before instanceof.
				configuration.setConfigSpec((InventoryConfigurationSpec) configSpec);
			}
			configuration.setEffDate(new Date());

			if (service.getConfigurations() != null && service.getConfigurations().size() > 0) {
				serviceVersion = (ServiceConfigurationVersion) configurationManager.createConfigurationVersion(service,
						configuration);
			} else {
				serviceVersion = (ServiceConfigurationVersion) configurationManager.createConfigurationVersion(service,
						configuration, (InventoryConfigurationSpec) configSpec);
			}
		} catch (Exception ex) {
			log.debug("","Exception while creating Service configuration.");
			ex.printStackTrace();
		}
		log.info("createServiceConfiguration - END");
		return serviceVersion;
	}
	
	
	
	
	public static ServiceConfigurationItem findServiceConfigurationItem(ServiceConfigurationVersion scv, String scItemName) {
		
		if(scv!=null && scv.getConfigItems()!=null) {
			System.out.println("crated");
			for(ServiceConfigurationItem scvConfigItem: scv.getConfigItems()) {
				if(scvConfigItem!=null && scvConfigItem.getName()!=null && scvConfigItem.getName().equalsIgnoreCase(scItemName)) {
					return scvConfigItem;
				}
			}
		}
		return null;
		
	}

	public static ServiceConfigurationItem findChildServiceConfigurationItem(ServiceConfigurationItem rfsParentDeviceAccessItem, String childScItemName) {
		System.out.println("rfsParentDeviceAccessItem is:" +rfsParentDeviceAccessItem.getChildConfigItems());
		if(rfsParentDeviceAccessItem!=null && rfsParentDeviceAccessItem.getChildConfigItems()!=null) {
			for(ServiceConfigurationItem childConfigItem: rfsParentDeviceAccessItem.getChildConfigItems()) {
				System.out.println("childConfigItem.getName() is:" +childConfigItem.getName());
				if(childConfigItem!=null && childConfigItem.getName()!=null && childConfigItem.getName().equalsIgnoreCase(childScItemName)) {
					return childConfigItem;
				}
			}
		}
		return null;
	}
	
	
	public static LogicalDevice findOrCreateLogicalDevice(String peDeviceName, String specName) throws ValidationException {
		System.out.println("specName in findOrCreateLogicalDevice is:" +specName);
		// check if the logical device with the same peDeviceName exists, if not, then create
		LogicalDeviceManager ldMgr = PersistenceHelper.makeLogicalDeviceManager();
		LogicalDeviceSearchCriteria ldSearchCriteria = ldMgr.makeLogicalDeviceSearchCriteria();
		
		CriteriaItem criteriaItem = ldSearchCriteria.makeCriteriaItem();
		criteriaItem.setName(peDeviceName);
		criteriaItem.setValue(peDeviceName);
		criteriaItem.setOperator(CriteriaOperator.EQUALS_IGNORE_CASE);
		LogicalDeviceSpecification ldSpec = (LogicalDeviceSpecification)findSpec(specName);
		ldSearchCriteria.setName(criteriaItem);
		ldSearchCriteria.setLogicalDeviceSpecification(ldSpec);
		List<LogicalDevice> ldList = ldMgr.findLogicalDevice(ldSearchCriteria);
		if(!ldList.isEmpty()) {
			return ldList.get(0);
		}else {
			// Logical Device with the given parameters doesnt exist so create new
			LogicalDevice ldToCreate = ldMgr.makeLogicalDevice();
			ldToCreate.setName(peDeviceName);
			ldToCreate.setSpecification(ldSpec);
			Set<DeviceInterface> devInfSet = new HashSet<>();
			DeviceInterface devInf = ldMgr.makeDeviceInterface();
			devInf.setId(peDeviceName);
			devInf.setName("DI_"+peDeviceName);
			devInfSet.add(devInf);
			ldToCreate.setAllDeviceInterfaces(devInfSet);
			
			List<LogicalDevice> ldListToCreate = new ArrayList<>();
			ldListToCreate.add(ldToCreate);
			List<LogicalDevice> createdLdList = ldMgr.createLogicalDevice(ldListToCreate);
			if(!createdLdList.isEmpty()) {
				return createdLdList.get(0);
			}
		}
		
		return null;
	}
	
	public static boolean findLogicalDeviceBySpec(String peDeviceName, String specName) throws ValidationException {
		System.out.println("specName in findOrCreateLogicalDevice is:" +specName);
		// check if the logical device with the same peDeviceName exists, if not, then create
		LogicalDeviceManager ldMgr = PersistenceHelper.makeLogicalDeviceManager();
		LogicalDeviceSearchCriteria ldSearchCriteria = ldMgr.makeLogicalDeviceSearchCriteria();
		
		CriteriaItem criteriaItem = ldSearchCriteria.makeCriteriaItem();
		criteriaItem.setName(peDeviceName);
		criteriaItem.setValue(peDeviceName);
		criteriaItem.setOperator(CriteriaOperator.EQUALS_IGNORE_CASE);
		LogicalDeviceSpecification ldSpec = (LogicalDeviceSpecification)findSpec(specName);
		ldSearchCriteria.setName(criteriaItem);
		ldSearchCriteria.setLogicalDeviceSpecification(ldSpec);
		List<LogicalDevice> ldList = ldMgr.findLogicalDevice(ldSearchCriteria);
		if(!ldList.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	// Get the config Item Specification based on the name
	public static InventoryConfigurationSpec getConfigItemSpecByName(ServiceConfigurationItem parentConfigItem, String configItemName) throws InstantiationException, IllegalAccessException {
		InventoryConfigurationSpec sChild = null;
		SpecManager spMgr = PersistenceHelper.makeSpecManager();
		List<SpecificationRel> specRelList = null;
		specRelList = spMgr.getSpecificationRels(parentConfigItem.getConfigSpec(), null, true, 0);
		if(specRelList!=null) {
			for(SpecificationRel specRel : specRelList) {
				Specification childSpec = specRel.getChild();
				if(childSpec!=null && childSpec.getName()!=null && childSpec.getName().equalsIgnoreCase(configItemName)) {
					sChild = (InventoryConfigurationSpec) childSpec;
				}
			}
		}
		
		return sChild;
	}
 
	 public static ServiceConfigurationItem createconfigItem(ServiceConfigurationVersion scv,String configItemName) throws Exception {
		 ServiceConfigurationItem parentItem=(ServiceConfigurationItem) scv.getConfigItemTypeConfig();
		 ServiceConfigurationManager scvMgr=PersistenceHelper.makeServiceConfigurationManager();
		 InventoryConfigurationSpec scvItemspec=(InventoryConfigurationSpec) findSpec(configItemName);
		 Collection<?> ItemList= scvMgr.createConfigurationItems(parentItem, scvItemspec, 1);
		 if(!ItemList.isEmpty()) {
			ServiceConfigurationItem newItem=(ServiceConfigurationItem) ItemList.iterator().next();
			newItem.setName(configItemName);
			newItem.setLabel(configItemName);
			return newItem;
		 }
		 return  null;
	  }
	 
	 // Method to find or create the child config items where the item is optional
	 public static ServiceConfigurationItem findOrCreateChildConfigItem(ServiceConfigurationItem parentConfigItem,
				String childConfigItemName, String label) {
			ServiceConfigurationItem newItem = null;
			try {
				List<ServiceConfigurationItem> childConfigItemsList = parentConfigItem.getChildConfigItems();
				for (ServiceConfigurationItem sci : childConfigItemsList) {
					if (sci.getName().equals(childConfigItemName) && sci.getLabel().equalsIgnoreCase(label)) {
						return sci;
					}
				}
				ServiceConfigurationManager designer = PersistenceHelper.makeServiceConfigurationManager();
				InventoryConfigurationSpec configSpec = getConfigItemSpecByName(parentConfigItem, childConfigItemName);
				Collection<?> childConfigItems = designer.createConfigurationItems(parentConfigItem, configSpec, 1);
	 
				if (childConfigItems != null && childConfigItems.size() > 0) {
					newItem = (ServiceConfigurationItem) childConfigItems.iterator().next();
				}
			} catch (Exception ex) {
				log.debug("Error:" + ex.getLocalizedMessage());
			}
			return newItem;
		}
	 
	 // Find the service based on service Id or service Name or External Object Id
	 public static Service findService(String serviceVal, String criteriaField) throws ValidationException {
		 ServiceManager serviceMgr = PersistenceHelper.makeServiceManager();
		 ServiceSearchCriteria serviceSearch = serviceMgr.makeServiceSearchCriteria();
		 CriteriaItem criteria = serviceSearch.makeCriteriaItem();
		 criteria.setOperator(CriteriaOperator.EQUALS);
		 criteria.setValue(serviceVal);
		 if(criteriaField.equalsIgnoreCase("Id"))
			 serviceSearch.setId(criteria);
		 else if (criteriaField.equalsIgnoreCase("Name"))
			 serviceSearch.setName(criteria);
		 else if (criteriaField.equalsIgnoreCase("externalIdentityObject"))
			 serviceSearch.setExternalObjectId(criteria);
		 List<Service> results = serviceMgr.findServices(serviceSearch);
		 return results.size() > 0 ? results.get(0) : null;
	 }
	 
	 public static Service findRFSService(ServiceConfigurationVersion cfsScv) {		 
		 
		 Service rfsService = null;
		 System.out.println("Am in findRFSService ");
		 List<ServiceConfigurationItem> cfsConfigItemList = new ArrayList<>();
		 cfsConfigItemList = cfsScv.getConfigItems();
		 if(cfsConfigItemList!=null && !cfsConfigItemList.isEmpty()) {
			 System.out.println("Inside if");
			 for(ServiceConfigurationItem cfsConfigItem: cfsConfigItemList) {
				 System.out.println("Inside for");
				 Persistent persistent = cfsConfigItem.getAssignment();
				 System.out.println("persistent:"+persistent);
				 if(persistent!=null)
					 System.out.println("Class is: "+persistent.getClass());
				 
				 if (persistent instanceof ServiceAssignmentDAO) {
					 ServiceAssignmentDAO saDao = (ServiceAssignmentDAO)persistent;
					 System.out.println("saDao.getService():" +saDao.getService());
					 return saDao.getService();
				 }
				 
			 }
			
		 }
		 
		 
		 return null;
	 }
	 
	 public static ServiceConfigurationVersion getLatestConfigurationVersion(Service rfsService) {
		 ServiceConfigurationVersion scv = null;
		 
		 if(rfsService!=null) {
			 List<ServiceConfigurationVersion> configList= rfsService.getConfigurations();
			 for(int i=configList.size()-1 ; i>=0; i-- ) {
				 scv = configList.get(i);
				 if(scv.getConfigState().equals(ConfigurationStatus.CANCELLED) || scv.getConfigState().equals(ConfigurationStatus.PENDING_CANCEL)) {
					 continue;
				 }else {
					 return scv;
				 }
			 }
		 }
		 return scv;
	 }
	 
	 public static LogicalDevice findExistingLogicalDevice(String peDeviceName, String specName) throws ValidationException {
			System.out.println("specName in findOrCreateLogicalDevice is:" +specName);
			// check if the logical device with the same peDeviceName exists, if not, then create
			LogicalDeviceManager ldMgr = PersistenceHelper.makeLogicalDeviceManager();
			LogicalDeviceSearchCriteria ldSearchCriteria = ldMgr.makeLogicalDeviceSearchCriteria();
			
			CriteriaItem criteriaItem = ldSearchCriteria.makeCriteriaItem();
			criteriaItem.setName(peDeviceName);
			criteriaItem.setValue(peDeviceName);
			criteriaItem.setOperator(CriteriaOperator.EQUALS_IGNORE_CASE);
			LogicalDeviceSpecification ldSpec = (LogicalDeviceSpecification)findSpec(specName);
			ldSearchCriteria.setName(criteriaItem);
			ldSearchCriteria.setLogicalDeviceSpecification(ldSpec);
			List<LogicalDevice> ldList = ldMgr.findLogicalDevice(ldSearchCriteria);
			if(!ldList.isEmpty()) {
				return ldList.get(0);
			}
			return null;
		}
	 

}
