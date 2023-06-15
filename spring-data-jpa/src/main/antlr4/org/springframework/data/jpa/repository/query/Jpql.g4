/*
 * Copyright 2011-2023 the original author or authors.
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
grammar Jpql;

@header {
/**
 * JPQL per https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#bnf
 *
 * This is JPA BNF for JPQL. There are gaps and inconsistencies in the BNF itself, explained by other fragments of the spec.
 *
 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#bnf
 * @author Greg Turnquist
 * @since 3.1
 */
}

/*
    Parser rules
 */

start
    : ql_statement EOF
    ;

ql_statement
    : select_statement
    | update_statement
    | delete_statement
    ;

select_statement
    : select_clause from_clause (where_clause)? (groupby_clause)? (having_clause)? (orderby_clause)?
    ;

update_statement
    : update_clause (where_clause)?
    ;

delete_statement
    : delete_clause (where_clause)?
    ;

from_clause
    : FROM identification_variable_declaration (',' identificationVariableDeclarationOrCollectionMemberDeclaration )*
    ;

// This parser rule is needed to iterate over these two types from #from_clause
identificationVariableDeclarationOrCollectionMemberDeclaration
    : identification_variable_declaration
    | collection_member_declaration
    ;

identification_variable_declaration
    : range_variable_declaration (join | fetch_join)*
    ;

range_variable_declaration
    : entity_name AS? identification_variable
    ;

join
    : join_spec join_association_path_expression AS? identification_variable (join_condition)?
    ;

fetch_join
    : join_spec FETCH join_association_path_expression
    ;

join_spec
    : ((LEFT (OUTER)?) | INNER)? JOIN
    ;

join_condition
    : ON conditional_expression
    ;

join_association_path_expression
    : join_collection_valued_path_expression
    | join_single_valued_path_expression
    | TREAT '(' join_collection_valued_path_expression AS subtype ')'
    | TREAT '(' join_single_valued_path_expression AS subtype ')'
    ;

join_collection_valued_path_expression
    : identification_variable '.' (single_valued_embeddable_object_field '.')* collection_valued_field
    ;

join_single_valued_path_expression
    : identification_variable '.' (single_valued_embeddable_object_field '.')* single_valued_object_field
    ;

collection_member_declaration
    : IN '(' collection_valued_path_expression ')' AS? identification_variable
    ;

qualified_identification_variable
    : map_field_identification_variable
    | ENTRY '(' identification_variable ')'
    ;

map_field_identification_variable
    : KEY '(' identification_variable ')'
    | VALUE '(' identification_variable ')'
    ;

single_valued_path_expression
    : qualified_identification_variable
    | TREAT '(' qualified_identification_variable AS subtype ')'
    | state_field_path_expression
    | single_valued_object_path_expression
    ;

general_identification_variable
    : identification_variable
    | map_field_identification_variable
    ;

general_subpath
    : simple_subpath
    | treated_subpath ('.' single_valued_object_field)*
    ;

simple_subpath
    : general_identification_variable
    | general_identification_variable ('.' single_valued_object_field)*
    ;

treated_subpath
    : TREAT '(' general_subpath AS subtype ')'
    ;

state_field_path_expression
    : general_subpath '.' state_field
    ;

state_valued_path_expression
    : state_field_path_expression
    | general_identification_variable
    ;

single_valued_object_path_expression
    : general_subpath '.' single_valued_object_field
    ;

collection_valued_path_expression
    : general_subpath '.' collection_value_field // BNF at end of spec has a typo
    ;

update_clause
    : UPDATE entity_name (AS? identification_variable)? SET update_item (',' update_item)*
    ;

update_item
    : (identification_variable '.')? (single_valued_embeddable_object_field '.')* (state_field | single_valued_object_field) '=' new_value
    ;

new_value
    : scalar_expression
    | simple_entity_expression
    | NULL
    ;

delete_clause
    : DELETE FROM entity_name (AS? identification_variable)?
    ;

select_clause
    : SELECT (DISTINCT)? select_item (',' select_item)*
    ;

select_item
    : select_expression (AS? result_variable)?
    ;

select_expression
    : single_valued_path_expression
    | scalar_expression
    | aggregate_expression
    | identification_variable
    | OBJECT '(' identification_variable ')'
    | constructor_expression
    ;

