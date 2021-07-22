CREATE TABLE employee
(
    ID   int NOT NULL,
    name varchar(50)    NOT NULL,
    primary key (ID)
);;

INSERT INTO employee (ID, NAME) VALUES (3, 'Fanny');;
INSERT INTO employee (ID, NAME) VALUES (4, 'Gabriel');;

DROP PROCEDURE IF EXISTS get_employees;;
CREATE PROCEDURE get_employees()
BEGIN
    SELECT * FROM employee;
END;;

DROP PROCEDURE IF EXISTS get_employees_count;;
CREATE PROCEDURE get_employees_count(OUT employees INT)
BEGIN
    SELECT COUNT(*) into employees FROM employee;
END;;

DROP PROCEDURE IF EXISTS get_single_employee;;
CREATE PROCEDURE get_single_employee()
BEGIN
    SELECT * FROM employee WHERE employee.ID = 3;
END;;
