package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractPersistable;


/**
 * @author Oliver Gierke
 */
@Entity
public class Account extends AbstractPersistable<Long> {

    private static final long serialVersionUID = -5719129808165758887L;
}
