package org.springframework.data.jpa.repository.support;

import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.EclipseLinkTemplates;
import com.querydsl.jpa.HQLTemplates;
import com.querydsl.jpa.OpenJPATemplates;
import com.querydsl.jpa.impl.JPADeleteClause;
import org.springframework.data.jpa.provider.PersistenceProvider;

import javax.persistence.EntityManager;

/**
 * Helper instance to ease access to DeleteClause JPA query API.
 *
 * @author Nikita Mishchenko
 */
public class DeleteQuerydsl {

    private final EntityManager em;
    private final PersistenceProvider provider;
    private final EntityPath<?> path;

    public DeleteQuerydsl(EntityManager em, EntityPath<?> path) {
        this.em = em;
        this.provider = PersistenceProvider.fromEntityManager(em);
        this.path = path;
    }

    public JPADeleteClause createQuery() {
        switch (provider) {
            case ECLIPSELINK:
                return new JPADeleteClause(em, path, EclipseLinkTemplates.DEFAULT);
            case HIBERNATE:
                return new JPADeleteClause(em, path, HQLTemplates.DEFAULT);
            case OPEN_JPA:
                return new JPADeleteClause(em, path, OpenJPATemplates.DEFAULT);
            case GENERIC_JPA:
            default:
                return new JPADeleteClause(em, path);
        }
    }

}
