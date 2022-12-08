package org.ehrbase.dao.access.jooq.dom;

import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.jooq.Result;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;

public class Status implements ActiveObjectAware<StatusRecord> {

  private Contribution contributionAccess;
  private AuditDetail auditDetailsAccess; // audit associated with this status

  private I_DomainAccess domainAccess;
  private StatusRecord statusRecord;

  public Status(I_DomainAccess domainAccess, UUID ehrId, String tenantIdentifier) {
    this.domainAccess = domainAccess;
    statusRecord = domainAccess.getContext().newRecord(STATUS);
    statusRecord.setNamespace(tenantIdentifier);

    contributionAccess = new Contribution(domainAccess, ehrId, tenantIdentifier);
    contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    auditDetailsAccess = new AuditDetail(domainAccess, tenantIdentifier);
  }
  
  public Status(I_DomainAccess domainAccess, UUID ehrId, StatusRecord statusRecord) {
    this.domainAccess = domainAccess;
    this.statusRecord = statusRecord;
    
    contributionAccess = new Contribution(domainAccess, ehrId, statusRecord.getNamespace());
    contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    auditDetailsAccess = new AuditDetail(domainAccess, statusRecord.getNamespace());
  }
  
  public Status(I_DomainAccess domainAccess, UUID ehrId, StatusHistoryRecord statusHistoryRecord) {
    this.domainAccess = domainAccess;
    this.statusRecord = Mapper.from(statusHistoryRecord);
    
    contributionAccess = new Contribution(domainAccess, ehrId, statusRecord.getNamespace());
    contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    auditDetailsAccess = new AuditDetail(domainAccess, statusRecord.getNamespace());
  }
  
  public String getNamespace() {
    return statusRecord.getNamespace();
  }
  
  public Contribution getContributionAccess() {
    return contributionAccess;
  }
  
  public void setSysTransaction(Timestamp transactionTime) {
    this.statusRecord.setSysTransaction(transactionTime);
  }

  public UUID getId() {
    return statusRecord.getId();
  }

  public void setAuditDetailsAccess(AuditDetail auditDetailsAccess) {
    this.auditDetailsAccess = auditDetailsAccess;
  }

  public void setContributionAccess(Contribution contributionAccess) {
    this.contributionAccess = contributionAccess;
  }
  
  public void setHasAudit(UUID auditId) {
    this.statusRecord.setHasAudit(auditId);
  }

  public AuditDetail getAuditDetailsAccess() {
    return this.auditDetailsAccess;
  }

  public UUID getAuditDetailsId() {
    return statusRecord.getHasAudit();
  }

  public void setContributionId(UUID contribution) {
    this.statusRecord.setInContribution(contribution);
  }

  public UUID getAttestationRef() {
    return statusRecord.getAttestationRef();
  }
  
  public UUID getContributionId() {
    return this.statusRecord.getInContribution();
  }

  public void setAuditAndContributionAuditValues(UUID systemId, UUID committerId, String description,
      ContributionChangeType changeType) {
    if (committerId == null || systemId == null || changeType == null) {
      throw new IllegalArgumentException("arguments not optional");
    }
    this.auditDetailsAccess.setCommitter(committerId);
    this.auditDetailsAccess.setSystemId(systemId);
    this.auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, changeType));

    if (description != null) {
      this.auditDetailsAccess.setDescription(description);
    }

    this.contributionAccess.setAuditDetailsValues(committerId, systemId, description, changeType);
  }

  public Timestamp getInitialTimeOfVersionedEhrStatus() {
    Result<StatusHistoryRecord> result = domainAccess.getContext().selectFrom(STATUS_HISTORY)
        .where(STATUS_HISTORY.EHR_ID.eq(statusRecord.getEhrId())) // ehrId from this instance
        .orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc()) // oldest at top, i.e. [0]
        .fetch();

    if (!result.isEmpty()) {
      StatusHistoryRecord statusHistoryRecord = result.get(0); // get oldest
      return statusHistoryRecord.getSysTransaction();
    }

    return statusRecord.getSysTransaction();
  }

  public void setModifiable(Boolean modifiable) {
    this.statusRecord.setIsModifiable(modifiable);
  }
  
  public void setQueryable(Boolean queryable) {
    this.statusRecord.setIsQueryable(queryable);
  }
  
  public Boolean isModifiable() {
    return this.statusRecord.getIsModifiable();
  }
  
  public Boolean isQueryable() {
    return this.statusRecord.getIsQueryable();
  }
  
  public String getArchetypeNodeId() {
    return statusRecord.getArchetypeNodeId();
  }
  
  public void setArchetypeNodeId(String nodeId) {
    statusRecord.setArchetypeNodeId(nodeId);
  }
  
  public UUID getParty() {
    return statusRecord.getParty();
  }
  
  public void setParty(UUID partsId) {
    statusRecord.setParty(partsId);
  }
  
  public EhrStatus getStatus() {
    EhrStatus status = new EhrStatus();

    status.setModifiable(isModifiable());
    status.setQueryable(isQueryable());

    Optional.ofNullable(getOtherDetails()).ifPresent(d -> status.setOtherDetails(getOtherDetails()));

    status.setArchetypeNodeId(getArchetypeNodeId());
    Object name = new RecordedDvCodedText().fromDB(getActiveObject(), STATUS.NAME);
    status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

    UUID statusId = getId();
    status.setUid(new HierObjectId(statusId.toString() + "::" + domainAccess.getServerConfig().getNodename() + "::" + getLatestVersionNumber()));

    PartySelf partySelf = (PartySelf) new PersistedPartyProxy(domainAccess).retrieve(getParty());
    status.setSubject(partySelf);

    return status;
  }

  public void setOtherDetails(ItemStructure otherDetails) {
    if (otherDetails != null) {
      statusRecord.setOtherDetails(otherDetails);
    }
  }

  public ItemStructure getOtherDetails() {
    return this.statusRecord.getOtherDetails();
  }

  public void setEhrId(UUID ehrId) {
    this.statusRecord.setEhrId(ehrId);
  }

  public UUID getEhrId() {
    return this.statusRecord.getEhrId();
  }

  public Timestamp getSysTransaction() {
    return this.statusRecord.getSysTransaction();
  }
  
  public Integer getLatestVersionNumber() {
    UUID statusId = statusRecord.getId();
    int versionCount = domainAccess.getContext().fetchCount(STATUS_HISTORY, STATUS_HISTORY.ID.eq(statusId));
    return versionCount == 0 ? 1 : versionCount + 1;
  }

  @Override
  public StatusRecord getActiveObject() {
    return statusRecord;
  }

  @Override
  public void setId(UUID id) {
    statusRecord.setId(id);
  }
}
