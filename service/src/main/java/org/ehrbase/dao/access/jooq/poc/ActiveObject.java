package org.ehrbase.dao.access.jooq.poc;

public interface ActiveObject {
  public default Integer persistAllways() {
    return persist();
  };
  public Integer persist();
  public Boolean update();
  public Integer delete();
  public boolean isDirty();

}