constructor_expression
    : NEW constructor_name '(' constructor_item (',' constructor_item)* ')'
    ;

constructor_item
    : single_valued_path_expression
    | scalar_expression
    | aggregate_expression
    | identification_variable
    ;

aggregate_expression
    : (AVG | MAX | MIN | SUM) '(' (DISTINCT)? state_valued_path_expression ')'
    | COUNT '(' (DISTINCT)? (identification_variable | state_valued_path_expression | single_valued_object_path_expression) ')'
    | function_invocation
    ;

where_clause
    : WHERE conditional_expression
    ;

groupby_clause
    : GROUP BY groupby_item (',' groupby_item)*
    ;

groupby_item
    : single_valued_path_expression
    | identification_variable
    ;

having_clause
    : HAVING conditional_expression
    ;

orderby_clause
    : ORDER BY orderby_item (',' orderby_item)*
    ;

// TODO Error in spec BNF, correctly shown elsewhere in spec.
orderby_item
    : (state_field_path_expression | general_identification_variable | result_variable ) (ASC | DESC)?
    ;

subquery
    : simple_select_clause subquery_from_clause (where_clause)? (groupby_clause)? (having_clause)?
    ;

subquery_from_clause
    : FROM subselect_identification_variable_declaration (',' (subselect_identification_variable_declaration | collection_member_declaration))*
    ;

subselect_identification_variable_declaration
    : identification_variable_declaration
    | derived_path_expression AS? identification_variable (join)*
    | derived_collection_member_declaration
    ;

derived_path_expression
    : general_derived_path '.' single_valued_object_field
    | general_derived_path '.' collection_valued_field
    ;

general_derived_path
    : simple_derived_path
    | treated_derived_path ('.' single_valued_object_field)*
    ;

simple_derived_path
    : superquery_identification_variable ('.' single_valued_object_field)*
    ;

treated_derived_path
    : TREAT '(' general_derived_path AS subtype ')'
    ;

derived_collection_member_declaration
    : IN superquery_identification_variable '.' (single_valued_object_field '.')* collection_valued_field
    ;

simple_select_clause
    : SELECT (DISTINCT)? simple_select_expression
    ;

simple_select_expression
    : single_valued_path_expression
    | scalar_expression
    | aggregate_expression
    | identification_variable
    ;

scalar_expression
    : arithmetic_expression
    | string_expression
    | enum_expression
    | datetime_expression
    | boolean_expression
    | case_expression
    | entity_type_expression
    ;

conditional_expression
    : conditional_term
    | conditional_expression OR conditional_term
    ;

conditional_term
    : conditional_factor
    | conditional_term AND conditional_factor
    ;

conditional_factor
    : (NOT)? conditional_primary
    ;

conditional_primary
    : simple_cond_expression
    | '(' conditional_expression ')'
    ;

simple_cond_expression
    : comparison_expression
    | between_expression
    | in_expression
    | like_expression
    | null_comparison_expression
    | empty_collection_comparison_expression
    | collection_member_expression
    | exists_expression
    ;

between_expression
    : arithmetic_expression (NOT)? BETWEEN arithmetic_expression AND arithmetic_expression
    | string_expression (NOT)? BETWEEN string_expression AND string_expression
    | datetime_expression (NOT)? BETWEEN datetime_expression AND datetime_expression
    ;

in_expression
    : (state_valued_path_expression | type_discriminator) (NOT)? IN (('(' in_item (',' in_item)* ')') | ( '(' subquery ')') | collection_valued_input_parameter)
    ;

in_item
    : literal
    | single_valued_input_parameter
    ;

like_expression
    : string_expression (NOT)? LIKE pattern_value (ESCAPE escape_character)?
    ;

null_comparison_expression
    : (single_valued_path_expression | input_parameter) IS (NOT)? NULL
    ;

empty_collection_comparison_expression
    : collection_valued_path_expression IS (NOT)? EMPTY
    ;

collection_member_expression
    : entity_or_value_expression (NOT)? MEMBER (OF)? collection_valued_path_expression
    ;

