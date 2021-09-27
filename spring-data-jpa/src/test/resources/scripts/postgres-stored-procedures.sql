CREATE TABLE employee
(
    ID   numeric                           NOT NULL,
    name text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT employee_pkey PRIMARY KEY (ID)
);;

INSERT INTO employee (ID, NAME) VALUES (3, 'Fanny');;
INSERT INTO employee (ID, NAME) VALUES (4, 'Gabriel');;

CREATE OR REPLACE FUNCTION get_employees()
    RETURNS refcursor
    LANGUAGE 'plpgsql'
AS
$BODY$
DECLARE
    ref refcursor;
BEGIN
    OPEN ref FOR SELECT * FROM employee;
    RETURN ref;
END;
$BODY$;;

CREATE OR REPLACE FUNCTION get_employees_count()
    RETURNS integer
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    RETURN (SELECT COUNT(*) FROM employee);
END;
$BODY$;;

CREATE OR REPLACE FUNCTION get_single_employee()
    RETURNS refcursor
    LANGUAGE 'plpgsql'
AS
$BODY$
DECLARE
    ref refcursor;
BEGIN
    OPEN ref FOR SELECT * FROM employee WHERE employee.ID = 3;
    RETURN ref;
END;
$BODY$;;
