package org.ehrbase.dao.access.jooq.dom;

import java.sql.Connection;

import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.IntrospectService;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PersistentCtx implements I_DomainAccess {

    private final DSLContext context;
    private final I_KnowledgeCache knowledgeManager;
    private final IntrospectService introspectService;

    private final ServerConfig serverConfig;

    @Autowired
    public PersistentCtx(
            DSLContext context,
            KnowledgeCacheService knowledgeManager,
            IntrospectService introspectService,
            ServerConfig serverConfig) {
        this.context = context;
        this.knowledgeManager = knowledgeManager;
        this.introspectService = introspectService;
        this.serverConfig = serverConfig;
    }

    public PersistentCtx(I_DomainAccess domainAccess) {
        this.context = domainAccess.getContext();
        this.knowledgeManager = domainAccess.getKnowledgeManager();
        this.introspectService = domainAccess.getIntrospectService();
        this.serverConfig = domainAccess.getServerConfig();
    }

    @Override
    public SQLDialect getDialect() {
        return context.dialect();
    }

    @Override
    public Connection getConnection() {
        return context.configuration().connectionProvider().acquire();
    }

    @Override
    public void releaseConnection(Connection connection) {
        context.configuration().connectionProvider().release(connection);
    }

    @Override
    public DSLContext getContext() {
        return context;
    }

    @Override
    public I_KnowledgeCache getKnowledgeManager() {
        return knowledgeManager;
    }

    @Override
    public IntrospectService getIntrospectService() {
        return introspectService;
    }

    @Override
    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }

    @Override
    public org.ehrbase.dao.access.support.DataAccess getDataAccess() {
      throw new UnsupportedOperationException();
    }
}
