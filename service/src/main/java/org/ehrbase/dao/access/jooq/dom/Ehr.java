package org.ehrbase.dao.access.jooq.dom;

import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.STATUS;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.service.RecordedDvCodedText;
import org.ehrbase.service.RecordedDvText;
import org.ehrbase.util.UuidGenerator;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;

public class Ehr implements ActiveObjectAware<EhrRecord> {
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

    ehrRecord.setId(Objects.requireNonNullElseGet(ehrId, UuidGenerator::randomUUID));

    this.statusAccess = new Status(domainAccess, ehrRecord.getId(), tenantIdentifier);
    this.statusAccess.getActiveObject().setId(UuidGenerator.randomUUID());
    this.statusAccess.getActiveObject().setIsModifiable(true);
    this.statusAccess.getActiveObject().setIsQueryable(true);
    this.statusAccess.getActiveObject().setParty(partyId);
    this.statusAccess.getActiveObject().setEhrId(ehrRecord.getId());

    ehrRecord.setSystemId(systemId);
    ehrRecord.setDirectory(directoryId);
    ehrRecord.setAccess(accessId);
    ehrRecord.setNamespace(tenantIdentifier);

    if (ehrRecord.getSystemId() == null) { // storeComposition a default entry for the current system
      ehrRecord.setSystemId(I_SystemAccess.createOrRetrieveLocalSystem(domainAccess));
    }

    this.isNew = true;

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
    getStatusAccess().setModifiable(modifiable);
  }

  public void setArchetypeNodeId(String archetypeNodeId) {
    getStatusAccess().setArchetypeNodeId(archetypeNodeId);
  }

  public String getArchetypeNodeId() {
    return getStatusAccess().getArchetypeNodeId();
  }

  public void setName(DvText name) {
    new RecordedDvText().toDB(getStatusAccess().getActiveObject(), STATUS.NAME, name);
  }

  public void setName(DvCodedText name) {
    new RecordedDvCodedText().toDB(getStatusAccess().getActiveObject(), STATUS.NAME, name);
  }

  public void setQueryable(Boolean queryable) {
    getStatusAccess().setQueryable(queryable);
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

  public Timestamp getCreationDate() {
    return ehrRecord.getDateCreated();
  }
  
  public String getCreationdateTzid() {
    return ehrRecord.getDateCreatedTzid();
  }
  
  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public UUID getParty() {
    return getStatusAccess().getParty();
  }

  public void setParty(UUID partyId) {
    getStatusAccess().setParty(partyId);
  }

  public UUID getId() {
    return ehrRecord.getId();
  }

  public Boolean isModifiable() {
    return getStatusAccess().isModifiable();
  }

  public Boolean isQueryable() {
    return getStatusAccess().isQueryable();
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

    if(status.getArchetypeNodeId() != null)
      setArchetypeNodeId(status.getArchetypeNodeId());

    if(status.getName() != null)
      setName(status.getName());

    UUID subjectUuid = new PersistedPartyProxy(domainAccess.getDataAccess()).getOrCreate(status.getSubject(), getStatusAccess().getNamespace());
    setParty(subjectUuid);

    hasStatusChanged = true;
  }
  
  private static final String OBJECT_ID_TMPL = "%s::%s::%s";

  public EhrStatus getStatus() {
    EhrStatus status = new EhrStatus();

    status.setModifiable(isModifiable());
    status.setQueryable(isQueryable());

    if (getStatusAccess().getOtherDetails() != null)
      status.setOtherDetails(getStatusAccess().getOtherDetails());
    status.setArchetypeNodeId(getArchetypeNodeId());
    
    Object name = new RecordedDvCodedText().fromDB(getStatusAccess().getActiveObject(), STATUS.NAME);
    status.setName(name instanceof DvText ? (DvText) name : (DvCodedText) name);

    UUID statusId = getStatusAccess().getId();
    status.setUid(new HierObjectId(
        String.format(OBJECT_ID_TMPL, statusId.toString(), domainAccess.getServerConfig().getNodename(), getStatusAccess().getLatestVersionNumber())));

    PartySelf partySelf = (PartySelf) new PersistedPartyProxy(domainAccess).retrieve(getParty());
    status.setSubject(partySelf);

    return status;
  }

  @Override
  public EhrRecord getActiveObject() {
    return ehrRecord;
  }

  @Override
  public void setId(UUID id) {
    ehrRecord.setId(id);
  }
}
