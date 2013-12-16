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
// SilkParserTest.scala
// Since: 2012/08/10 16:05
//
//--------------------------------------

/**
 * @author leo
 */
class SilkParserTest extends SilkTextSpec {

  import SilkSample._

  def p(silk:String) {
    debug(s"parsing $silk")
    val r = SilkGrammar.parse("silk", silk)
    if(r.isLeft) {
      fail(s"parse error:\n$silk")
    }
  }

  def pe(silk:String) = {
    val r = SilkGrammar.parse("silk", silk)
    r
  }


  "SilkParser" should {
    "parse preambles" taggedAs("preamble") in {
      p("""%silk - version:1.0""")
      p("""%silk(version:1.0)""")
      p("""%silk - version:1.0, encoding:utf-8""")
    }

    "parse records" in {
      p(r0)
      p(r1)
    }

    "report errors" in {
      pe("""%silk version:2.0""") shouldBe 'left
    }

    "parse nodes" taggedAs("node") in {
      p("-person")
      p("-log.debug")
      p("-person - id:1, name:leo")


    }

  }
}