entity_or_value_expression
    : single_valued_object_path_expression
    | state_field_path_expression
    | simple_entity_or_value_expression
    ;

simple_entity_or_value_expression
    : identification_variable
    | input_parameter
    | literal
    ;

exists_expression
    : (NOT)? EXISTS '(' subquery ')'
    ;

all_or_any_expression
    : (ALL | ANY | SOME) '(' subquery ')'
    ;

comparison_expression
    : string_expression comparison_operator (string_expression | all_or_any_expression)
    | boolean_expression op=('=' | '<>') (boolean_expression | all_or_any_expression)
    | enum_expression op=('=' | '<>') (enum_expression | all_or_any_expression)
    | datetime_expression comparison_operator (datetime_expression | all_or_any_expression)
    | entity_expression op=('=' | '<>') (entity_expression | all_or_any_expression)
    | arithmetic_expression comparison_operator (arithmetic_expression | all_or_any_expression)
    | entity_type_expression op=('=' | '<>') entity_type_expression
    ;

comparison_operator
    : op='='
    | op='>'
    | op='>='
    | op='<'
    | op='<='
    | op='<>'
    ;

arithmetic_expression
    : arithmetic_term
    | arithmetic_expression op=('+' | '-') arithmetic_term
    ;

arithmetic_term
    : arithmetic_factor
    | arithmetic_term op=('*' | '/') arithmetic_factor
    ;

arithmetic_factor
    : op=('+' | '-')? arithmetic_primary
    ;

arithmetic_primary
    : state_valued_path_expression
    | numeric_literal
    | '(' arithmetic_expression ')'
    | input_parameter
    | functions_returning_numerics
    | aggregate_expression
    | case_expression
    | function_invocation
    | '(' subquery ')'
    ;

string_expression
    : state_valued_path_expression
    | string_literal
    | input_parameter
    | functions_returning_strings
    | aggregate_expression
    | case_expression
    | function_invocation
    | '(' subquery ')'
    ;

datetime_expression
    : state_valued_path_expression
    | input_parameter
    | functions_returning_datetime
    | aggregate_expression
    | case_expression
    | function_invocation
    | date_time_timestamp_literal
    | '(' subquery ')'
    ;

boolean_expression
    : state_valued_path_expression
    | boolean_literal
    | input_parameter
    | case_expression
    | function_invocation
    | '(' subquery ')'
    ;

enum_expression
    : state_valued_path_expression
    | enum_literal
    | input_parameter
    | case_expression
    | '(' subquery ')'
    ;

entity_expression
    : single_valued_object_path_expression
    | simple_entity_expression
    ;

simple_entity_expression
    : identification_variable
    | input_parameter
    ;

entity_type_expression
    : type_discriminator
    | entity_type_literal
    | input_parameter
    ;

type_discriminator
    : TYPE '(' (general_identification_variable | single_valued_object_path_expression | input_parameter) ')'
    ;

functions_returning_numerics
    : LENGTH '(' string_expression ')'
    | LOCATE '(' string_expression ',' string_expression (',' arithmetic_expression)? ')'
    | ABS '(' arithmetic_expression ')'
    | CEILING '(' arithmetic_expression ')'
    | EXP '(' arithmetic_expression ')'
    | FLOOR '(' arithmetic_expression ')'
    | LN '(' arithmetic_expression ')'
    | SIGN '(' arithmetic_expression ')'
    | SQRT '(' arithmetic_expression ')'
    | MOD '(' arithmetic_expression ',' arithmetic_expression ')'
    | POWER '(' arithmetic_expression ',' arithmetic_expression ')'
    | ROUND '(' arithmetic_expression ',' arithmetic_expression ')'
    | SIZE '(' collection_valued_path_expression ')'
    | INDEX '(' identification_variable ')'
    | extract_datetime_field
    ;

functions_returning_datetime
    : CURRENT_DATE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | LOCAL DATE
    | LOCAL TIME
    | LOCAL DATETIME
    | extract_datetime_part
    ;

functions_returning_strings
    : CONCAT '(' string_expression ',' string_expression (',' string_expression)* ')'
    | SUBSTRING '(' string_expression ',' arithmetic_expression (',' arithmetic_expression)? ')'
    | TRIM '(' ((trim_specification)? (trim_character)? FROM)? string_expression ')'
    | LOWER '(' string_expression ')'
    | UPPER '(' string_expression ')'
    ;

