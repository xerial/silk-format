Silk Text Format
====

Silk text format is a compact and flexible columnar data format.
 
## Specification  (draft)

### Comment-line

Starts with `#`

```
# This is a comment line
```

### Preambles
Preamble line starts with `%`:

#### Header 
Specifies silk format version
```
%silk - version:1.0
```
#### Record schema definition

```
# Detailed definition
%record person(id:int, name:string)

# Simplified syntax. You can use `-` instead of wrapping with parentheses. 
# If type name is ommitted, the default is string type
%record person - id:int, name
```

#### Primitive data types

If no type is specified in a record definition, the default data type becomes `string`.

* `string` (UTF8 encoding)
* `int`
* `float`
* `double`
* `boolean`
* `array[A]` (fixed-length array of type A)

```
%record person - id:int, name, phone*
```

* `map[K, V]` (K -> V: key-value pair)
* `seq[A]` (sequence of data of type A)
* `json`

### Line format

Silk represents a record using tab-separated format. 

```
%record person(id:int, name) 

-person
1	leo
2	yui
```

```
%record fruit_table:map[string, string]

-fruit_table
A	apple
B	banana
C	coconut
```


#### Embedding JSON in a column

```
%record log(date, level, param:json)

-log
2013-11-20	info	{"message":"hello silk"}
2013-11-20	debug	{"result":"success", "elapsed time":12.3}
```

#### In-line representation of nested records
```
%record person(id:int, name, address:address)
%record address(address, phone, country)

-person
1	leo	(ABC Street, XXX-XXXX, Japan)
2	yui	(YYY Town, ZZZ-ZZZZ, US)
```

### Indantation format

A more human readable data description, suited to configuration files, data
descriptions, etc. 

```
-person
 -id: leo
 -name: yui
```


### Import statment

`record.silk`
```
%silk version:1.0
%record person - id:int, name
```

```
%import "record.silk"

-person
1	leo
2	yui
```


### Context line

Context line is a meta data for annotating or grouping records

```
> imported - type:Apache log, date:2013-11-20
```
