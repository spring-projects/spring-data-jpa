CREATE TABLE test_model
(
    ID   numeric                           NOT NULL,
    uuid UUID,
    local_date DATE,
    CONSTRAINT test_model_pk PRIMARY KEY (ID)
);;

CREATE OR REPLACE PROCEDURE countByUuid(IN this_uuid uuid)
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
END;
$BODY$
;;

CREATE OR REPLACE PROCEDURE countByLocalDate(IN this_local_date DATE)
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
END;
$BODY$
;;
