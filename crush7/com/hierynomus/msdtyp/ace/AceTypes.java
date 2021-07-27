package com.hierynomus.msdtyp.ace;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.SID;
import com.hierynomus.protocol.commons.EnumWithValue;
import java.util.Set;
import java.util.UUID;

public class AceTypes {
  public static ACE accessAllowedAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid) {
    return new AceType1(new AceHeader(AceType.ACCESS_ALLOWED_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid);
  }
  
  public static ACE accessAllowedObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid) {
    return new AceType2(new AceHeader(AceType.ACCESS_ALLOWED_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid);
  }
  
  public static ACE accessDeniedAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid) {
    return new AceType1(new AceHeader(AceType.ACCESS_DENIED_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid);
  }
  
  public static ACE accessDeniedObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid) {
    return new AceType2(new AceHeader(AceType.ACCESS_DENIED_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid);
  }
  
  public static ACE accessAllowedCallbackAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid, byte[] applicationData) {
    return new AceType3(new AceHeader(AceType.ACCESS_ALLOWED_CALLBACK_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid, applicationData);
  }
  
  public static ACE accessDeniedCallbackAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid, byte[] applicationData) {
    return new AceType3(new AceHeader(AceType.ACCESS_DENIED_CALLBACK_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid, applicationData);
  }
  
  public static ACE accessAllowedCallbackObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid, byte[] applicationData) {
    return new AceType4(new AceHeader(AceType.ACCESS_ALLOWED_CALLBACK_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid, applicationData);
  }
  
  public static ACE accessDeniedCallbackObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid, byte[] applicationData) {
    return new AceType4(new AceHeader(AceType.ACCESS_DENIED_CALLBACK_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid, applicationData);
  }
  
  public static ACE systemAuditAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid) {
    return new AceType1(new AceHeader(AceType.SYSTEM_AUDIT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid);
  }
  
  public static ACE systemAuditObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid, byte[] applicationData) {
    return new AceType4(new AceHeader(AceType.SYSTEM_AUDIT_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid, applicationData);
  }
  
  public static ACE systemAuditCallbackAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid, byte[] applicationData) {
    return new AceType3(new AceHeader(AceType.SYSTEM_AUDIT_CALLBACK_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid, applicationData);
  }
  
  public static ACE systemMandatoryLabelAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, SID sid) {
    return new AceType1(new AceHeader(AceType.SYSTEM_MANDATORY_LABEL_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), sid);
  }
  
  public static ACE systemAuditCallbackObjectAce(Set<AceFlags> aceFlags, Set<AccessMask> accessMask, UUID objectType, UUID inheritedObjectType, SID sid, byte[] applicationData) {
    return new AceType4(new AceHeader(AceType.SYSTEM_AUDIT_CALLBACK_OBJECT_ACE_TYPE, aceFlags), EnumWithValue.EnumUtils.toLong(accessMask), objectType, inheritedObjectType, sid, applicationData);
  }
  
  public static ACE systemResourceAttributeAce(Set<AceFlags> aceFlags, byte[] attributeData) {
    return new AceType3(new AceHeader(AceType.SYSTEM_RESOURCE_ATTRIBUTE_ACE_TYPE, aceFlags), 0L, SID.EVERYONE, attributeData);
  }
  
  public static ACE systemScopedPolicyIdAce(Set<AceFlags> aceFlags, SID sid) {
    return new AceType1(new AceHeader(AceType.SYSTEM_SCOPED_POLICY_ID_ACE_TYPE, aceFlags), 0L, sid);
  }
}
