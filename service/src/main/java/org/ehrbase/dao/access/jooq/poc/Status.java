package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import java.sql.Timestamp;
import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
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

public class Status implements ActiveObject {

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

  public StatusRecord getStatusRecord() {
    return this.statusRecord;
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

  public int getEhrStatusVersionFromTimeStamp(Timestamp time) {
    UUID statusUid = this.statusRecord.getId();
    // retrieve current version from status tables
    I_StatusAccess retStatusAccess = I_StatusAccess.retrieveInstance(domainAccess.getDataAccess(), statusUid);

    // retrieve all other versions from status_history and sort by time
    Result result = domainAccess.getContext().selectFrom(STATUS_HISTORY).where(STATUS_HISTORY.ID.eq(statusUid))
        .orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()) // latest at top, i.e. [0]
        .fetch();

    // see 'what version was the top version at moment T?'
    // first: is time T after current version? then current version is result
    if (time.after(retStatusAccess.getStatusRecord().getSysTransaction())) {
      return getLatestVersionNumber(domainAccess, statusUid);
    }
    // second: if not, which one of the historical versions matches?
    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) instanceof StatusHistoryRecord) {
        // is time T after this version? then return its version number
        if (time.after(((StatusHistoryRecord) result.get(i)).getSysTransaction())) {
          return result.size() - i; // reverses iterator because order was reversed above and always get non zero
        }
      } else {
        throw new InternalServerException("Problem comparing timestamps of EHR_STATUS versions");
      }
    }

    throw new ObjectNotFoundException("EHR_STATUS", "Could not find EHR_STATUS version matching given timestamp");
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

    // if haven't returned above use time from latest version (already available in
    // this instance)
    return statusRecord.getSysTransaction();
  }

  public EhrStatus getStatus() {
    EhrStatus status = new EhrStatus();

    status.setModifiable(getStatusRecord().getIsModifiable());
    status.setQueryable(getStatusRecord().getIsQueryable());
    // set otherDetails if available
    if (getStatusRecord().getOtherDetails() != null) {
      status.setOtherDetails(getStatusRecord().getOtherDetails());
    }

    // Locatable attribute
    status.setArchetypeNodeId(getStatusRecord().getArchetypeNodeId());
    Object name = new RecordedDvCodedText().fromDB(getStatusRecord(), STATUS.NAME);
    status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

    UUID statusId = getStatusRecord().getId();
    status.setUid(new HierObjectId(statusId.toString() + "::" + domainAccess.getServerConfig().getNodename() + "::"
        + I_StatusAccess.getLatestVersionNumber(domainAccess, statusId)));

    PartySelf partySelf = (PartySelf) new PersistedPartyProxy(domainAccess).retrieve(getStatusRecord().getParty());
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

  public static Integer getLatestVersionNumber(I_DomainAccess domainAccess, UUID statusId) {

    if (!hasPreviousVersionOfStatus(domainAccess, statusId)) {
      return 1;
    }

    int versionCount = domainAccess.getContext().fetchCount(STATUS_HISTORY, STATUS_HISTORY.ID.eq(statusId));

    return versionCount + 1;
  }

  private static boolean hasPreviousVersionOfStatus(I_DomainAccess domainAccess, UUID ehrStatusId) {
    return domainAccess.getContext().fetchExists(STATUS_HISTORY, STATUS_HISTORY.ID.eq(ehrStatusId));
  }

  @Override
  public Integer persist() {
    domainAccess.getContext().attach(statusRecord);
    if(statusRecord.getId() == null)
      statusRecord.setId(UUID.randomUUID());
    return statusRecord.store();
  }

  @Override
  public Boolean update() {
    domainAccess.getContext().attach(statusRecord);
    return statusRecord.update() == 1;
  }

  @Override
  public Integer delete() {
    domainAccess.getContext().attach(statusRecord);
    return statusRecord.delete();
  }

  @Override
  public boolean isDirty() {
    return statusRecord.changed();
  }
}
