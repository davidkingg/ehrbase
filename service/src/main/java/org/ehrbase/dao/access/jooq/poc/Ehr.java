package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.STATUS;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.records.AdminDeleteEhrFullRecord;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvText;
import org.ehrbase.util.UuidGenerator;
import org.jooq.Result;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;

public class Ehr implements ActiveObject {
  public static final String JSONB = "::jsonb";
  public static final String EXCEPTION = " exception:";
  public static final String COULD_NOT_RETRIEVE_EHR_FOR_ID = "Could not retrieve EHR for id:";
  public static final String COULD_NOT_RETRIEVE_EHR_FOR_PARTY = "Could not retrieve EHR for party:";

  private I_DomainAccess domainAccess;
  private EhrRecord ehrRecord;
  private boolean isNew = false;
  private boolean hasStatusChanged = false;

  ItemStructure otherDetails = null;

  private Contribution contributionAccess; // locally referenced contribution associated to ehr transactions
  private Status statusAccess; // associated EHR_STATUS. Each EHR has 1 EHR_STATUS

  public Ehr(I_DomainAccess domainAccess, UUID partyId, UUID systemId, UUID directoryId,
      UUID accessId, UUID ehrId, String tenantIdentifier) {

    this.domainAccess = domainAccess;
    this.ehrRecord = domainAccess.getContext().newRecord(EHR_);
    // checking for and executing case of custom ehr ID
    ehrRecord.setId(Objects.requireNonNullElseGet(ehrId, UuidGenerator::randomUUID));

    // init a new EHR_STATUS with default values to associate with this EHR
    this.statusAccess = new Status(domainAccess, ehrRecord.getId(), tenantIdentifier);
    this.statusAccess.getStatusRecord().setId(UuidGenerator.randomUUID());
    this.statusAccess.getStatusRecord().setIsModifiable(true);
    this.statusAccess.getStatusRecord().setIsQueryable(true);
    this.statusAccess.getStatusRecord().setParty(partyId);
    this.statusAccess.getStatusRecord().setEhrId(ehrRecord.getId());

    ehrRecord.setSystemId(systemId);
    ehrRecord.setDirectory(directoryId);
    ehrRecord.setAccess(accessId);
    ehrRecord.setNamespace(tenantIdentifier);

    if (ehrRecord.getSystemId() == null) { // storeComposition a default entry for the current system
      ehrRecord.setSystemId(I_SystemAccess.createOrRetrieveLocalSystem(domainAccess));
    }

    this.isNew = true;

    // associate a contribution with this EHR
    contributionAccess = new Contribution(domainAccess, ehrRecord.getId(), tenantIdentifier);
    contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
  }

  public Ehr(I_DomainAccess domainAccess, EhrRecord record) {
    this.domainAccess = domainAccess;
    this.ehrRecord = record;

    statusAccess = new Status(domainAccess, record.getId(), record.getNamespace());
    contributionAccess = new Contribution(domainAccess, record.getId(), record.getNamespace());
    contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
  }
  
  public boolean isStatusChanged() {
    return hasStatusChanged;
  }
  
  public void setStatusChanged(boolean status) {
    hasStatusChanged = status;
  }
  
  public String getNamespace() {
    return ehrRecord.getNamespace();
  }

  public void setAccess(UUID access) {
    ehrRecord.setAccess(access);
  }

  public void setDirectory(UUID directory) {
    ehrRecord.setDirectory(directory);
  }

  public void setSystem(UUID system) {
    ehrRecord.setSystemId(system);
  }

  public void setModifiable(Boolean modifiable) {
    getStatusAccess().getStatusRecord().setIsModifiable(modifiable);
  }

  public void setArchetypeNodeId(String archetypeNodeId) {
    getStatusAccess().getStatusRecord().setArchetypeNodeId(archetypeNodeId);
  }

  public String getArchetypeNodeId() {
    return getStatusAccess().getStatusRecord().getArchetypeNodeId();
  }

  public void setName(DvText name) {
    new RecordedDvText().toDB(getStatusAccess().getStatusRecord(), STATUS.NAME, name);
  }

  public void setName(DvCodedText name) {
    new RecordedDvCodedText().toDB(getStatusAccess().getStatusRecord(), STATUS.NAME, name);
  }

  public void setQueryable(Boolean queryable) {
    getStatusAccess().getStatusRecord().setIsQueryable(queryable);
  }

  public void setDateCreated(Timestamp transactionTime) {
    ehrRecord.setDateCreated(transactionTime);
  }

