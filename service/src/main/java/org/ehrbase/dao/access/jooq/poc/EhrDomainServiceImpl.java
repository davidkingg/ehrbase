package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.STATUS;
import static org.ehrbase.jooq.pg.Tables.STATUS_HISTORY;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.BooleanUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.ContributionDef.ContributionState;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.functional.Try;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EhrDomainServiceImpl implements EhrDomainService {
    private static final Logger logger = LoggerFactory.getLogger(EhrDomainServiceImpl.class);
    public static final String JSONB = "::jsonb";
    public static final String EXCEPTION = " exception:";
    public static final String COULD_NOT_RETRIEVE_EHR_FOR_ID = "Could not retrieve EHR for id:";
    public static final String COULD_NOT_RETRIEVE_EHR_FOR_PARTY = "Could not retrieve EHR for party:";

    // set this variable to change the identification  mode in status
    public enum PARTY_MODE {
        IDENTIFIER,
        EXTERNAL_REF
    }

    private final I_DomainAccess domainAccess;
    private final StatusDomainService statusService;
    private final ContributionDomainService contributionService;
    private final Persister<EhrRecord, Ehr> persister;
    
    
    public EhrDomainServiceImpl(I_DomainAccess domainAccess, StatusDomainService statusService, ContributionDomainService contributionService) {
      this.domainAccess = domainAccess;
      this.statusService = statusService;
      this.contributionService = contributionService;
      this.persister = Persister.persister(domainAccess);
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    private Try<UUID,IllegalArgumentException> retrieveBySubjectUUID(UUID subjectUuid, String partyIdentifiedUuid) {
      try {
          DSLContext context = domainAccess.getContext();
          
          Record record = context
              .select(STATUS.EHR_ID)
              .from(STATUS)
              .where(STATUS.PARTY
                  .eq(context
                      .select(PARTY_IDENTIFIED.ID)
                      .from(PARTY_IDENTIFIED)
                      .where(PARTY_IDENTIFIED.ID.eq(subjectUuid))))
              .fetchOne();

          if(record != null && record.size() != 0)
            return Try.success((UUID) record.getValue(0));
          
          logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + partyIdentifiedUuid);
          return Try.success(null);
      } catch (Exception e) {
        return Try.failure(new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + partyIdentifiedUuid + EXCEPTION + e));
      }
    }
    
    public UUID findBySubject(UUID subjectUuid) {
      return retrieveBySubjectUUID(subjectUuid, subjectUuid.toString()).getOrThrow();
    }

    public UUID findBySubject(String subjectId, String issuerSpace) {
        DSLContext context = domainAccess.getContext();
        
        IdentifierRecord identifierRecord = context.fetchOne(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(subjectId).and(IDENTIFIER.ISSUER.eq(issuerSpace)));

        if (identifierRecord == null)
            throw new IllegalArgumentException("Could not invalidateContent an identified party for code:" + subjectId + " issued by:" + issuerSpace);

        return retrieveBySubjectUUID(identifierRecord.getParty(), subjectId).getOrThrow();
    }

    public UUID findBySubjectExternalRef(String subjectId, String issuerSpace) {
        try {
            DSLContext context = domainAccess.getContext();
            Record record = context
                .select(STATUS.EHR_ID)
                .from(STATUS)
                .where(STATUS.PARTY.eq(context.select(PARTY_IDENTIFIED.ID)
                        .from(PARTY_IDENTIFIED)
                        .where(PARTY_IDENTIFIED
                                .PARTY_REF_VALUE
                                .eq(subjectId)
                                .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.eq(issuerSpace)))))
                .fetchOne();
            
            if(record != null && record.size() != 0)
              return (UUID) record.getValue(0);

            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectId);
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_PARTY + subjectId + EXCEPTION + e);
        }
    }

    public Ehr findByStatus(UUID ehrId, UUID status, Integer version) {
        if (version < 1)
            throw new IllegalArgumentException("Version number must be > 0");

        Status statusAccess = statusService.retrieveInstance(status);
        DSLContext context = domainAccess.getContext();
        Integer versions = context.fetchCount(STATUS_HISTORY, STATUS_HISTORY.EHR_ID.eq(statusAccess.getEhrId())) + 1;
        
        if (versions > version && !version.equals(versions)) {
            Result<StatusHistoryRecord> result = context.selectFrom(STATUS_HISTORY).where(STATUS_HISTORY.EHR_ID.eq(ehrId)).orderBy(STATUS_HISTORY.SYS_TRANSACTION.asc()).fetch();
            if (result.isEmpty())
                throw new InternalServerException("Error retrieving EHR_STATUS"); // should never be reached
            StatusHistoryRecord statusHistoryRecord = result.get(version - 1);
            statusAccess = new Status(domainAccess, ehrId, Mapper.from(statusHistoryRecord));
        }

        try {
          EhrRecord record = context.selectFrom(EHR_).where(EHR_.ID.eq(statusAccess.getEhrId())).fetchOne();
          Ehr ehrAccess = new Ehr(domainAccess, record);
          ehrAccess.setStatusAccess(statusAccess);
          
          if (record.size() == 0) {
            logger.warn("Could not retrieveInstanceByNamedSubject ehr for status:" + status);
            return null;
          }

          ehrAccess.setNew(false);
          return ehrAccess;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not retrieveInstanceByNamedSubject EHR for status:" + status + EXCEPTION + e);
        }
    }

    public Ehr find(UUID ehrId) {
        DSLContext context = domainAccess.getContext();

        EhrRecord record;

        try {
            record = context.selectFrom(EHR_).where(EHR_.ID.eq(ehrId)).fetchOne();
        } catch (Exception e) {
            throw new IllegalArgumentException(COULD_NOT_RETRIEVE_EHR_FOR_ID + ehrId + EXCEPTION + e);
        }

        if (record == null || record.size() == 0) {
            logger.warn(COULD_NOT_RETRIEVE_EHR_FOR_ID + ehrId);
            return null;
        }

        Ehr ehrAccess = new Ehr(domainAccess, record);
        ehrAccess.setStatusAccess(statusService.retrieveInstanceByEhrId(ehrAccess.getId()));

        if (ehrAccess.getStatusAccess().getStatusRecord().getOtherDetails() != null)
            ehrAccess.otherDetails = ehrAccess.getStatusAccess().getStatusRecord().getOtherDetails();

        ehrAccess.setNew(false);
        ehrAccess.setContributionAccess(contributionService.retrieveInstance(ehrAccess.getStatusAccess().getContributionId()));
        return ehrAccess;
    }

    public Map<String, Object> fetchSubjectIdentifiers(UUID ehrId) {
        Ehr ehrAccess = find(ehrId);
        DSLContext context = domainAccess.getContext();

        if (ehrAccess == null)         
            throw new IllegalArgumentException("No ehr found for id:" + ehrId);

        Map<String, Object> idlist = new MultiValueMap();

        context
          .selectFrom(IDENTIFIER).where(IDENTIFIER.PARTY.eq(getParty(ehrAccess))).fetch()
          .forEach(record -> {
            idlist.put("identifier_issuer", record.getIssuer());
            idlist.put("identifier_id_value", record.getIdValue());
          });

        context
          .selectFrom(PARTY_IDENTIFIED).where(PARTY_IDENTIFIED.ID.eq(getParty(ehrAccess))).fetch()
          .forEach(record -> {
            idlist.put("ref_name_space", record.getPartyRefNamespace());
            idlist.put("id_value", record.getPartyRefValue());
            idlist.put("ref_name_scheme", record.getPartyRefScheme());
            idlist.put("ref_party_type", record.getPartyRefType());
          });

        return idlist;
    }

    public Map<String, Map<String, String>> getCompositionList(UUID ehrId) {
        Ehr ehrAccess = find(ehrId);
        DSLContext context = domainAccess.getContext();

        if (ehrAccess == null)
            throw new IllegalArgumentException("No ehr found for id:" + ehrId);

        Map<String, Map<String, String>> compositionlist = new HashMap<>();

        context.selectFrom(ENTRY).where(ENTRY.COMPOSITION_ID.eq(context.select(COMPOSITION.ID).from(COMPOSITION).where(COMPOSITION.EHR_ID.eq(ehrId)))).fetch()
        .forEach(record -> {
          Map<String, String> details = new HashMap<>();
            details.put("composition_id", record.getCompositionId().toString());
            details.put("templateId", record.getTemplateId());
            details.put("date", record.getSysTransaction().toString());
          compositionlist.put("details", details);
        });

        return compositionlist;
    }

    private UUID getParty(Ehr ehrAccess) {
        return ehrAccess.getStatusRecord().getParty();
    }

    public boolean removeDirectory(UUID ehrId) {
        DSLContext ctx = domainAccess.getContext();
        return ctx.update(EHR_).setNull(EHR_.DIRECTORY).where(EHR_.ID.eq(ehrId)).execute() > 0;
    }
    
    public UUID create(Ehr ehr, Timestamp transactionTime) {
      ehr.setDateCreated(transactionTime);
      ehr.setDateCreatedTzid(transactionTime);
      
      persister.persist(ehr);

      UUID contributionId =  contributionService.commit(ehr.getContributionAccess(), transactionTime);

      if(ehr.isNew() && ehr.isStatusChanged()) {
        Status status = ehr.getStatusAccess();
        status.setContributionId(contributionId);
        status.setEhrId(ehr.getId());
        status.setOtherDetails(ehr.getOtherDetails());
        statusService.commit(status, transactionTime.toLocalDateTime(), contributionId);
        ehr.setStatusChanged(false);
      }
      return ehr.getId();
    }

    public UUID create(Ehr ehr, UUID committerId, UUID systemId, String description) {
      Contribution contributionAccess = ehr.getContributionAccess();
        contributionAccess.setAuditDetailsValues(committerId, systemId, description, ContributionChangeType.CREATION);
        contributionAccess.setDataType(ContributionDataType.ehr);
        contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);

      Status statusAccess = ehr.getStatusAccess();
      
      statusAccess.setAuditAndContributionAuditValues(
          systemId,
          committerId,
          description,
          ContributionChangeType.CREATION);

      return create(ehr, TransactionTime.millis());
    }
    
    //---------------------------------------------------------------------------------------------------------------------------------------------
    public Boolean update(Ehr ehr, Timestamp transactionTime) {
      return update(ehr, transactionTime, false);
    }

    public Boolean update(Ehr ehr, Timestamp transactionTime, boolean force) {
      boolean result = false;

      if(ehr.isStatusChanged()) {
        Contribution contributionAccess = ehr.getContributionAccess();
        Status statusAccess = ehr.getStatusAccess();
        
        statusAccess.setContributionAccess(contributionAccess);
        statusAccess.setAuditDetailsAccess(
          new AuditDetail(
            domainAccess,
            contributionAccess.getAuditsSystemId(),
            contributionAccess.getAuditsCommitter(),
            ContributionChangeType.MODIFICATION,
            contributionAccess.getAuditsDescription(),
            ehr.getNamespace()));
        statusAccess.setOtherDetails(ehr.getOtherDetails());
        
        
        result = statusService.update(
          statusAccess,
          LocalDateTime.ofInstant(transactionTime.toInstant(), ZoneId.systemDefault()),
          contributionAccess.getId());

        ehr.setStatusChanged(false);
      }

      if(force || persister.isDirty(ehr)) {
        ehr.setDateCreated(transactionTime);
        ehr.setDateCreatedTzid(transactionTime);
        result |= persister.update(ehr);
      }

      return result;
    }
    
    //:TODO rename this
    public boolean isSubjectAssignedToEhr(UUID partyId) {
      return domainAccess.getContext().fetchExists(STATUS, STATUS.PARTY.eq(partyId));
  }

    public Boolean update(Ehr ehr, UUID committerId, UUID systemId, UUID contributionId,
        ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description) {
      
      Timestamp timestamp = TransactionTime.millis();
      if(contributionId != null) {
        Contribution access = contributionService.retrieveInstance(contributionId);
        if(access == null)
          throw new InternalServerException("Can't update status with invalid contribution ID.");
        ehr.setContributionAccess(access);
      } else if(ehr.isStatusChanged()) {
        ehr.setContributionAccess(new Contribution(domainAccess, ehr.getId(), ehr.getNamespace()));
        provisionContributionAccess(ehr.getContributionAccess(), committerId, systemId, description, state, contributionChangeType);
        contributionService.commit(ehr.getContributionAccess());
      }
      return update(ehr, timestamp);
    }

    private void provisionContributionAccess(Contribution access, UUID committerId, UUID systemId,
        String description, ContributionDef.ContributionState state,
        I_ConceptAccess.ContributionChangeType contributionChangeType) {
      access.setAuditDetailsValues(committerId, systemId, description, contributionChangeType);
      access.setState(state);
      access.setDataType(ContributionDataType.ehr);
      access.setState(ContributionState.COMPLETE);
    }
    
    public boolean exists(UUID ehrId) {
      return domainAccess.getContext().fetchExists(EHR_, EHR_.ID.eq(ehrId));
    }

    public boolean isModifiable(UUID ehrId) {
      return BooleanUtils.isTrue(domainAccess.getContext()
        .select(STATUS.IS_MODIFIABLE).from(STATUS).where(STATUS.EHR_ID.eq(ehrId)).fetchOne(Record1::value1));
    }
}
