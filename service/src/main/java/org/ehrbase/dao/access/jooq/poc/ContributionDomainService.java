package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.CONTRIBUTION;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.enums.ContributionState;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContributionDomainService {
  Logger log = LoggerFactory.getLogger(ContributionDomainService.class);

  private final I_DomainAccess domainAccess;
  private final AuditDetailDomainService auditDetailsService;
  private final Persister<ContributionRecord, Contribution> persister;

  public ContributionDomainService(I_DomainAccess domainAccess, AuditDetailDomainService auditDetailsService) {
    this.domainAccess = domainAccess;
    this.auditDetailsService = auditDetailsService;
    this.persister = Persister.persister(domainAccess);
  }

  public Contribution retrieveInstance(UUID contributionId) {
    try {
      return Optional.ofNullable(domainAccess.getContext().fetchOne(CONTRIBUTION, CONTRIBUTION.ID.eq(contributionId)))
          .map(
              rec -> new Contribution(domainAccess, rec, auditDetailsService.retrieveInstance(rec.getHasAudit())))
          .orElse(null);
    } catch (Exception e) {
      throw new InternalServerException("fetching contribution failed", e);
    }
  }

  public UUID commit(Contribution contribution, Timestamp transactionTime) {
    auditDetailsService.commit(contribution.getAuditDetails());
    AuditDetail auditDetails = contribution.getAuditDetails();
    contribution.setHasAuditDetails(auditDetails.getId());

    if (contribution.getState().equals(ContributionState.incomplete.getLiteral()))
      log.warn("Contribution state has not been set");

    // :TODO maybe set the ehtId
//    contribution.setEhrId(this.getEhrId());

    if(persister.persistAllways(contribution) == 0)
      throw new InternalServerException("Couldn't store contribution");

    return contribution.getId();
  }

  public UUID commit(Contribution contribution) {
    return commit(contribution, TransactionTime.millis());
  }

  public UUID commit(Contribution contribution, Timestamp transactionTime, ContributionDataType contributionType,
      ContributionDef.ContributionState state) {
    contribution.setContributionDataType(Objects.requireNonNullElse(contributionType, ContributionDataType.other));
    contribution.setState(Objects.requireNonNullElse(state, ContributionDef.ContributionState.COMPLETE));

    return commit(contribution, Optional.ofNullable(transactionTime).orElseGet(() -> TransactionTime.millis()));
  }

  public UUID commit(Contribution contribution, Timestamp transactionTime, UUID committerId, UUID systemId,
      ContributionDataType contributionType, ContributionDef.ContributionState state,
      I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
    AuditDetail audit = new AuditDetail(domainAccess, contribution.getNamespace());

    contribution.setContributionDataType(Objects.requireNonNullElse(contributionType, ContributionDataType.other));
    contribution.setState(Objects.requireNonNullElse(state, ContributionDef.ContributionState.COMPLETE));
    audit.setCommitter(Optional.ofNullable(committerId)
        .orElseThrow(() -> new InternalServerException("Missing mandatory committer ID")));
    audit.setSystemId(
        Optional.ofNullable(systemId).orElseThrow(() -> new InternalServerException("Missing mandatory system ID")));

    // :TODO
    if (contributionChangeType != null)
      audit.setChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, contributionChangeType.name()));
    else
      audit.setChangeType(
          I_ConceptAccess.fetchContributionChangeType(domainAccess, I_ConceptAccess.ContributionChangeType.CREATION));

    Optional.ofNullable(description).ifPresent(d -> audit.setDescription(d));
    contribution.setAuditDetails(audit);
    return commit(contribution, Optional.ofNullable(transactionTime).orElseGet(() -> TransactionTime.millis()));
  }

  public UUID commitWithSignature(Contribution contribution, String signature) {
    contribution.setSignature(signature);
    contribution.setState(ContributionDef.ContributionState.COMPLETE);
    persister.persist(contribution);
    return contribution.getId();
  }

  public Boolean update(Contribution contribution, Timestamp transactionTime, UUID committerId, UUID systemId,
      String contributionType, String contributionState, String contributionChangeType, String description) {
    return update(contribution, transactionTime, committerId, systemId,
        Optional.ofNullable(contributionType).map(c -> ContributionDataType.valueOf(contributionType)).orElse(null),
        Optional.ofNullable(contributionState).map(c -> ContributionDef.ContributionState.valueOf(contributionState)).orElse(null),
        Optional.ofNullable(contributionChangeType).map(c -> I_ConceptAccess.ContributionChangeType.valueOf(contributionChangeType)).orElse(null),
        description);
  }

  public Boolean update(Contribution contribution, Timestamp transactionTime, UUID committerId, UUID systemId,
      ContributionDataType contributionType, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {

    Optional.ofNullable(contributionType).ifPresent(c -> contribution.setContributionDataType(c));
    Optional.ofNullable(state).ifPresent(c -> contribution.setState(c));

    AuditDetail auditDetails = new AuditDetail(domainAccess, contribution.getNamespace());
    Optional.ofNullable(committerId).ifPresent(c -> auditDetails.setCommitter(c));
    Optional.ofNullable(systemId).ifPresent(c -> auditDetails.setSystemId(systemId));
    Optional.ofNullable(description).ifPresent(c -> auditDetails.setDescription(c));
    Optional.ofNullable(contributionChangeType).ifPresent(c -> auditDetails.setChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, c)));

    contribution.setAuditDetails(auditDetails);
    return update(contribution, transactionTime);
  }

  public UUID updateWithSignature(Contribution contribution, String signature) {
    contribution.setSignature(signature);
    contribution.setState(ContributionDef.ContributionState.COMPLETE);
    persister.update(contribution);
    return contribution.getId();
  }

  public Boolean update(Contribution contribution, Timestamp transactionTime) {
    return update(contribution, transactionTime, false);
  }

  public Boolean update(Contribution contribution, Timestamp transactionTime, boolean force) {
    if(force || contribution.isChanged()) {
      if(!contribution.isChanged())
        contribution.setChange(true);

      contribution.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, I_ConceptAccess.ContributionChangeType.MODIFICATION));

      AuditDetail auditDetails = contribution.getAuditDetails();
      
      if(auditDetailsService.update(auditDetails, transactionTime, force).equals(Boolean.FALSE))
        throw new InternalServerException("Couldn't update auditDetails");
      
      contribution.setAuditDetails(auditDetails);

      contribution.setId(UuidGenerator.randomUUID()); // force to create new entry from old values
      return persister.persistAllways(contribution) == 1;
    }

    return false;
  }

  public Boolean update(Contribution contribution) {
    return update(contribution, TransactionTime.millis());
  }

  public Boolean update(Contribution contribution, Boolean force) {
    return update(contribution, TransactionTime.millis(), force);
  }
  
  public Integer delete(Contribution contribution) {
    return persister.delete(contribution);
  }
}
