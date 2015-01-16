/;
DROP procedure IF EXISTS plus1inout
/;
CREATE procedure plus1inout (IN arg int, OUT res int)  
BEGIN ATOMIC  
	set res = arg + 1; 
END
/;
DROP procedure IF EXISTS nooutput
/;
CREATE procedure nooutput (IN arg int)  
BEGIN ATOMIC 
	declare res int;
	set res = arg + 1; 
END
/;