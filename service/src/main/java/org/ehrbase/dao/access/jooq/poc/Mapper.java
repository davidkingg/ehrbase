package org.ehrbase.dao.access.jooq.poc;

import static org.ehrbase.jooq.pg.Tables.STATUS;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.records.StatusHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;

public class Mapper {
  public static StatusRecord from(StatusHistoryRecord statusHistoryRecord) {
    StatusRecord statusRecord = new StatusRecord();
    //:TODO maybe add ID
    statusRecord.setEhrId(statusHistoryRecord.getEhrId());
    statusRecord.setIsQueryable(statusHistoryRecord.getIsQueryable());
    statusRecord.setIsModifiable(statusHistoryRecord.getIsModifiable());
    statusRecord.setParty(statusHistoryRecord.getParty());
    statusRecord.setOtherDetails(statusHistoryRecord.getOtherDetails());
    statusRecord.setSysTransaction(statusHistoryRecord.getSysTransaction());
    statusRecord.setSysPeriod(statusHistoryRecord.getSysPeriod());
    statusRecord.setHasAudit(statusHistoryRecord.getHasAudit());
    statusRecord.setAttestationRef(statusHistoryRecord.getAttestationRef());
    statusRecord.setInContribution(statusHistoryRecord.getInContribution());
    statusRecord.setArchetypeNodeId(statusHistoryRecord.getArchetypeNodeId());
    statusRecord.setName(statusHistoryRecord.getName());
    statusRecord.setNamespace(statusHistoryRecord.getNamespace());
    return statusRecord;
  }
  
  public static StatusHistoryRecord from(StatusRecord statusRecord) {
    StatusHistoryRecord statusHistoryRecord = new StatusHistoryRecord();
    //:TODO maybe add ID
    statusHistoryRecord.setId(statusRecord.getId());
    statusHistoryRecord.setEhrId(statusRecord.getEhrId());
    statusHistoryRecord.setIsQueryable(statusRecord.getIsQueryable());
    statusHistoryRecord.setIsModifiable(statusRecord.getIsModifiable());
    statusHistoryRecord.setParty(statusRecord.getParty());
    statusHistoryRecord.setOtherDetails(statusRecord.getOtherDetails());
    statusHistoryRecord.setSysTransaction(statusRecord.getSysTransaction());
    statusHistoryRecord.setSysPeriod(statusRecord.getSysPeriod());
    statusHistoryRecord.setHasAudit(statusRecord.getHasAudit());
    statusHistoryRecord.setAttestationRef(statusRecord.getAttestationRef());
    statusHistoryRecord.setInContribution(statusRecord.getInContribution());
    statusHistoryRecord.setArchetypeNodeId(statusRecord.getArchetypeNodeId());
    statusHistoryRecord.setName(statusRecord.getName());
    statusHistoryRecord.setNamespace(statusRecord.getNamespace());
    return statusHistoryRecord;
  }  
}
