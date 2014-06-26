package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class PrimitiveVersionProperty {

	@Id Long id;
	@Version long version;
}
