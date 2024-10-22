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

CREATE OR REPLACE PROCEDURE get_employees_count(OUT results integer)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    results = (SELECT COUNT(*) FROM employee);
END;
$BODY$;;

CREATE OR REPLACE PROCEDURE positional_inout_parameter_issue3460(IN inParam integer, INOUT inoutParam integer, OUT outParam integer)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    outParam = 3;
END;
$BODY$;;

CREATE OR REPLACE PROCEDURE multiple_out(IN someNumber integer, OUT some_cursor REFCURSOR,
                                         OUT result1 integer, OUT result2 integer)
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    result1 = 1 * someNumber;
    result2 = 2 * someNumber;

    OPEN some_cursor FOR SELECT COUNT(*) FROM employee;
END;
$BODY$;;

CREATE OR REPLACE PROCEDURE accept_array(IN some_chars VARCHAR(255)[],
                                         OUT dims VARCHAR(255))
    LANGUAGE 'plpgsql'
AS
$BODY$
BEGIN
    dims = array_dims(some_chars);
END;
$BODY$;;
