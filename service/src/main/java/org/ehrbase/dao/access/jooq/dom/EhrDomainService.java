package org.ehrbase.dao.access.jooq.dom;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.util.ContributionDef;

public interface EhrDomainService {

  UUID findBySubject(UUID subjectUuid);

  UUID findBySubject(String subjectId, String issuerSpace);

  UUID findBySubjectExternalRef(String subjectId, String issuerSpace);

  Ehr findByStatus(UUID ehrId, UUID status, Integer version);

  Ehr find(UUID ehrId);

  Map<String, Object> fetchSubjectIdentifiers(UUID ehrId);

  Map<String, Map<String, String>> getCompositionList(UUID ehrId);

  boolean removeDirectory(UUID ehrId);

  UUID create(Ehr ehr, Timestamp transactionTime);

  UUID create(Ehr ehr, UUID committerId, UUID systemId, String description);

  Boolean update(Ehr ehr, Timestamp transactionTime);

  Boolean update(Ehr ehr, Timestamp transactionTime, boolean force);

  boolean isSubjectAssignedToEhr(UUID partyId);

  Boolean update(Ehr ehr, UUID committerId, UUID systemId, UUID contributionId, ContributionDef.ContributionState state,
      I_ConceptAccess.ContributionChangeType contributionChangeType, String description);
  
  boolean exists(UUID ehrId);

  boolean isModifiable(UUID ehrId);

}