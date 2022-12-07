package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.CONTRIBUTION;

import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.AdminApiUtils;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.enums.ContributionState;
import org.ehrbase.jooq.pg.tables.AdminDeleteStatusHistory;
import org.ehrbase.jooq.pg.tables.records.AdminDeleteStatusRecord;
import org.ehrbase.jooq.pg.tables.records.AdminGetLinkedCompositionsForContribRecord;
import org.ehrbase.jooq.pg.tables.records.AdminGetLinkedStatusForContribRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contribution implements ActiveObject {

  Logger log = LoggerFactory.getLogger(Contribution.class);

  private I_DomainAccess domainAccess;
  private ContributionRecord contributionRecord;
  private AuditDetail auditDetails;

  public AuditDetail getAuditDetails() {
    return auditDetails;
  }

  public void setAuditDetails(AuditDetail audit) {
    auditDetails = audit;
  }

  public Contribution(I_DomainAccess domainAccess, UUID ehrId, String tenantIdentifier) {
    this.domainAccess = domainAccess;
    
    contributionRecord = domainAccess.getContext().newRecord(CONTRIBUTION);
    contributionRecord.setEhrId(ehrId);
    contributionRecord.setNamespace(tenantIdentifier);

    auditDetails = new AuditDetail(domainAccess, tenantIdentifier);
  }

  public Contribution(I_DomainAccess domainAccess, ContributionRecord record, AuditDetail audit) {
    this.domainAccess = domainAccess;
    this.contributionRecord = record;
    this.auditDetails = audit;
  }

  public boolean isChanged() {
    return contributionRecord.changed();
  }
  
  public void setChange(boolean b) {
    contributionRecord.changed(b);
  }
  
  public String getState() {
    return contributionRecord.getState().getLiteral();
  }

  public void setSignature(String value) {
    contributionRecord.setSignature(value);
  }

  public UUID getContributionId() {
    return contributionRecord.getId();
  }

  public void setAuditDetailsChangeType(UUID changeType) {
    auditDetails.setChangeType(changeType);
  }

  public ContributionDataType getContributionDataType() {
    return contributionRecord.getContributionType();
  }

  public void setContributionDataType(ContributionDataType contributionDataType) {
    contributionRecord.setContributionType(contributionDataType);
  }

  public void setState(ContributionDef.ContributionState state) {
    if (state != null)
      contributionRecord.setState(ContributionState.valueOf(state.getLiteral()));
  }

  public void setComplete() {
    contributionRecord.setState(ContributionState.valueOf(ContributionState.complete.getLiteral()));
  }

  public void setIncomplete() {
    contributionRecord.setState(ContributionState.valueOf(ContributionState.incomplete.getLiteral()));
  }

  public void setDeleted() {
    contributionRecord.setState(ContributionState.valueOf(ContributionState.deleted.getLiteral()));
  }

  public void setAuditDetailsValues(UUID committer, UUID system, String description,
      ContributionChangeType changeType) {
    if (committer == null || system == null || changeType == null)
      throw new IllegalArgumentException("arguments not optional");
    auditDetails.setCommitter(committer);
    auditDetails.setSystemId(system);
    auditDetails.setChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, changeType));

    if (description != null)
      auditDetails.setDescription(description);
  }

  public void setAuditDetailsValues(com.nedap.archie.rm.generic.AuditDetails auditObject) {
    // parse
    UUID committer = new PersistedPartyProxy(domainAccess).getOrCreate(auditObject.getCommitter(), auditDetails.getNamespace());
    UUID system = I_SystemAccess.createOrRetrieveInstanceId(domainAccess, null, auditObject.getSystemId());
    UUID changeType = I_ConceptAccess.fetchContributionChangeType(domainAccess, auditObject.getChangeType().getValue());

    // set
    if (committer == null || system == null)
      throw new IllegalArgumentException("arguments not optional");
    auditDetails.setCommitter(committer);
    auditDetails.setSystemId(system);
    auditDetails.setChangeType(changeType);

    // optional description
    if (auditObject.getDescription() != null)
      auditDetails.setDescription(auditObject.getDescription().getValue());
  }

  public void setAuditDetailsCommitter(UUID committer) {
    auditDetails.setCommitter(committer);
  }

  public void setAuditDetailsSystemId(UUID system) {
    auditDetails.setSystemId(system);
  }

  public void setAuditDetailsDescription(String description) {
    auditDetails.setDescription(description);
  }

  public UUID getAuditsCommitter() {
    return auditDetails.getCommitter();
  }

  public UUID getAuditsSystemId() {
    return auditDetails.getSystemId();
  }

  public String getAuditsDescription() {
    return auditDetails.getDescription();
  }

  public ContributionChangeType getAuditsChangeType() {
    return I_ConceptAccess.ContributionChangeType.valueOf(auditDetails.getChangeType().getLiteral().toUpperCase());
  }

  public ContributionDef.ContributionType getContributionType() {
    return ContributionDef.ContributionType.valueOf(contributionRecord.getContributionType().getLiteral());
  }

  public ContributionDef.ContributionState getContributionState() {
    return ContributionDef.ContributionState.valueOf(contributionRecord.getState().getLiteral());
  }

  public UUID getEhrId() {
    return contributionRecord.getEhrId();
  }

  public String getDataType() {
    return contributionRecord.getContributionType().getLiteral();
  }

  public void setDataType(ContributionDataType contributionDataType) {
    contributionRecord.setContributionType(contributionDataType);
  }

  public UUID getId() {
    return contributionRecord.getId();
  }
  
  public void setId(UUID id) {
    contributionRecord.setId(id);
  }

  public void setEhrId(UUID ehrId) {
    contributionRecord.setEhrId(ehrId);
  }

  public void setHasAuditDetails(UUID auditId) {
    contributionRecord.setHasAudit(auditId);
  }

  public UUID getHasAuditDetails() {
    return contributionRecord.getHasAudit();
  }

  public String getNamespace() {
    return contributionRecord.getNamespace();
  }

  @Override
  public Integer persist() {
    domainAccess.getContext().attach(contributionRecord);
    if (contributionRecord.getId() == null)
      contributionRecord.setId(UUID.randomUUID());
    return contributionRecord.store();
  }

  @Override
  public Integer persistAllways() {
    domainAccess.getContext().attach(contributionRecord);
    if (contributionRecord.getId() == null)
      contributionRecord.setId(UUID.randomUUID());
    return contributionRecord.insert();
  }

  @Override
  public Boolean update() {
    domainAccess.getContext().attach(contributionRecord);
    return contributionRecord.update() == 1;
  }

  @Override
  public boolean isDirty() {
    return contributionRecord.changed();
  }
  
  public Integer delete() {
    domainAccess.getContext().attach(contributionRecord);
    return contributionRecord.delete();
  }
  
  //------------------------------------------------------------------------------------------------------------------------
  //:TODO currently not impl. should be handeld by a specific REST/Service call with high priviledges
  public void adminDelete() {
    AdminApiUtils adminApi = new AdminApiUtils(domainAccess.getContext());

    // retrieve info on all linked versioned_objects
    Result<AdminGetLinkedCompositionsForContribRecord> linkedCompositions = Routines
        .adminGetLinkedCompositionsForContrib(domainAccess.getContext().configuration(), this.getId());
    Result<AdminGetLinkedStatusForContribRecord> linkedStatus = Routines
        .adminGetLinkedStatusForContrib(domainAccess.getContext().configuration(), this.getId());

    // handling of linked composition
    linkedCompositions.forEach(compo -> adminApi.deleteComposition(compo.getComposition()));

    // handling of linked status
    linkedStatus.forEach(status -> {
      Result<AdminDeleteStatusRecord> delStatus = Routines.adminDeleteStatus(domainAccess.getContext().configuration(),
          status.getStatus());
      if (delStatus.isEmpty()) {
        throw new InternalServerException("Admin deletion of Status failed! Unexpected result.");
      }
      // handle auxiliary objects
      delStatus.forEach(id -> {
        // delete status audit
        adminApi.deleteAudit(id.getStatusAudit(), "Status", false);

        // clear history
        int res = domainAccess.getContext().selectQuery(new AdminDeleteStatusHistory().call(status.getStatus())).execute();
        if (res != 1)
          throw new InternalServerException("Admin deletion of Status failed!");
      });
    });

    // delete contribution itself
    adminApi.deleteContribution(this.getId(), null, false);
  }
}
