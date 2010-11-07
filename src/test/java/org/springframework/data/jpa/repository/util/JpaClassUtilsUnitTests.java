package org.springframework.data.jpa.repository.util;

import static org.junit.Assert.*;
import static org.springframework.data.jpa.repository.utils.JpaClassUtils.*;

import javax.persistence.Entity;

import org.junit.Test;


/**
 * @author Oliver Gierke
 */
public class JpaClassUtilsUnitTests {

    @Test
    public void usesSimpleClassNameIfNoEntityNameGiven() throws Exception {

        assertEquals("User", getEntityName(User.class));
        assertEquals("AnotherNamedUser", getEntityName(NamedUser.class));
    }

    static class User {

    }

    @Entity(name = "AnotherNamedUser")
    static class NamedUser {

    }
}
