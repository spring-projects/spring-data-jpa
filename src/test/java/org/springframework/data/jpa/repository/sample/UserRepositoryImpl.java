package org.springframework.data.jpa.repository.sample;

import org.springframework.data.jpa.domain.sample.User;


/**
 * Dummy implementation to allow check for invoking a custom implementation.
 * 
 * @author Oliver Gierke
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.synyx.hades.dao.UserDao#someOtherMethod(org.synyx.hades.domain.User)
     */
    public void someCustomMethod(User u) {

        System.out.println("Some custom method was invoked!");
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.synyx.hades.dao.UserDaoCustom#findFooMethod()
     */
    public void findByOverrridingMethod() {

        System.out.println("A mthod overriding a finder was invoked!");
    }
}
