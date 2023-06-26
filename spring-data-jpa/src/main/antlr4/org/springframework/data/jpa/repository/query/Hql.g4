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

grammar Hql;

@header {
/**
 * HQL per https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#query-language
 *
 * This is a mixture of Hibernate's BNF and missing bits of grammar. There are gaps and inconsistencies in the
 * BNF itself, explained by other fragments of their spec. Additionally, alternate labels are used to provide easier
 * management of complex rules in the generated Visitor. Finally, there are labels applied to rule elements (op=('+'|'-')
 * to simplify the processing.
 *
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
    : selectStatement
    | updateStatement
    | deleteStatement
    | insertStatement
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-select
selectStatement
	: queryExpression
	;

queryExpression
	: withClause? orderedQuery (setOperator orderedQuery)*
	;

withClause
    : WITH cte (',' cte)*
    ;

cte
    : identifier AS (NOT? MATERIALIZED)? '(' queryExpression ')' searchClause? cycleClause?
    ;

searchClause
    : SEARCH (BREADTH | DEPTH) FIRST BY searchSpecifications SET identifier
    ;

searchSpecifications
    : searchSpecification (',' searchSpecification)*
    ;

searchSpecification
    : identifier sortDirection? nullsPrecedence?
    ;

cycleClause
    : CYCLE cteAttributes SET identifier (TO literal DEFAULT literal)? (USING identifier)?
    ;

cteAttributes
    : identifier (',' identifier)*
    ;

orderedQuery
	: (query | '(' queryExpression ')') queryOrder?
	;

query
	: selectClause fromClause? whereClause? (groupByClause havingClause?)? # SelectQuery
	| fromClause whereClause? (groupByClause havingClause?)? selectClause? # FromQuery
	;

queryOrder
	: orderByClause limitClause? offsetClause? fetchClause?
	;

fromClause
	: FROM entityWithJoins (',' entityWithJoins)*
	;

entityWithJoins
	: fromRoot (joinSpecifier)*
	;

joinSpecifier
    : join
    | crossJoin
    | jpaCollectionJoin
    ;

fromRoot
	: entityName variable?
	| LATERAL? '(' subquery ')' variable?
	;

join
	: joinType JOIN FETCH? joinTarget joinRestriction? // Spec BNF says joinType isn't optional, but text says that it is.
	;

joinTarget
	: path variable?                        # JoinPath
	| LATERAL? '(' subquery ')' variable?   # JoinSubquery
	;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-update
updateStatement
    : UPDATE VERSIONED? targetEntity setClause whereClause?
    ;

targetEntity
	: entityName variable?
	;

setClause
	: SET assignment (',' assignment)*
	;

assignment
    : simplePath '=' expressionOrPredicate
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-delete
deleteStatement
    : DELETE FROM? targetEntity whereClause?
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-insert
insertStatement
    : INSERT INTO? targetEntity targetFields (queryExpression | valuesList)
    ;

// Already defined underneath updateStatement
//targetEntity
//	: entityName variable?
//	;

targetFields
	: '(' simplePath (',' simplePath)* ')'
	;

valuesList
	: VALUES values (',' values)*
	;

values
	: '(' expression (',' expression)* ')'
	;

instantiation
    : NEW instantiationTarget '(' instantiationArguments ')'
    ;

alias
    : AS? identifier // spec says IDENTIFIER but clearly does NOT mean a reserved word
    ;

groupedItem
    : identifier
    | INTEGER_LITERAL
    | expression
    ;

sortedItem
    : sortExpression sortDirection? nullsPrecedence?
    ;

sortExpression
    : identifier
    | INTEGER_LITERAL
    | expression
    ;

sortDirection
    : ASC
    | DESC
    ;

nullsPrecedence
    : NULLS (FIRST | LAST)
    ;

limitClause
    : LIMIT parameterOrIntegerLiteral
    ;

offsetClause
    : OFFSET parameterOrIntegerLiteral (ROW | ROWS)?
    ;

fetchClause
    : FETCH (FIRST | NEXT) (parameterOrIntegerLiteral | parameterOrNumberLiteral '%') (ROW | ROWS) (ONLY | WITH TIES)
    ;

/*******************
    Gaps in the spec.
 *******************/

subquery
    : queryExpression
    ;

selectClause
    : SELECT DISTINCT? selectionList
    ;

selectionList
    : selection (',' selection)*
    ;

selection
    : selectExpression variable?
    ;

selectExpression
    : instantiation
    | mapEntrySelection
    | jpaSelectObjectSyntax
    | expressionOrPredicate
    ;

mapEntrySelection
    : ENTRY '(' path ')'
    ;

/**
 * Deprecated syntax dating back to EJB-QL prior to EJB 3, required by JPA, never documented in Hibernate
 */
jpaSelectObjectSyntax
	: OBJECT '(' identifier ')'
	;

whereClause
    : WHERE predicate (',' predicate)*
    ;

joinType
    : INNER?
    | (LEFT | RIGHT | FULL)? OUTER?
    | CROSS
    ;

crossJoin
    : CROSS JOIN entityName variable?
    ;

joinRestriction
    : (ON | WITH) predicate
    ;

// Deprecated syntax dating back to EJB-QL prior to EJB 3, required by JPA, never documented in Hibernate
jpaCollectionJoin
    : ',' IN '(' path ')' variable?
    ;

groupByClause
    : GROUP BY groupedItem (',' groupedItem)*
    ;

orderByClause
    : ORDER BY sortedItem (',' sortedItem)*
    ;

havingClause
    : HAVING predicate (',' predicate)*
    ;

setOperator
    : UNION ALL?
    | INTERSECT ALL?
    | EXCEPT ALL?
    ;

// Literals
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-literals
literal
    : NULL
    | booleanLiteral
    | stringLiteral
    | numericLiteral
    | dateTimeLiteral
    | binaryLiteral
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-boolean-literals
booleanLiteral
    : TRUE
    | FALSE
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-string-literals
stringLiteral
    : STRINGLITERAL
    | JAVASTRINGLITERAL
    | CHARACTER
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-numeric-literals
numericLiteral
    : INTEGER_LITERAL
    | FLOAT_LITERAL
    | HEXLITERAL
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-datetime-literals
dateTimeLiteral
    : LOCAL_DATE
    | LOCAL_TIME
    | LOCAL_DATETIME
    | CURRENT_DATE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | OFFSET_DATETIME
    | (LOCAL | CURRENT) DATE
    | (LOCAL | CURRENT) TIME
    | (LOCAL | CURRENT | OFFSET) DATETIME
    | INSTANT
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-duration-literals
datetimeField
	: YEAR
	| MONTH
	| DAY
	| WEEK
	| QUARTER
	| HOUR
	| MINUTE
	| SECOND
	| NANOSECOND
	| EPOCH
	;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-binary-literals
binaryLiteral
    : BINARY_LITERAL
    | '{' HEXLITERAL (',' HEXLITERAL)*  '}'
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-enum-literals
// TBD

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-java-constants
// TBD

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-entity-name-literals
// TBD

// Expressions
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-expressions
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-concatenation
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-numeric-arithmetic
expression
    : '(' expression ')'                                            # GroupedExpression
    | '(' expressionOrPredicate (',' expressionOrPredicate)+ ')'    # TupleExpression
    | '(' subquery ')'                                              # SubqueryExpression
    | primaryExpression                                             # PlainPrimaryExpression
    | op=('+' | '-') numericLiteral                                 # SignedNumericLiteral
    | op=('+' | '-') expression                                     # SignedExpression
    | expression datetimeField                                      # ToDurationExpression
    | expression BY datetimeField                                   # FromDurationExpression
    | expression op=('*' | '/') expression                          # MultiplicationExpression
    | expression op=('+' | '-') expression                          # AdditionExpression
    | expression '||' expression                                    # HqlConcatenationExpression
    ;

primaryExpression
    : caseList                                  # CaseExpression
    | literal                                   # LiteralExpression
    | parameter                                 # ParameterExpression
    | function                                  # FunctionExpression
    | generalPathFragment                       # GeneralPathExpression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-Datetime-arithmetic
// TBD

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-path-expressions
identificationVariable
    : identifier
    | simplePath
    ;

path
    : treatedPath pathContinutation?
    | generalPathFragment
    ;

generalPathFragment
    : simplePath indexedPathAccessFragment?
    ;

indexedPathAccessFragment
	: '[' expression ']' ('.' generalPathFragment)?
	;

simplePath
    : identifier simplePathElement*
    ;

simplePathElement
    : '.' identifier
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-case-expressions
caseList
    : simpleCaseExpression
    | searchedCaseExpression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-simple-case-expressions
simpleCaseExpression
    : CASE expressionOrPredicate caseWhenExpressionClause+ (ELSE expressionOrPredicate)? END
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-searched-case-expressions
searchedCaseExpression
    : CASE caseWhenPredicateClause+ (ELSE expressionOrPredicate)? END
    ;

caseWhenExpressionClause
    : WHEN expression THEN expressionOrPredicate
    ;

caseWhenPredicateClause
    : WHEN predicate THEN expressionOrPredicate
    ;

// Functions
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-exp-functions
function
    : functionName '(' (functionArguments | ASTERISK)?  ')' pathContinutation? filterClause? withinGroup? overClause? # GenericFunction
    | functionName '(' subquery ')'                                                                       # FunctionWithSubquery
    | castFunction                                                                                        # CastFunctionInvocation
    | extractFunction                                                                                     # ExtractFunctionInvocation
    | trimFunction                                                                                        # TrimFunctionInvocation
    | everyFunction                                                                                       # EveryFunctionInvocation
    | anyFunction                                                                                         # AnyFunctionInvocation
    | treatedPath                                                                                         # TreatedPathInvocation
    ;

functionArguments
    : DISTINCT?  expressionOrPredicate (',' expressionOrPredicate)*
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-aggregate-functions-filter
filterClause
    : FILTER '(' whereClause ')'
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-aggregate-functions-orderedset
withinGroup
    : WITHIN GROUP '(' orderByClause ')'
    ;

overClause
    : OVER '(' partitionClause? orderByClause? frameClause? ')'
    ;

partitionClause
    : PARTITION BY expression (',' expression)*
    ;

frameClause
    : (RANGE|ROWS|GROUPS) frameStart frameExclusion?
    | (RANGE|ROWS|GROUPS) BETWEEN frameStart AND frameEnd frameExclusion?
    ;

frameStart
	: UNBOUNDED PRECEDING   # UnboundedPrecedingFrameStart
	| expression PRECEDING  # ExpressionPrecedingFrameStart
	| CURRENT ROW           # CurrentRowFrameStart
	| expression FOLLOWING  # ExpressionFollowingFrameStart
	;

frameExclusion
	: EXCLUDE CURRENT ROW   # CurrentRowFrameExclusion
	| EXCLUDE GROUP         # GroupFrameExclusion
	| EXCLUDE TIES          # TiesFrameExclusion
	| EXCLUDE NO OTHERS     # NoOthersFrameExclusion
    ;

frameEnd
	: expression PRECEDING  # ExpressionPrecedingFrameEnd
	| CURRENT ROW           # CurrentRowFrameEnd
	| expression FOLLOWING  # ExpressionFollowingFrameEnd
	| UNBOUNDED FOLLOWING   # UnboundedFollowingFrameEnd
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-functions
castFunction
    : CAST '(' expression AS castTarget ')'
    ;

castTarget
	: castTargetType ('(' INTEGER_LITERAL (',' INTEGER_LITERAL)? ')')?
	;

castTargetType
	returns [String fullTargetName]
	: (i=identifier { $fullTargetName = _localctx.i.getText(); }) ('.' c=identifier { $fullTargetName += ("." + _localctx.c.getText() ); })*
	;

extractFunction
    : EXTRACT '(' expression FROM expression ')'
    | dateTimeFunction '(' expression ')'
    ;

trimFunction
    : TRIM '(' (LEADING | TRAILING | BOTH)? stringLiteral? FROM? expression ')'
    ;

dateTimeFunction
    : d=(YEAR
    | MONTH
    | DAY
    | WEEK
    | QUARTER
    | HOUR
    | MINUTE
    | SECOND
    | NANOSECOND
    | EPOCH)
    ;

everyFunction
    : every=(EVERY | ALL) '(' predicate ')'
    | every=(EVERY | ALL) '(' subquery ')'
    | every=(EVERY | ALL) (ELEMENTS | INDICES) '(' simplePath ')'
    ;

anyFunction
    : any=(ANY | SOME) '(' predicate ')'
    | any=(ANY | SOME) '(' subquery ')'
    | any=(ANY | SOME) (ELEMENTS | INDICES) '(' simplePath ')'
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-treat-type
treatedPath
    : TREAT '(' path AS simplePath')' pathContinutation?
    ;

pathContinutation
    : '.' simplePath
    ;

// Predicates
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-conditional-expressions
predicate
    : '(' predicate ')'                     # GroupedPredicate
    | dealingWithNullExpression             # NullExpressionPredicate
    | inExpression                          # InPredicate
    | betweenExpression                     # BetweenPredicate
    | relationalExpression                  # RelationalPredicate
    | stringPatternMatching                 # LikePredicate
    | existsExpression                      # ExistsPredicate
    | collectionExpression                  # CollectionPredicate
    | NOT predicate                         # NotPredicate
    | predicate AND predicate               # AndPredicate
    | predicate OR predicate                # OrPredicate
    | expression                            # ExpressionPredicate
    ;

expressionOrPredicate
    : expression
    | predicate
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-relational-comparisons
// NOTE: The TIP shows that "!=" is also supported. Hibernate's source code shows that "^=" is another NOT_EQUALS option as well.
relationalExpression
    : expression op=('=' | '>' | '>=' | '<' | '<=' | '<>' | '!=' | '^=' ) expression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-between-predicate
betweenExpression
    : expression NOT? BETWEEN expression AND expression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-null-predicate
dealingWithNullExpression
    : expression IS NOT? NULL
    | expression IS NOT? DISTINCT FROM expression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-like-predicate
stringPatternMatching
    : expression NOT? (LIKE | ILIKE) expression (ESCAPE (stringLiteral|parameter))?
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-elements-indices
// TBD

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-in-predicate
inExpression
    : expression NOT? IN inList
    ;

inList
	: (ELEMENTS | INDICES) '(' simplePath ')'
	| '(' subquery ')'
	| parameter
	| '(' (expressionOrPredicate (',' expressionOrPredicate)*)? ')'
	;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-exists-predicate
existsExpression
    : EXISTS (ELEMENTS | INDICES) '(' simplePath ')'
    | EXISTS expression
    ;

// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-collection-operators
collectionExpression
    : expression IS NOT? EMPTY
    | expression NOT? MEMBER OF path
    ;

// Projection
// https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#hql-select-new
instantiationTarget
    : LIST
    | MAP
    | simplePath
    ;

instantiationArguments
    : instantiationArgument (',' instantiationArgument)*
    ;

instantiationArgument
	: (expressionOrPredicate | instantiation) variable?
	;

// Low level parsing rules

parameterOrIntegerLiteral
    : parameter
    | INTEGER_LITERAL
    ;

parameterOrNumberLiteral
    : parameter
    | numericLiteral
    ;

variable
    : AS identifier
    | reservedWord
    ;

parameter
    : prefix=':' identifier
    | prefix='?' INTEGER_LITERAL?
    ;

entityName
    : identifier ('.' identifier)*
    ;

identifier
    : reservedWord
    ;

character
    : CHARACTER
    ;

functionName
    : reservedWord
    ;

reservedWord
    : IDENTIFICATION_VARIABLE
	| f=(ALL
	| AND
	| ANY
	| AS
	| ASC
	| AVG
	| BETWEEN
	| BOTH
	| BREADTH
	| BY
	| CASE
	| CAST
	| COLLATE
	| COUNT
	| CROSS
	| CUBE
	| CURRENT
	| CURRENT_DATE
	| CURRENT_INSTANT
	| CURRENT_TIME
	| CURRENT_TIMESTAMP
	| CYCLE
	| DATE
	| DATETIME
	| DAY
	| DEFAULT
	| DELETE
	| DEPTH
	| DESC
	| DISTINCT
	| ELEMENT
	| ELEMENTS
	| ELSE
	| EMPTY
	| END
	| ENTRY
	| EPOCH
	| ERROR
	| ESCAPE
	| EVERY
	| EXCEPT
	| EXCLUDE
	| EXISTS
	| EXTRACT
	| FETCH
	| FILTER
	| FIRST
	| FLOOR
	| FOLLOWING
	| FOR
	| FORMAT
	| FROM
	| FULL
	| FUNCTION
	| GROUP
	| GROUPS
	| HAVING
	| HOUR
	| ID
	| IGNORE
	| ILIKE
	| IN
	| INDEX
	| INDICES
	| INNER
	| INSERT
	| INSTANT
	| INTERSECT
	| INTO
	| IS
	| JOIN
	| KEY
	| LAST
	| LEADING
	| LEFT
	| LIKE
	| LIMIT
	| LIST
	| LISTAGG
	| LOCAL
	| LOCAL_DATE
	| LOCAL_DATETIME
	| LOCAL_TIME
	| MAP
	| MATERIALIZED
	| MAX
	| MAXELEMENT
	| MAXINDEX
	| MEMBER
	| MICROSECOND
	| MILLISECOND
	| MIN
	| MINELEMENT
	| MININDEX
	| MINUTE
	| MONTH
	| NANOSECOND
	| NATURALID
	| NEW
	| NEXT
	| NO
	| NOT
	| NULLS
	| OBJECT
	| OF
	| OFFSET
	| OFFSET_DATETIME
	| ON
	| ONLY
	| OR
	| ORDER
	| OTHERS
	| OUTER
	| OVER
	| OVERFLOW
	| OVERLAY
	| PAD
	| PARTITION
	| PERCENT
	| PLACING
	| POSITION
	| PRECEDING
	| QUARTER
	| RANGE
	| RESPECT
	| RIGHT
	| ROLLUP
	| ROW
	| ROWS
	| SEARCH
	| SECOND
	| SELECT
	| SET
	| SIZE
	| SOME
	| SUBSTRING
	| SUM
	| THEN
	| TIES
	| TIME
	| TIMESTAMP
	| TIMEZONE_HOUR
	| TIMEZONE_MINUTE
	| TO
	| TRAILING
	| TREAT
	| TRIM
	| TRUNC
	| TRUNCATE
	| TYPE
	| UNBOUNDED
	| UNION
	| UPDATE
	| USING
	| VALUE
	| VALUES
	| VERSION
	| VERSIONED
	| WEEK
	| WHEN
	| WHERE
	| WITH
	| WITHIN
	| WITHOUT
	| YEAR)
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

ALL                         : A L L;
AND                         : A N D;
ANY                         : A N Y;
AS                          : A S;
ASC                         : A S C;
ASTERISK                    : '*';
AVG                         : A V G;
BETWEEN                     : B E T W E E N;
BOTH                        : B O T H;
BREADTH                     : B R E A D T H;
BY                          : B Y;
CASE                        : C A S E;
CAST                        : C A S T;
CEILING                     : C E I L I N G;
COLLATE                     : C O L L A T E;
COUNT                       : C O U N T;
CROSS                       : C R O S S;
CUBE                        : C U B E;
CURRENT                     : C U R R E N T;
CURRENT_DATE                : C U R R E N T '_' D A T E;
CURRENT_INSTANT             : C U R R E N T '_' I N S T A N T;
CURRENT_TIME                : C U R R E N T '_' T I M E;
CURRENT_TIMESTAMP           : C U R R E N T '_' T I M E S T A M P;
CYCLE                       : C Y C L E;
DATE                        : D A T E;
DATETIME                    : D A T E T I M E ;
DAY                         : D A Y;
DEFAULT                     : D E F A U L T;
DELETE                      : D E L E T E;
DEPTH                       : D E P T H;
DESC                        : D E S C;
DISTINCT                    : D I S T I N C T;
ELEMENT                     : E L E M E N T;
ELEMENTS                    : E L E M E N T S;
ELSE                        : E L S E;
EMPTY                       : E M P T Y;
END                         : E N D;
ENTRY                       : E N T R Y;
EPOCH                       : E P O C H;
ERROR                       : E R R O R;
ESCAPE                      : E S C A P E;
EVERY                       : E V E R Y;
EXCEPT                      : E X C E P T;
EXCLUDE                     : E X C L U D E;
EXISTS                      : E X I S T S;
EXP                         : E X P;
EXTRACT                     : E X T R A C T;
FALSE                       : F A L S E;
FETCH                       : F E T C H;
FILTER                      : F I L T E R;
FIRST                       : F I R S T;
FK                          : F K;
FLOOR                       : F L O O R;
FOLLOWING                   : F O L L O W I N G;
FOR                         : F O R;
FORMAT                      : F O R M A T;
FROM                        : F R O M;
FULL                        : F U L L;
FUNCTION                    : F U N C T I O N;
GROUP                       : G R O U P;
GROUPS                      : G R O U P S;
HAVING                      : H A V I N G;
HOUR                        : H O U R;
ID                          : I D;
IGNORE                      : I G N O R E;
ILIKE                       : I L I K E;
IN                          : I N;
INDEX                       : I N D E X;
INDICES                     : I N D I C E S;
INNER                       : I N N E R;
INSERT                      : I N S E R T;
INSTANT                     : I N S T A N T;
INTERSECT                   : I N T E R S E C T;
INTO                        : I N T O;
IS                          : I S;
JOIN                        : J O I N;
KEY                         : K E Y;
LAST                        : L A S T;
LATERAL                     : L A T E R A L;
LEADING                     : L E A D I N G;
LEFT                        : L E F T;
LIKE                        : L I K E;
LIMIT                       : L I M I T;
LIST                        : L I S T;
LISTAGG                     : L I S T A G G;
LN                          : L N;
LOCAL                       : L O C A L;
LOCAL_DATE                  : L O C A L '_' D A T E ;
LOCAL_DATETIME              : L O C A L '_' D A T E T I M E;
LOCAL_TIME                  : L O C A L '_' T I M E;
MAP                         : M A P;
MATERIALIZED                : M A T E R I A L I Z E D;
MAX                         : M A X;
MAXELEMENT                  : M A X E L E M E N T;
MAXINDEX                    : M A X I N D E X;
MEMBER                      : M E M B E R;
MICROSECOND                 : M I C R O S E C O N D;
MILLISECOND                 : M I L L I S E C O N D;
MIN                         : M I N;
MINELEMENT                  : M I N E L E M E N T;
MININDEX                    : M I N I N D E X;
MINUTE                      : M I N U T E;
MONTH                       : M O N T H;
NANOSECOND                  : N A N O S E C O N D;
NATURALID                   : N A T U R A L I D;
NEW                         : N E W;
NEXT                        : N E X T;
NO                          : N O;
NOT                         : N O T;
NULL                        : N U L L;
NULLS                       : N U L L S;
OBJECT                      : O B J E C T;
OF                          : O F;
OFFSET                      : O F F S E T;
OFFSET_DATETIME             : O F F S E T '_' D A T E T I M E;
ON                          : O N;
ONLY                        : O N L Y;
OR                          : O R;
ORDER                       : O R D E R;
OTHERS                      : O T H E R S;
OUTER                       : O U T E R;
OVER                        : O V E R;
OVERFLOW                    : O V E R F L O W;
OVERLAY                     : O V E R L A Y;
PAD                         : P A D;
PARTITION                   : P A R T I T I O N;
PERCENT                     : P E R C E N T;
PLACING                     : P L A C I N G;
POSITION                    : P O S I T I O N;
POWER                       : P O W E R;
PRECEDING                   : P R E C E D I N G;
QUARTER                     : Q U A R T E R;
RANGE                       : R A N G E;
RESPECT                     : R E S P E C T;
RIGHT                       : R I G H T;
ROLLUP                      : R O L L U P;
ROW                         : R O W;
ROWS                        : R O W S;
SEARCH                      : S E A R C H;
SECOND                      : S E C O N D;
SELECT                      : S E L E C T;
SET                         : S E T;
SIZE                        : S I Z E;
SOME                        : S O M E;
SUBSTRING                   : S U B S T R I N G;
SUM                         : S U M;
THEN                        : T H E N;
TIES                        : T I E S;
TIME                        : T I M E;
TIMESTAMP                   : T I M E S T A M P;
TIMEZONE_HOUR               : T I M E Z O N E '_' H O U R;
TIMEZONE_MINUTE             : T I M E Z O N E '_' M I N U T E;
TO                          : T O;
TRAILING                    : T R A I L I N G;
TREAT                       : T R E A T;
TRIM                        : T R I M;
TRUE                        : T R U E;
TRUNC                       : T R U N C;
TRUNCATE                    : T R U N C A T E;
TYPE                        : T Y P E;
UNBOUNDED                   : U N B O U N D E D;
UNION                       : U N I O N;
UPDATE                      : U P D A T E;
USING                       : U S I N G;
VALUE                       : V A L U E;
VALUES                      : V A L U E S;
VERSION                     : V E R S I O N;
VERSIONED                   : V E R S I O N E D;
WEEK                        : W E E K;
WHEN                        : W H E N;
WHERE                       : W H E R E;
WITH                        : W I T H;
WITHIN                      : W I T H I N;
WITHOUT                     : W I T H O U T;
YEAR                        : Y E A R;

fragment INTEGER_NUMBER     : ('0' .. '9')+ ;
fragment FLOAT_NUMBER       : INTEGER_NUMBER+ '.'? INTEGER_NUMBER* (E [+-]? INTEGER_NUMBER)? ;
fragment HEX_DIGIT          : [0-9a-fA-F];


CHARACTER                   : '\'' (~ ('\'' | '\\' )) '\'' ;
STRINGLITERAL               : '\'' ('\'' '\'' | ~('\''))* '\'' ;
JAVASTRINGLITERAL           : '"' ( ('\\' [btnfr"']) | ~('"'))* '"';
INTEGER_LITERAL             : INTEGER_NUMBER (L | B I)? ;
FLOAT_LITERAL               : FLOAT_NUMBER (D | F | B D)?;
HEXLITERAL                  : '0' X HEX_DIGIT+ ;
BINARY_LITERAL              : [xX] '\'' HEX_DIGIT+ '\''
                            | [xX] '"'  HEX_DIGIT+ '"'
                            ;

IDENTIFICATION_VARIABLE     : ('a' .. 'z' | 'A' .. 'Z' | '\u0080' .. '\ufffe' | '$' | '_') ('a' .. 'z' | 'A' .. 'Z' | '\u0080' .. '\ufffe' | '0' .. '9' | '$' | '_')* ;

