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

# Simplified syntax. You can use `-` instead of wrapping parameters with parentheses. 
# If the type name is ommitted, the default is string type
%record person - id:int, name
```

#### Primitive data types

If no type is specified in a record definition, the default data type becomes `string`.

* `string` (UTF8 encoding)
* `int`
* `float`
* `double`
* `boolean`
* Array type
```
%record person - id:int, name, phone:string*
# string tyep can be omitted
%record person - id:int, name, phone*

# Array of double type
```
%record point - value:double[2]
```
* Map type
```
%record property:map[string, int]
-property 
A	  0
B	  1
C	  2
```
* Stream type. 
  * Uses when the length of array is large
```
%record read - qname, flag:int, chr:alnum, start:int, score:int, cigar, mname, mstart:int, isize:int, qseq, qv, tag:_
-read
read_28833_29006_6945        99        chr20        28833        20	10M1D25M        =        28993        195	AGCTTAGCTAGCTACCTATATCTTGGTCTTGGCCG        <<<<<<<<<<<<<<<<<<<<<:<9/,&,22;;<<< {MF:130, Nm:1, H0:0, H1:0, RG:L1}
read_28701_28881_323b        147        chr20        28834        30	35M        =        28701        -168	ACCTATATCTTGGCCTTGGCCGATGCGGCCTTGCA        <<<<<;<<<<7;:<<<6;<<<<<<<<<<<<7<<<<	{MF:18, Nm:0, H0:1, H1:0, RG:L2}
```
 
* `json`
* `optional`

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
