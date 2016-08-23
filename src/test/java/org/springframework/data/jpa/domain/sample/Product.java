package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Product {

	@Id @GeneratedValue private Long id;

	public Long getId() {
		return id;
	}
}