  public void setDateCreatedTzid(Timestamp transactionTime) {
    ehrRecord.setDateCreatedTzid(
      OffsetDateTime
        .from(transactionTime.toLocalDateTime().atOffset(ZoneOffset.from(OffsetDateTime.now())))
      .getOffset()
      .getId()
    );
  }

  public EhrRecord getEhrRecord() {
    return ehrRecord;
  }

  public StatusRecord getStatusRecord() {
    return getStatusAccess().getStatusRecord();
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public UUID getParty() {
    return getStatusAccess().getStatusRecord().getParty();
  }

  public void setParty(UUID partyId) {
    getStatusAccess().getStatusRecord().setParty(partyId);
  }

  public UUID getId() {
    return ehrRecord.getId();
  }

  public Boolean isModifiable() {
    return getStatusAccess().getStatusRecord().getIsModifiable();
  }

  public Boolean isQueryable() {
    return getStatusAccess().getStatusRecord().getIsQueryable();
  }

  public UUID getSystemId() {
    return ehrRecord.getSystemId();
  }

  public UUID getStatusId() {
    return statusAccess.getId();
  }

  public UUID getDirectoryId() {
    return ehrRecord.getDirectory();
  }

  public UUID getAccessId() {
    return ehrRecord.getAccess();
  }

  public void setOtherDetails(ItemStructure otherDetails, String templateId) {
    this.otherDetails = otherDetails;
  }

  public ItemStructure getOtherDetails() {
    return otherDetails;
  }

  public Contribution getContributionAccess() {
    return contributionAccess;
  }

  public void setContributionAccess(Contribution contributionAccess) {
    this.contributionAccess = contributionAccess;
  }

  public Status getStatusAccess() {
    return this.statusAccess;
  }

  public void setStatusAccess(Status statusAccess) {
    this.statusAccess = statusAccess;
  }

  public void setStatus(EhrStatus status) {
    setModifiable(status.isModifiable());
    setQueryable(status.isQueryable());
    setOtherDetails(status.getOtherDetails(), null);

    // Locatable stuff if present
    if (status.getArchetypeNodeId() != null) {
      setArchetypeNodeId(status.getArchetypeNodeId());
    }

    if (status.getName() != null) {
      setName(status.getName());
    }

    UUID subjectUuid = new PersistedPartyProxy(domainAccess.getDataAccess()).getOrCreate(status.getSubject(),
        getStatusAccess().getStatusRecord().getNamespace());
    setParty(subjectUuid);

    hasStatusChanged = true;
  }

  public EhrStatus getStatus() {
    EhrStatus status = new EhrStatus();

    status.setModifiable(isModifiable());
    status.setQueryable(isQueryable());
    // set otherDetails if available
    if (getStatusAccess().getStatusRecord().getOtherDetails() != null) {
      status.setOtherDetails(getStatusAccess().getStatusRecord().getOtherDetails());
    }

    // Locatable attribute
    status.setArchetypeNodeId(getArchetypeNodeId());
    Object name = new RecordedDvCodedText().fromDB(getStatusAccess().getStatusRecord(), STATUS.NAME);
    status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

    UUID statusId = getStatusAccess().getStatusRecord().getId();
    status.setUid(new HierObjectId(statusId.toString() + "::" + domainAccess.getServerConfig().getNodename() + "::"
        + I_StatusAccess.getLatestVersionNumber(domainAccess, statusId)));

    PartySelf partySelf = (PartySelf) new PersistedPartyProxy(domainAccess).retrieve(getParty());
    status.setSubject(partySelf);

    return status;
  }

  @Override
  public Integer persist() {
    domainAccess.getContext().attach(ehrRecord);
    if(ehrRecord.getId() == null)
      ehrRecord.setId(UUID.randomUUID());
    return ehrRecord.store();
  }

  @Override
  public Boolean update() {
    domainAccess.getContext().attach(ehrRecord);
    return ehrRecord.update() == 1;
  }

  @Override
  public Integer delete() {
    throw new InternalServerException("INTERNAL: this delete is not legal");
  }

  @Override
  public boolean isDirty() {
    return ehrRecord.changed();
  }
  
  //------------------------------------------------------------------------------------------------------------------------
  //:TODO currently not impl. should be handeld by a specific REST/Service call with high priviledges  
  public void adminDeleteEhr() {
    Result<AdminDeleteEhrFullRecord> result = Routines.adminDeleteEhrFull(domainAccess.getContext().configuration(), this.getId());
    if (result.isEmpty() || !Boolean.TRUE.equals(result.get(0).getDeleted())) {
      throw new InternalServerException("Admin deletion of EHR failed!");
    }
  }
}
