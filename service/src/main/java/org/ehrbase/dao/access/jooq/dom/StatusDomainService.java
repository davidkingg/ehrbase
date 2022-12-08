package org.ehrbase.dao.access.jooq.dom;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;

import com.nedap.archie.rm.support.identification.ObjectVersionId;

public interface StatusDomainService {

  Status retrieveInstance(UUID statusId);

  Status retrieveInstanceByNamedSubject(String partyName);

  Status retrieveInstanceByParty(UUID partyIdentified);

  Status retrieveByVersion(UUID statusId, int version);

  Status retrieveInstanceByEhrId(UUID ehrId);

  Map<ObjectVersionId, Status> retrieveInstanceByContribution(UUID contributionId, String node);

  Map<Integer, Status> getVersionMapOfStatus(UUID statusId);

  UUID commit(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description);

  UUID commit(Status status, LocalDateTime timestamp, UUID contribution);

  boolean update(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description, ContributionChangeType changeType);

  boolean update(Status status, LocalDateTime timestamp, UUID contribution);

  int delete(Status status, LocalDateTime timestamp, UUID committerId, UUID systemId, String description);

  int delete(Status status, LocalDateTime timestamp, UUID contribution);

  int getEhrStatusVersionFromTimeStamp(UUID statusUid, Timestamp time);

  Integer getLatestVersionNumber(UUID statusId);

  boolean exists(UUID ehrStatusId);

}