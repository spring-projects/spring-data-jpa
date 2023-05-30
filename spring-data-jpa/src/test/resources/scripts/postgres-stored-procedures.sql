CREATE TABLE employee
(
    ID   numeric                           NOT NULL,
    name text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT employee_pkey PRIMARY KEY (ID)
);;

INSERT INTO employee (ID, NAME) VALUES (3, 'Fanny');;
INSERT INTO employee (ID, NAME) VALUES (4, 'Gabriel');;

CREATE OR REPLACE PROCEDURE get_employees(OUT ref refcursor)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    OPEN ref FOR SELECT * FROM employee;
END;
$BODY$;;

CREATE OR REPLACE PROCEDURE get_employees_count(OUT results integer)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    results = (SELECT COUNT(*) FROM employee);
END;
$BODY$;;

CREATE OR REPLACE PROCEDURE get_single_employee(OUT ref refcursor)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    OPEN ref FOR SELECT * FROM employee WHERE employee.ID = 3;
END;
$BODY$;;
