/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.*;
import org.springframework.data.jpa.repository.sample.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Sviataslau Apanasionak
 */
//DATAJPA-1572
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfig.class)
@Transactional
public class RepositorySortOptionalNotOptionalCountTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private LaptopRepository laptopRepository;

    @Autowired
    private ComputerRepository computerRepository;


    @Autowired
    private PhoneRepository phoneRepository;

    @Before
    public void init(){
    	phoneRepository.deleteAllInBatch();
    	computerRepository.deleteAllInBatch();
    	laptopRepository.deleteAllInBatch();
    	personRepository.deleteAllInBatch();
    	roomRepository.deleteAllInBatch();
    	floorRepository.deleteAllInBatch();
    }

    @Test
    public void testManyToOneOptionalInRoot() {

    	Floor floor = new Floor();
        floor = floorRepository.save(floor);

        Room room = new Room();
        room.setFloor(floor);
        room = roomRepository.save(room);

        Person person1 = new Person();
        person1.setRoom(room);
        person1 = personRepository.save(person1);

        Person person2 = new Person();
        person2 = personRepository.save(person2);

        Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "room.floor.number"));
        List<Person> users = personRepository.findAll(sort);

        Assert.assertEquals(2, users.size());
    }

	@Test
	public void testManyToOneOptionalInNonRoot() {

		Floor floor = new Floor();
		floor = floorRepository.save(floor);

		Room room = new Room();
		room.setFloor(floor);
		room = roomRepository.save(room);

		Person person1 = new Person();
		person1.setRoom(room);
		person1 = personRepository.save(person1);

		Person person2 = new Person();
		person2 = personRepository.save(person2);

		Computer computer1 = new Computer();
		computer1.setPerson(person1);
		Computer computer2 = new Computer();
		computer2.setPerson(person2);

		computerRepository.save(computer1);
		computerRepository.save(computer2);

		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "person.room.floor.number"));
		List<Computer> computers = computerRepository.findAll(sort);

		Assert.assertEquals(2, computers.size());
	}

	@Test
	public void testOneToOneOptionalInRoot() {

		Floor floor = new Floor();
		floor = floorRepository.save(floor);

		Room room = new Room();
		room.setFloor(floor);
		room = roomRepository.save(room);

		Person person1 = new Person();
		person1.setRoom(room);
		person1 = personRepository.save(person1);

		Laptop laptop1 = new Laptop();
		laptop1 = laptopRepository.save(laptop1);

		Laptop laptop2 = new Laptop();
		laptop2.setPerson(person1);
		laptop2 = laptopRepository.save(laptop2);


		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "person.room.floor.number"));
		List<Laptop> laptops = laptopRepository.findAll(sort);

		Assert.assertEquals(2, laptops.size());
	}

	@Test
	public void testOneToOneOptionalInNonRoot() {

		Floor floor = new Floor();
		floor = floorRepository.save(floor);

		Room room = new Room();
		room.setFloor(floor);
		room = roomRepository.save(room);

		Person person1 = new Person();
		person1.setRoom(room);
		person1 = personRepository.save(person1);

		Person person2 = new Person();
		person2 = personRepository.save(person2);

		Phone phone1 = new Phone();
		phone1.setPerson(person1);

		Phone phone2 = new Phone();
		phone2.setPerson(person2);

		phoneRepository.save(phone1);
		phoneRepository.save(phone2);


		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "person.room.floor.number"));
		List<Phone> phones = phoneRepository.findAll(sort);

		Assert.assertEquals(2, phones.size());
	}

}