trim_specification
    : LEADING
    | TRAILING
    | BOTH
    ;


function_invocation
    : FUNCTION '(' function_name (',' function_arg)* ')'
    ;

extract_datetime_field
    : EXTRACT '(' datetime_field FROM datetime_expression ')'
    ;

datetime_field
    : identification_variable
    ;

extract_datetime_part
    : EXTRACT '(' datetime_part FROM datetime_expression ')'
    ;

datetime_part
    : identification_variable
    ;

function_arg
    : literal
    | state_valued_path_expression
    | input_parameter
    | scalar_expression
    ;

case_expression
    : general_case_expression
    | simple_case_expression
    | coalesce_expression
    | nullif_expression
    ;

general_case_expression
    : CASE when_clause (when_clause)* ELSE scalar_expression END
    ;

when_clause
    : WHEN conditional_expression THEN scalar_expression
    ;

simple_case_expression
    : CASE case_operand simple_when_clause (simple_when_clause)* ELSE scalar_expression END
    ;

case_operand
    : state_valued_path_expression
    | type_discriminator
    ;

simple_when_clause
    : WHEN scalar_expression THEN scalar_expression
    ;

coalesce_expression
    : COALESCE '(' scalar_expression (',' scalar_expression)+ ')'
    ;

nullif_expression
    : NULLIF '(' scalar_expression ',' scalar_expression ')'
    ;

/*******************
    Gaps in the spec.
 *******************/

trim_character
    : CHARACTER
    | character_valued_input_parameter
    ;

identification_variable
    : IDENTIFICATION_VARIABLE
    | f=(COUNT
    | INNER
    | KEY
    | LEFT
    | ORDER
    | OUTER
    | FLOOR
    | SIGN
    | VALUE)
    ;

constructor_name
    : state_field_path_expression
    ;

literal
    : STRINGLITERAL
    | INTLITERAL
    | FLOATLITERAL
    | boolean_literal
    | entity_type_literal
    ;

input_parameter
    : '?' INTLITERAL
    | ':' identification_variable
    ;

pattern_value
    : string_expression
    ;

date_time_timestamp_literal
    : STRINGLITERAL
    ;

entity_type_literal
    : identification_variable
    ;

escape_character
    : CHARACTER
    | character_valued_input_parameter //
    ;

numeric_literal
    : INTLITERAL
    | FLOATLITERAL
    ;

boolean_literal
    : TRUE
    | FALSE
    ;

enum_literal
    : state_field_path_expression
    ;

string_literal
    : CHARACTER
    | STRINGLITERAL
    ;

single_valued_embeddable_object_field
    : identification_variable
    ;

subtype
    : identification_variable
    ;

collection_valued_field
    : identification_variable
    ;

single_valued_object_field
    : identification_variable
    ;

state_field
    : identification_variable
    ;

collection_value_field
    : identification_variable
    ;

entity_name
    : identification_variable ('.' identification_variable)* // Hibernate sometimes expands the entity name to FQDN when using named queries
    ;

result_variable
    : identification_variable
    ;

superquery_identification_variable
    : identification_variable
    ;

collection_valued_input_parameter
    : input_parameter
    ;

single_valued_input_parameter
    : input_parameter
    ;

function_name
    : string_literal
    ;

character_valued_input_parameter
    : CHARACTER
    | input_parameter
    ;

/*
    Lexer rules
 */


WS                          : [ \t\r\n] -> skip ;

// Build up case-insentive tokens

fragment A: 'a' | 'A';
fragment B: 'b' | 'B';
fragment C: 'c' | 'C';
fragment D: 'd' | 'D';
fragment E: 'e' | 'E';
fragment F: 'f' | 'F';
fragment G: 'g' | 'G';
fragment H: 'h' | 'H';
fragment I: 'i' | 'I';
fragment J: 'j' | 'J';
fragment K: 'k' | 'K';
fragment L: 'l' | 'L';
fragment M: 'm' | 'M';
fragment N: 'n' | 'N';
fragment O: 'o' | 'O';
fragment P: 'p' | 'P';
fragment Q: 'q' | 'Q';
fragment R: 'r' | 'R';
fragment S: 's' | 'S';
fragment T: 't' | 'T';
fragment U: 'u' | 'U';
fragment V: 'v' | 'V';
fragment W: 'w' | 'W';
fragment X: 'x' | 'X';
fragment Y: 'y' | 'Y';
fragment Z: 'z' | 'Z';

