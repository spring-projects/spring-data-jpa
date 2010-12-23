package org.springframework.data.jpa.repository.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.sample.User;


/**
 * Dummy implementation to allow check for invoking a custom implementation.
 * 
 * @author Oliver Gierke
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

    private static final Logger LOG = LoggerFactory
            .getLogger(UserRepositoryImpl.class);


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#
     * someCustomMethod(org.springframework.data.jpa.domain.sample.User)
     */
    public void someCustomMethod(User u) {

        LOG.debug("Some custom method was invoked!");
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.sample.UserRepositoryCustom#
     * findByOverrridingMethod()
     */
    public void findByOverrridingMethod() {

        LOG.debug("A method overriding a finder was invoked!");
    }
}
