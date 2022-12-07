package org.ehrbase.dao.access.jooq.poc;

import java.util.UUID;

import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.jooq.UpdatableRecord;

public interface Persister<R extends UpdatableRecord<R>,A extends ActiveObjectAware<R>> {
  
  public static <R extends UpdatableRecord<R>,A extends ActiveObjectAware<R>> Persister<R,A> persister(I_DomainAccess ctx) {
    return new DefaultPersister<>(ctx);
  }
  
  public Integer persistAllways(A rec);
  public Integer persist(A rec);
  public Boolean update(A rec);
  public Integer delete(A rec);
  public boolean isDirty(A rec);
  
  public static class DefaultPersister<R extends UpdatableRecord<R>, A extends ActiveObjectAware<R>> implements Persister<R, A> {
    private final I_DomainAccess ctx;
    
    public DefaultPersister(I_DomainAccess ctx) {
      this.ctx = ctx;
    }
    
    @Override
    public Integer persistAllways(A rec) {
      ctx.getContext().attach(rec.getActiveObject());
      if (rec.getId() == null)
        rec.setId(UUID.randomUUID());
      return rec.getActiveObject().insert();
    }

    @Override
    public Integer persist(A rec) {
      ctx.getContext().attach(rec.getActiveObject());
      if (rec.getId() == null)
        rec.setId(UUID.randomUUID());
      return rec.getActiveObject().store();
    }

    @Override
    public Boolean update(A rec) {
      ctx.getContext().attach(rec.getActiveObject());
      return rec.getActiveObject().update() == 1;
    }

    @Override
    public Integer delete(A rec) {
      ctx.getContext().attach(rec.getActiveObject());
      return rec.getActiveObject().delete();
    }

    @Override
    public boolean isDirty(A rec) {
      return rec.getActiveObject().changed();
    }
  }
}