// The following are reserved identifiers:

ABS                         : A B S;
ALL                         : A L L;
AND                         : A N D;
ANY                         : A N Y;
AS                          : A S;
ASC                         : A S C;
AVG                         : A V G;
BETWEEN                     : B E T W E E N;
BOTH                        : B O T H;
BY                          : B Y;
CASE                        : C A S E;
CEILING                     : C E I L I N G;
COALESCE                    : C O A L E S C E;
CONCAT                      : C O N C A T;
COUNT                       : C O U N T;
CURRENT_DATE                : C U R R E N T '_' D A T E;
CURRENT_TIME                : C U R R E N T '_' T I M E;
CURRENT_TIMESTAMP           : C U R R E N T '_' T I M E S T A M P;
DATE                        : D A T E;
DATETIME                    : D A T E T I M E ;
DELETE                      : D E L E T E;
DESC                        : D E S C;
DISTINCT                    : D I S T I N C T;
END                         : E N D;
ELSE                        : E L S E;
EMPTY                       : E M P T Y;
ENTRY                       : E N T R Y;
ESCAPE                      : E S C A P E;
EXISTS                      : E X I S T S;
EXP                         : E X P;
EXTRACT                     : E X T R A C T;
FALSE                       : F A L S E;
FETCH                       : F E T C H;
FLOOR                       : F L O O R;
FROM                        : F R O M;
FUNCTION                    : F U N C T I O N;
GROUP                       : G R O U P;
HAVING                      : H A V I N G;
IN                          : I N;
INDEX                       : I N D E X;
INNER                       : I N N E R;
IS                          : I S;
JOIN                        : J O I N;
KEY                         : K E Y;
LEADING                     : L E A D I N G;
LEFT                        : L E F T;
LENGTH                      : L E N G T H;
LIKE                        : L I K E;
LN                          : L N;
LOCAL                       : L O C A L;
LOCATE                      : L O C A T E;
LOWER                       : L O W E R;
MAX                         : M A X;
MEMBER                      : M E M B E R;
MIN                         : M I N;
MOD                         : M O D;
NEW                         : N E W;
NOT                         : N O T;
NULL                        : N U L L;
NULLIF                      : N U L L I F;
OBJECT                      : O B J E C T;
OF                          : O F;
ON                          : O N;
OR                          : O R;
ORDER                       : O R D E R;
OUTER                       : O U T E R;
POWER                       : P O W E R;
ROUND                       : R O U N D;
SELECT                      : S E L E C T;
SET                         : S E T;
SIGN                        : S I G N;
SIZE                        : S I Z E;
SOME                        : S O M E;
SQRT                        : S Q R T;
SUBSTRING                   : S U B S T R I N G;
SUM                         : S U M;
THEN                        : T H E N;
TIME                        : T I M E;
TRAILING                    : T R A I L I N G;
TREAT                       : T R E A T;
TRIM                        : T R I M;
TRUE                        : T R U E;
TYPE                        : T Y P E;
UPDATE                      : U P D A T E;
UPPER                       : U P P E R;
VALUE                       : V A L U E;
WHEN                        : W H E N;
WHERE                       : W H E R E;


CHARACTER                   : '\'' (~ ('\'' | '\\')) '\'' ;
IDENTIFICATION_VARIABLE     : ('a' .. 'z' | 'A' .. 'Z' | '\u0080' .. '\ufffe' | '$' | '_') ('a' .. 'z' | 'A' .. 'Z' | '\u0080' .. '\ufffe' | '0' .. '9' | '$' | '_')* ;
STRINGLITERAL               : '\'' (~ ('\'' | '\\'))* '\'' ;
FLOATLITERAL                : ('0' .. '9')* '.' ('0' .. '9')+ (E '0' .. '9')* ;
INTLITERAL                  : ('0' .. '9')+ ;
