package org.ehrbase.dao.access.jooq.poc;

import java.util.UUID;

public interface ActiveObjectAware<R extends org.jooq.Record> {
  R getActiveObject();
  UUID getId();
  void setId(UUID id);
}
