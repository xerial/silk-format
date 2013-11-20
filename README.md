Silk Text Format
====

Silk text format is a compact and flexible columnar data format.

## Specification 

### Schema definition

```
%silk version:1.0
%record person(id:int, name) 
%record log(date, level, json)
%record fruits table:map[string, string]
```

#### Primitive data types

If no type name is given in a record, the default data type becomes `string`.

* string (UTF8 encoding)
* int
* float
* double
* boolean


### Line format

Silk represents a record using tab-separated format. 

```
-person
1	leo
2	yui
```

```
-fruits table
A	apple
B	banana
C	coconut
```


#### Embedding JSON in a column

```
-log
2013-11-20	info	{"message":"hello silk"}
2013-11-20	debug	{"result":"success", "elapsed time":12.3}
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
%record 
```

```
%silk version:1.0
%import "record.silk"
```


