# Fix for CAST function syntax in EQL parser - GH-3863

## Issue Description
The EQL (EclipseLink Query Language) parser in Spring Data JPA currently doesn't properly support the standard JPA syntax for CAST functions:

```sql
SELECT i FROM Item i WHERE cast(i.date as date) <= cast(:currentDateTime as date)
```

When using this syntax with EclipseLink, it results in:
```
org.springframework.data.jpa.repository.query.BadJpqlGrammarException: At 1:39 and token 'as', no viable alternative at input 'select i from Item i where cast(i.date \*as date) <= cast(:currentDateTime as date)'; Bad EQL grammar \[select i from Item i where cast(i.date as date) <= cast(:currentDateTime as date)\]
```

## Fix
- Updated the EQL grammar to support the standard JPA CAST syntax with the `AS` keyword: `CAST(expression AS type)`
- Modified the `EqlQueryRenderer` to handle both the original EclipseLink syntax and the standard JPA syntax
- Added test cases to verify the fix

## JPA Spec Reference
The JPA specification defines the CAST function syntax as follows:
```
cast_expression ::= CAST(scalar_expression AS scalar_type)
```

This fix makes Spring Data JPA's EQL parser compliant with the standard as defined in the JPA specification. 