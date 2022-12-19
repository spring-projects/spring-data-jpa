CREATE TABLE test_model
(
    ID   numeric                           NOT NULL,
    uuid UUID,
    local_date DATE,
    CONSTRAINT test_model_pk PRIMARY KEY (ID)
);;

CREATE OR REPLACE FUNCTION countByUuid(this_uuid uuid)
    RETURNS int
    LANGUAGE 'plpgsql'
AS
$BODY$
DECLARE
    c integer;
BEGIN
    SELECT count(*)
    INTO c
    FROM test_model
    WHERE test_model.uuid = this_uuid;
    RETURN c;
END;
$BODY$
;;

CREATE OR REPLACE FUNCTION countByLocalDate(this_local_date DATE)
    RETURNS int
    LANGUAGE 'plpgsql'
AS
$BODY$
DECLARE
    c integer;
BEGIN
    SELECT count(*)
    INTO c
    FROM test_model
    WHERE test_model.local_date = this_local_date;
    RETURN c;
END;
$BODY$
;;
