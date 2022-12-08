package org.ehrbase.dao.access.jooq.dom;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import com.nedap.archie.rm.support.identification.ObjectVersionId;

@Service
public class StatusDomainServiceImpl implements StatusDomainService {

  private final I_DomainAccess domainAccess;
  private final ContributionDomainService contributionService;
  private final AuditDetailDomainService auditDetailsService;
  private final Persister<StatusRecord,Status> persister;
  

  public StatusDomainServiceImpl(I_DomainAccess domainAccess, ContributionDomainService contributionService,
      AuditDetailDomainService auditDetailsService) {
    this.domainAccess = domainAccess;
    this.contributionService = contributionService;
    this.auditDetailsService = auditDetailsService;
    this.persister = Persister.persister(domainAccess);
  }

  public Status retrieveInstance(UUID statusId) {
    StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));

    if(record == null)
      return null;

    return createStatusAccessForRetrieval(record, null, record.getNamespace());
  }

  public Status retrieveInstanceByNamedSubject(String partyName) {
    DSLContext context = domainAccess.getContext();

    StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.PARTY.eq(
        context.select(PARTY_IDENTIFIED.ID).from(PARTY_IDENTIFIED).where(PARTY_IDENTIFIED.NAME.eq(partyName))));

    if(record == null)
      return null;
    return createStatusAccessForRetrieval(record, null, record.getNamespace());
  }

  public Status retrieveInstanceByParty(UUID partyIdentified) {
    DSLContext context = domainAccess.getContext();

    StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.PARTY
        .eq(context.select(PARTY_IDENTIFIED.ID).from(PARTY_IDENTIFIED).where(PARTY_IDENTIFIED.ID.eq(partyIdentified))));

    if(record == null)
      return null;

    return createStatusAccessForRetrieval(record, null, record.getNamespace());
  }

  public Status retrieveByVersion(UUID statusId, int version) {
    if(version == getLatestVersionNumber(statusId))
      return retrieveInstance(statusId);

    Map<Integer, Status> allVersions = getVersionMapOfStatus(statusId);
    return allVersions.get(Integer.valueOf(version));
  }

  public Status retrieveInstanceByEhrId(UUID ehrId) {
    StatusRecord record = null;

    if (domainAccess.getContext().fetchExists(STATUS, STATUS.EHR_ID.eq(ehrId)))
      record = domainAccess.getContext().fetchOne(STATUS, STATUS.EHR_ID.eq(ehrId));
    else {
      if (!domainAccess.getContext().fetchExists(STATUS_HISTORY, STATUS_HISTORY.EHR_ID.eq(ehrId)))
        throw new InternalServerException("DB inconsistency. No STATUS for given EHR ID: " + ehrId);
      else {
        Result<StatusHistoryRecord> recordsRes = domainAccess.getContext()
            .selectFrom(STATUS_HISTORY).where(STATUS_HISTORY.EHR_ID.eq(ehrId)).orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()).fetch();
        if (recordsRes.get(0) != null)
          record = Mapper.from(recordsRes.get(0));
      }
    }

    if (record == null)
      return null;
    return createStatusAccessForRetrieval(record, null, record.getNamespace());
  }

  public Map<ObjectVersionId, Status> retrieveInstanceByContribution(UUID contributionId, String node) {
    Set<UUID> statuses = new HashSet<>();
    domainAccess.getContext()
      .select(STATUS.ID).from(STATUS).where(STATUS.IN_CONTRIBUTION.eq(contributionId)).fetch()
      .forEach(rec -> statuses.add(rec.value1()));

    domainAccess.getContext()
      .select(STATUS_HISTORY.ID).from(STATUS_HISTORY).where(STATUS_HISTORY.IN_CONTRIBUTION.eq(contributionId)).fetch().forEach(rec -> statuses.add(rec.value1()));

    Map<ObjectVersionId, Status> resultMap = new HashMap<>();
    
    statuses.forEach(statusId -> {
      Map<Integer, Status> map = getVersionMapOfStatus(statusId);

      map.forEach((k, v) -> {
        if(v.getContributionId().equals(contributionId))
          resultMap.put(
            new ObjectVersionId(statusId.toString(), node, k.toString()),
            v);
      });
    });

    return resultMap;
  }

  public Map<Integer, Status> getVersionMapOfStatus(UUID statusId) {
    Map<Integer, Status> versionMap = new HashMap<>();
    Integer versionCounter = getLatestVersionNumber(statusId);

    StatusRecord record = domainAccess.getContext().fetchOne(STATUS, STATUS.ID.eq(statusId));
    if (record != null) {
      Status statusAccess = createStatusAccessForRetrieval(record, null, record.getNamespace());
      versionMap.put(versionCounter, statusAccess);
      versionCounter--;
    }

    Result<StatusHistoryRecord> historyRecords = domainAccess.getContext()
      .selectFrom(STATUS_HISTORY).where(STATUS_HISTORY.ID.eq(statusId)).orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()).fetch();

    for (StatusHistoryRecord historyRecord : historyRecords) {
      Status historyAccess = createStatusAccessForRetrieval(null, historyRecord, historyRecord.getNamespace());
      versionMap.put(versionCounter, historyAccess);
      versionCounter--;
    }

    if (versionCounter != 0)
      throw new InternalServerException("Version Map generation failed");

    return versionMap;
  }
  
  public UUID commit(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
    createAndSetContribution(status, committerId, systemId, description, ContributionChangeType.CREATION);
    return internalCommit(status, timestamp);
  }

  public UUID commit(Status status, LocalDateTime timestamp, UUID contribution) {
    if(contribution == null)
      throw new InternalServerException("Invalid null valued contribution.");

    status.setContributionId(contribution);
    return internalCommit(status, timestamp);
  }

  private UUID internalCommit(Status status, LocalDateTime transactionTime) {
    AuditDetail auditDetailsAccess = status.getAuditDetailsAccess();
    
    auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, I_ConceptAccess.ContributionChangeType.CREATION));
    
    if (auditDetailsAccess.getChangeType() == null || auditDetailsAccess.getSystemId() == null || auditDetailsAccess.getCommitter() == null)
      throw new InternalServerException("Illegal to commit AuditDetailsAccess without setting mandatory fields.");
    
    auditDetailsService.commit(auditDetailsAccess);
    
    status.setHasAudit(auditDetailsAccess.getId());
    status.setSysTransaction(Timestamp.valueOf(transactionTime));

    if(persister.persist(status) == 0)
      throw new InvalidApiParameterException("Input EHR couldn't be stored; Storing EHR_STATUS failed");

    return status.getId();
  }
  
  private void createAndSetContribution(Status status, UUID committerId, UUID systemId, String description, ContributionChangeType changeType) {
    Contribution contributionAccess = status.getContributionAccess();
    contributionAccess.setAuditDetailsChangeType(I_ConceptAccess.fetchContributionChangeType(domainAccess, changeType));
    
    if(contributionAccess.getAuditsCommitter() == null || contributionAccess.getAuditsSystemId() == null) {
      if(committerId == null || systemId == null)
        throw new InternalServerException("Illegal to commit the contribution's AuditDetailsAccess without setting mandatory fields.");
      else {
        contributionAccess.setAuditDetailsCommitter(committerId);
        contributionAccess.setAuditDetailsSystemId(systemId);
        contributionAccess.setAuditDetailsDescription(description);
      }
    }
    
    contributionService.commit(contributionAccess);
    status.setContributionId(contributionAccess.getId());
  }
  
  public boolean update(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description, ContributionChangeType changeType) {
    createAndSetContribution(status, committerId, systemId, description, ContributionChangeType.MODIFICATION);
    return internalUpdate(status, timestamp);
  }

  public boolean update(Status status, LocalDateTime timestamp, UUID contribution) {
    if (contribution == null)
      throw new InternalServerException("Invalid null valued contribution.");
    status.setContributionId(contribution);
    return internalUpdate(status, timestamp);
  }

  private Boolean internalUpdate(Status status, LocalDateTime transactionTime) {
    AuditDetail auditDetailsAccess = status.getAuditDetailsAccess();
    auditDetailsService.commit(auditDetailsAccess);
    status.setHasAudit(auditDetailsAccess.getId());
    status.setSysTransaction(Timestamp.valueOf(transactionTime));

    try {
      return persister.update(status);
    } catch (RuntimeException e) {
      throw new InvalidApiParameterException("Couldn't marshall given EHR_STATUS / OTHER_DETAILS, content probably breaks RM rules");
    }
  }
  
  public int delete(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
    createAndSetContribution(status, committerId, systemId, description, ContributionChangeType.DELETED);
    return internalDelete(status, timestamp, committerId, systemId, description);
  }

  public int delete(Status status, LocalDateTime timestamp, UUID contribution) {
    if (contribution == null)
      throw new InternalServerException("Invalid null valued contribution.");

    status.setContributionId(contribution);

    var newContributionAccess = I_ContributionAccess.retrieveInstance(domainAccess, contribution);
    
    return internalDelete(
        status,
        timestamp,
        newContributionAccess.getAuditsCommitter(),
        newContributionAccess.getAuditsSystemId(),
        newContributionAccess.getAuditsDescription()
    );
  }
  
  private Integer internalDelete(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
    String tenantIdentifier = status.getNamespace();
    status.setSysTransaction(Timestamp.valueOf(timestamp));
    persister.delete(status);

    var delAudit = new AuditDetail(
        domainAccess,
        systemId,
        committerId,
        I_ConceptAccess.ContributionChangeType.DELETED, description,
        tenantIdentifier);
    
    auditDetailsService.commit(delAudit);
    
    return createAndCommitNewDeletedVersionAsHistory(
        status,
        delAudit.getId(),
        status.getContributionId(),
        tenantIdentifier);
  }
  
  private int createAndCommitNewDeletedVersionAsHistory(Status status, UUID delAuditId, UUID contrib, String tenantIdentifier) {
    StatusHistoryRecord newRecord = Mapper.from(status.getActiveObject());
      newRecord.setInContribution(contrib);
      newRecord.setNamespace(tenantIdentifier);
      newRecord.setHasAudit(delAuditId);
      
    Status historyStatusAccess = new Status(domainAccess, contrib, newRecord);

    if(persister.persist(historyStatusAccess) != 1)
      throw new InternalServerException("DB inconsistency");
    else
      return 1;
  }
  
  public int getEhrStatusVersionFromTimeStamp(UUID statusUid, Timestamp time) {
    Status retStatusAccess = retrieveInstance(statusUid);

    Result result = domainAccess.getContext()
      .selectFrom(STATUS_HISTORY).where(STATUS_HISTORY.ID.eq(statusUid)).orderBy(STATUS_HISTORY.SYS_TRANSACTION.desc()).fetch();

    if(time.after(retStatusAccess.getSysTransaction()))
      return getLatestVersionNumber(statusUid);

    for(int i = 0; i < result.size(); i++) {
      if(result.get(i) instanceof StatusHistoryRecord rec) {
        if (time.after(rec.getSysTransaction()))
          return result.size() - i;
      } else
        throw new InternalServerException("Problem comparing timestamps of EHR_STATUS versions");
    }

    throw new ObjectNotFoundException("EHR_STATUS", "Could not find EHR_STATUS version matching given timestamp");
  }  

  private Status createStatusAccessForRetrieval(StatusRecord record, StatusHistoryRecord historyRecord, String tenantIdentifier) {
    Status statusAccess;
    
    if(record != null)
      statusAccess = new Status(domainAccess, record.getEhrId(), record);
    else if(historyRecord != null)
      statusAccess = new Status(domainAccess, historyRecord.getEhrId(), Mapper.from(historyRecord));
    else
      throw new InternalServerException("Error creating version map of EHR_STATUS");

    AuditDetail auditAccess = auditDetailsService.retrieveInstance(statusAccess.getAuditDetailsId());
    statusAccess.setAuditDetailsAccess(auditAccess);

    Contribution retContributionAccess = contributionService.retrieveInstance(statusAccess.getContributionId());
    statusAccess.setContributionAccess(retContributionAccess);

    return statusAccess;
  }

  public Integer getLatestVersionNumber(UUID statusId) {
    int versionCount = domainAccess.getContext().fetchCount(STATUS_HISTORY, STATUS_HISTORY.ID.eq(statusId));
    return versionCount == 0 ? 1 : versionCount + 1;
  }

  public boolean exists(UUID ehrStatusId) {
    return domainAccess.getContext().fetchExists(STATUS, STATUS.ID.eq(ehrStatusId));
  }
}
