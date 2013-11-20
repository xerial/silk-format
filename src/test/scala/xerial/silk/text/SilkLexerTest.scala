/*
 * Copyright 2012 Taro L. Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xerial.silk.text



//--------------------------------------
//
// SilkLexerTest.scala
// Since: 2012/01/24 11:05
//
//--------------------------------------

object SilkSample {


  val p0 = """%silk - version:2.0"""
  val p1 = """%silk - version:2.0, encoding:utf-8"""

  val e0 = """%silk version:2.0"""

  val r0 = """%record A(id:int)"""
  val r1 = """%record B(id:int)"""

}


/**
 * @author leo
 */
class SilkLexerTest extends SilkTextSpec {

  def parse(silk:String) = {
    val t = SilkLexer.parseLine(silk)
    debug(t.mkString("\n"))
  }
  def parseJson(json:String) = {
    val t = SilkLexer.parseJSON(json)
    debug(t.mkString("\n"))
  }


  "SilkLexer" should {
    "parse preamble" taggedAs("preamble") in {
      parse("""%silk - version:1.0""")
      parse("""%silk(version:1.0)""")
      parse("""%import - file:header.silk""")
      parse("""# comment line""")
      parse(""" # comment line. Arbitrary text can be described here ~@!-$#@*&(%!""")
    }


    "parse node" taggedAs("node") in {
      parse("""-person - id:0, name:leo""")
      parse("""  -person(id:0, name:"leo")""")
      parse("""  -log(message:"hello world") """)
      parse("""-score -point:0.234 """)
    }

    "parse record" taggedAs("record") in {
      parse("""%record person - id:int, name""")
      parse("""%record fasta - name, description, sequence:seq[string]""")
      parse("""%record embed - _:person, address:string*""")
    }

    "parse data line" taggedAs("line") in {
      parse("10\tABC")
      parse("@sam\t10\tABC")
    }

    "parse json" taggedAs("json") in {
      parseJson("{id:1, name:leo}")
      parseJson("[0, 1, 2]")
      parseJson("[true, false, true]")
      parseJson("""{id:1, name:leo, address:{city:Tokyo, country:"JP"}""")
    }


  }

}