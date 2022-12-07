package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.tables.AuditDetails.AUDIT_DETAILS;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.TenantSupport;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.springframework.stereotype.Service;

@Service
public class AuditDetailDomainService {

  private final I_DomainAccess dataAccess;
  
  public AuditDetailDomainService(I_DomainAccess dataAccess) {
    this.dataAccess = dataAccess;
  }

  public AuditDetail retrieveInstance(UUID auditId) {
    try {
      AuditDetailsRecord auditDetailsRecord = dataAccess.getContext().fetchOne(AUDIT_DETAILS, AUDIT_DETAILS.ID.eq(auditId));
      if(auditDetailsRecord == null)
        return null;
      
      if(!TenantSupport.currentTenantIdentifier().equals(auditDetailsRecord.getNamespace()))
        throw new InternalServerException("Tenant id missmatch: Calling for id");
      
      return new AuditDetail(dataAccess, auditDetailsRecord);
    } catch (Exception e) {
      throw new InternalServerException("fetching audit_details failed", e);
    }
  }
  
  public UUID commit(AuditDetail audit) {
    return commit(audit, TransactionTime.millis());
  }
  
  public UUID commit(AuditDetail audit, Timestamp transactionTime) {
    audit.setTimeCommitted(transactionTime);
    audit.setTimeCommittedTzid();

    int result = audit.persistAllways();
    if (result == 1)
      return audit.getId();
    else
      throw new InternalServerException("Couldn't store auditDetails, DB problem");
  }

  public UUID commit(AuditDetail audit, UUID systemId, UUID committerId, String description) {
    if (systemId == null || committerId == null)
      throw new IllegalArgumentException("arguments not optional");

    audit.setSystemId(systemId);
    audit.setCommitter(committerId);
    Optional.ofNullable(description).ifPresent(d -> audit.setDescription(description));
    audit.setChangeType(I_ConceptAccess.ContributionChangeType.CREATION);
    audit.persist();
    return audit.getId();
  }

  public Boolean update(AuditDetail audit, Timestamp transactionTime, boolean force) {
    boolean result = false;

    if(force || audit.isChanged()) {
      audit.setId(UUID.randomUUID());
      result = audit.persistAllways() == 1;
    }

    return result;
  }

  public Boolean update(AuditDetail audit, Timestamp transactionTime) {
    return update(audit, TransactionTime.millis(), false);
  }

  public Boolean update(AuditDetail audit, Boolean force) {
    return update(audit, TransactionTime.millis(), force);
  }

  public Boolean update(AuditDetail audit) {
    return update(audit, false);
  }

  public Boolean update(AuditDetail audit, UUID systemId, UUID committer, I_ConceptAccess.ContributionChangeType changeType, String description) {
    Optional.ofNullable(systemId).ifPresent(o -> audit.setSystemId(o));
    Optional.ofNullable(committer).ifPresent(o -> audit.setCommitter(o));
    Optional.ofNullable(changeType).ifPresent(o -> {
      //:TODO
      audit.setChangeType(I_ConceptAccess.fetchContributionChangeType(dataAccess, changeType));

    });
    
    Optional.ofNullable(committer).ifPresent(o -> audit.setCommitter(o));
    Optional.ofNullable(description).ifPresent(o -> audit.setDescription(description));
    return audit.update();
  }

  public Integer delete(AuditDetail audit) {
    return audit.delete();
  }
}
