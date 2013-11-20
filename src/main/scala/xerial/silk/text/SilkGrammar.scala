//--------------------------------------
//
// SilkGrammar.scala
// Since: 2013/11/20 10:42 AM
//
//--------------------------------------

package xerial.silk.text

import xerial.core.log.Logger


object SilkGrammar extends Grammar with Logger {
  import Token._

  // Silk grammar rules
  "expr" := String | LParen ~ "expr" ~ RParen
  "silk" := DataLine | "node" | "preamble" | LineComment | BlankLine
  "preamble" := Preamble ~ QName ~ option(Name) ~ option("preambleParams")
  "preambleParams" := (Separator ~ repeat("preambleParam", Comma)) | (LParen ~ repeat("preambleParam", Comma) ~ RParen)
  "preambleParam" := Name ~ option(Colon ~ "preambleParamValue")
  "preambleParamValue" := "value" | "typeName"
  "typeName" := QName ~ option(LSquare ~ oneOrMore(QName, Comma) ~ RSquare)
  "node" := option(Indent) ~ Hyphen ~ Name ~ option("nodeParams") ~ option("nodeParamSugar" | "nodeParams")
  "context" := option(Indent) ~ RSquare ~ Name ~ option("nodeParams") ~ option("nodeParamSugar" | "nodeParams")
  "nodeParamSugar" := Separator ~ repeat("param", Comma)
  "nodeParams" := LParen ~ repeat("param", Comma) ~ RParen ~ option(Colon ~ NodeValue)
  "param" := Name ~ option(Colon ~ "value")
  "value" := NodeValue | Name | QName | Token.String | Integer | Real | "boolean" | Null | "tuple"
  "tuple" := LParen ~ repeat("value", Comma) ~ RParen
  "boolean" := True | False

  // Extended JSON Grammar
  "json" := "jsonObject" | "jsonArray"
  "jsonValue" := "jsonObject" | "jsonArray" | String | Integer | Real | "boolean"
  "jsonArray" := LBracket ~ repeat("jsonValue", Comma) ~ RBracket
  "jsonObject" := LBrace ~ repeat("jsonKeyValue", Comma) ~ RBrace
  "jsonKeyValue" := Name ~ Colon ~ "jsonValue"

}
