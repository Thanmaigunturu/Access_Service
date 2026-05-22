package com.training.uim;

import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.apache.xmlbeans.impl.values.XmlStringImpl;

import oracle.communications.inventory.api.entity.BusinessInteraction;
import oracle.communications.inventory.api.entity.BusinessInteractionAttachment;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.extensibility.extension.util.ExtensionPointContext;
import oracle.communications.inventory.xmlbeans.BusinessInteractionItemType;
import oracle.communications.inventory.xmlbeans.InteractionDocument;
import oracle.communications.inventory.xmlbeans.ParameterType;
import oracle.communications.platform.exception.ValidationException;

public class ValidateAccessCaptureRequest {
	
	@SuppressWarnings("unlikely-arg-type")
	public void validateAccessCapture(ExtensionPointContext context, Log log) throws ValidationException, XmlException, Exception {
		
		log.info("validateAccessCapture::","Valdate Access Captute Started");
		BusinessInteraction bi = (BusinessInteraction) context.getArguments()[0];
		BusinessInteractionAttachment biAttachment = (BusinessInteractionAttachment) context.getArguments()[1];
		
		if(bi == null) {
			throw new ValidationException("Business Interaction is Empty and needs to be created first");
		}
		
		String requestXml = biAttachment.convertContentToString();
		InteractionDocument interDoc = (InteractionDocument) InteractionDocument.Factory.parse(requestXml);
		List<BusinessInteractionItemType> biItemTypeList = interDoc.getInteraction().getBody().getItemList();
		String mediaType =null, peDeviceName = null, switchHostName = null, oltDeviceName = null, mwType = null;
		log.info("validateAccessCapture::","biItemTypeList starts");
		if(biItemTypeList!=null && !biItemTypeList.isEmpty()) {
			for(BusinessInteractionItemType biItemType : biItemTypeList) {
				if(biItemType.getParameterList()!=null) {
					log.info("validateAccessCapture::","biItemType.getParameterList starts");
					List<ParameterType> paramTypeList = biItemType.getParameterList();
					log.info("paramTypeList:", paramTypeList);
					System.out.println("paramTypeList");
					if(!paramTypeList.isEmpty()) {
						for(ParameterType paramType: paramTypeList) {
							if(paramType!=null && paramType.getName()!=null) {
								System.out.println("paramType.getName():"+paramType.getName());
								if(paramType.getName().equals("mediaType")) {
									mediaType = ((XmlObjectBase) paramType.getValue()).getStringValue();
									System.out.println("mediaType:"+mediaType);
									if(mediaType.isEmpty()) {
										throw new ValidationException("Media Type is mandatory");
									}
								} else if(paramType.getName().equals("peDeviceName")) {
									peDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
								}else if(paramType.getName().equals("switchHostName")) {
									switchHostName = ((XmlObjectBase) paramType.getValue()).getStringValue();
								}else if(paramType.getName().equals("oltDeviceName")) {
									oltDeviceName = ((XmlObjectBase) paramType.getValue()).getStringValue();
								}else if (paramType.getName().equals("mwType")){
									mwType = ((XmlObjectBase) paramType.getValue()).getStringValue();
								}
								/**else if(paramType.equals("mwType")) {
									mwType = paramType.getValue().toString();
								} **/
							}
						}
						
						if(mediaType.equalsIgnoreCase("Fiber") || mediaType.equalsIgnoreCase("Microwave")) {
							if(peDeviceName==null || peDeviceName.isEmpty() || switchHostName==null || switchHostName.isEmpty()) {
								throw new ValidationException("peDeviceName and switchHostName are mandatory when Media type is Fiber or Microwave");
							}
							if(mediaType.equalsIgnoreCase("Microwave")) {
								if(null == mwType || mwType.isEmpty()) {
									throw new ValidationException("mwType is mandatory when Media type is Microwave");
								}
							}
						} else if(mediaType.equalsIgnoreCase("GPON")) {
							if(peDeviceName==null || peDeviceName.isEmpty() || oltDeviceName==null || oltDeviceName.isEmpty()) {
								throw new ValidationException("peDeviceName and oltDeviceName are mandatory when Media type is GPON");
							}
						}
					}
				}
				
			}
		}
		
		
		
		log.info("validateAccessCapture::","Valdate Access Captute End");
		
	}

}
