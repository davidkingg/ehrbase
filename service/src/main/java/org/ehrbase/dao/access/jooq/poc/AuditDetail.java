package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.tables.AuditDetails.AUDIT_DETAILS;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.TerminologyId;

public class AuditDetail implements ActiveObject {

  private I_DomainAccess dataAccess;
  private AuditDetailsRecord auditDetailsRecord;

  public AuditDetail(I_DomainAccess dataAccess, String tenantIdentifier) {
    this(dataAccess, dataAccess.getContext().newRecord(AUDIT_DETAILS));
    this.auditDetailsRecord.setNamespace(tenantIdentifier);
  }

  public AuditDetail(I_DomainAccess dataAccess, AuditDetailsRecord record) {
    this.auditDetailsRecord = record;
    this.dataAccess = dataAccess;
  }

  public AuditDetail(I_DomainAccess dataAccess, UUID systemId, UUID committer,
      I_ConceptAccess.ContributionChangeType changeType, String description, String tenantIdentifier) {
    this.dataAccess = dataAccess;
    this.auditDetailsRecord = dataAccess.getContext().newRecord(AUDIT_DETAILS);
    auditDetailsRecord.setSystemId(systemId);
    auditDetailsRecord.setCommitter(committer);
    setChangeType(I_ConceptAccess.fetchContributionChangeType(dataAccess, changeType));
    auditDetailsRecord.setDescription(description);
    auditDetailsRecord.setNamespace(tenantIdentifier);
  }

  public void setId(UUID id) {
    auditDetailsRecord.setId(id);
  }
  
  public void setTimeCommitted(Timestamp transactionTime) {
    auditDetailsRecord.setTimeCommitted(transactionTime);
  }
  
  public boolean isChanged() {
    return auditDetailsRecord.changed();
  }
  
  public void setTimeCommittedTzid() {
    auditDetailsRecord.setTimeCommittedTzid(ZonedDateTime.now().getZone().getId());
  }
  
  public UUID getId() {
    return auditDetailsRecord.getId();
  }

  public void setSystemId(UUID systemId) {
    auditDetailsRecord.setSystemId(systemId);
  }

  public UUID getSystemId() {
    return auditDetailsRecord.getSystemId();
  }

  public void setCommitter(UUID committer) {
    auditDetailsRecord.setCommitter(committer);
  }

  public UUID getCommitter() {
    return auditDetailsRecord.getCommitter();
  }

  public void setChangeType(UUID changeType) {
    String changeTypeString = I_ConceptAccess.fetchConceptLiteral(dataAccess, changeType);
    auditDetailsRecord.setChangeType(ContributionChangeType.valueOf(changeTypeString));
  }

  public void setChangeType(I_ConceptAccess.ContributionChangeType changeType) {
    auditDetailsRecord.setChangeType(ContributionChangeType.valueOf(changeType.name()));
  }

  public ContributionChangeType getChangeType() {
    return auditDetailsRecord.getChangeType();
  }

  public void setDescription(String description) {
    auditDetailsRecord.setDescription(description);
  }

  public String getDescription() {
    return auditDetailsRecord.getDescription();
  }

  public Timestamp getTimeCommitted() {
    return auditDetailsRecord.getTimeCommitted();
  }

  public String getTimeCommittedTzId() {
    return auditDetailsRecord.getTimeCommittedTzid();
  }

  public void setRecord(AuditDetailsRecord record) {
    if (StringUtils.isEmpty(record.getNamespace()))
      record.setNamespace(this.auditDetailsRecord.getNamespace());
    if (!this.auditDetailsRecord.getNamespace().equals(record.getNamespace()))
      throw new InternalServerException("Tenant id missmatch");
    this.auditDetailsRecord = record;
  }

  public com.nedap.archie.rm.generic.AuditDetails getAsAuditDetails() {
    String systemId = getSystemId().toString();
    PartyProxy party = new PersistedPartyProxy(dataAccess).retrieve(getCommitter());
    DvDateTime time = new DvDateTime(
        getTimeCommitted().toInstant().atZone(ZoneId.of(auditDetailsRecord.getTimeCommittedTzid())).toOffsetDateTime());
    DvCodedText changeType = new DvCodedText(getChangeType().getLiteral(),
        new CodePhrase(new TerminologyId("openehr"), Integer.toString(
            I_ConceptAccess.ContributionChangeType.valueOf(getChangeType().getLiteral().toUpperCase()).getCode())));
    DvText description = new DvText(getDescription());
    return new com.nedap.archie.rm.generic.AuditDetails(systemId, party, time, changeType, description);
  }

  public String getNamespace() {
    return auditDetailsRecord.getNamespace();
  }

  @Override
  public Integer persist() {
    dataAccess.getContext().attach(auditDetailsRecord);
    if(auditDetailsRecord.getId() == null)
      auditDetailsRecord.setId(UUID.randomUUID());
    return auditDetailsRecord.store();
  }
  
  @Override
  public Integer persistAllways() {
    dataAccess.getContext().attach(auditDetailsRecord);
    if(auditDetailsRecord.getId() == null)
      auditDetailsRecord.setId(UUID.randomUUID());
    return auditDetailsRecord.insert();
  }  

  @Override
  public Boolean update() {
    dataAccess.getContext().attach(auditDetailsRecord);
    return auditDetailsRecord.update() == 1;
  }

  @Override
  public Integer delete() {
    dataAccess.getContext().attach(auditDetailsRecord);
    return auditDetailsRecord.delete();
  }

  @Override
  public boolean isDirty() {
    return auditDetailsRecord.changed();
  }
}
