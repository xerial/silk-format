Silk Text Format
====

Silk text format is a compact columnar data format for structured data.

## Specification  (draft)

### Comment-line

Starts with `#`

```
# This is a comment line
```

### Preambles
Preamble line starts with `%`:

#### Header 
Specifies a silk format version.
```
%silk - version:1.0
```
#### Record schema definition

```
# Detailed definition
%record person(id:int, name:string)

# Simplified record syntax. You can use `-` instead of wrapping parameters with parentheses. 
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
# string type can be omitted
%record person - id:int, name, phone*
```
   * Example: An array of double type

```
%record point - value:double[2]
-point
0 1
2 3
```

* Map type

```
%record property:map[string, int]
-property 
A	  0
B	  1
C	  2
```

 
* `json` 
  * Silk uses an extended JSON format that can use QName (alphabet and number characters with spaces) token instead of double-quoted `"(String)"` 
  * Embedding tags in a column

```
%record read - qname, flag:int, chr:alnum, start:int, score:int, cigar, mname, mstart:int, isize:int, qseq, qv, tag:json
-read
read_28833_29006_6945        99        chr20        28833        20	10M1D25M        =        28993        195	AGCTTAGCTAGCTACCTATATCTTGGTCTTGGCCG        <<<<<<<<<<<<<<<<<<<<<:<9/,&,22;;<<< {MF:130, Nm:1, H0:0, H1:0, RG:L1}
read_28701_28881_323b        147        chr20        28834        30	35M        =        28701        -168	ACCTATATCTTGGCCTTGGCCGATGCGGCCTTGCA        <<<<<;<<<<7;:<<<6;<<<<<<<<<<<<7<<<<	{MF:18, Nm:0, H0:1, H1:0, RG:L2}
```

* `optional`

### Line format

Silk represents a list of records using tab-separated format. The line starting with `-` represents a data type name to be described.

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


#### Embedding (extended) JSON in a column

```
%record log(date, level, param:json)

-log
2013-11-20	info	{message:"hello silk"}
2013-11-20	debug	{result:"success", elapsed time:12.3}
```

It is equivalent to write as follows:
```
-log
 -date:2013-11-20
 -level:info
 -param
  -message:hello silk
-log
 -date:2013-11-20
 -level:debug
 -param
  -result:success
  -elapsed time:12.3
```

#### Embedding an array into a column
```
%record check_sheet(pid:int, answer:boolean*)
-check_sheet
1	[true, true, false, false, true]
2	[true, false, false, true, true]
```

#### In-line representation of nested records

```
%record person(id:int, name, address:address)
%record address(address, phone, country)

-person
1	leo	["ABC Street", "XXX-XXXX", "Japan"]
2	yui	["YYY Town", "ZZZ-ZZZZ", "US"]
```

#### Polymorphic types

```
%record log - date, message
# Embedding log record parameters into info/error records
%record info: _:log
%record error - _:log, cause

-log
@info  2013-11-20	system started
@error 2013-11-20	error occurred	NullPointerException
```

When the line starts with `@`, the first column is a type description. The remaining part is tab-separated values of the collesponding type.

### Indentation format

A more human readable data description, suited to configuration files, data
descriptions, etc. that need to be edited by hand.

```
-person
 -id: leo
 -name: yui
```


### Import statment

When silk format becomes large, or you need to share the schema definition, use `import` to 
include another silk data.

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

Context line starting with `>` is a meta data for annotating or grouping records.

```
>server - name:sv01, type:server log
-log
@info 2013-11-20	system started
@info 2013-11-20	received a task request
@error 2013-11-20	error occurred	NullPointerException
>client - name:sv100, type:client log
-log
@info 2013-11-20	client has started
@info 2013-11-20	client sending a task to server
```

### Schema-less data

If no record name or schema is specified, silk parses each data line as a tab-separated record of json. 
```
host:127.0.0.1  ident:- user:frank  time:"[10/Oct/2000:13:55:36 -0700]" req:"GET /apache_pb.gif HTTP/1.0" status:"200"  size:2326 referer:"http://www.example.com/start.html" ua:"Mozilla/4.08 [en] (Win98; I ;Nav)"
```

If your schema of the data becomes stable, you should define a record schema to create compact records:

```
%record weblog - host, ident, user, time, req, status, size:int, referer, ua
-weblog
127.0.0.1  -  frank  [10/Oct/2000:13:55:36 -0700] GET /apache_pb.gif HTTP/1.0 200 2326  http://www.example.com/start.html Mozilla/4.08 [en] (Win98; I ;Nav)
```

You can split the above weblog into schema and log record files:

`weblog.silk`
```
%record weblog - host, ident, user, time, req, status, size:int, referer, ua
-weblog
%import "weblog.tsv"
```

`weblog.tsv`
```
127.0.0.1  -  frank  [10/Oct/2000:13:55:36 -0700] GET /apache_pb.gif HTTP/1.0 200 2326  http://www.example.com/start.html Mozilla/4.08 [en] (Win98; I ;Nav)
```
