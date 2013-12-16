/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// XerialJ
//
// SilkTokenScanner.java
// Since: 2011/04/29 22:53:43
//
//--------------------------------------
package xerial.silk.text

import xerial.core.log.Logger
import xerial.core.io.text.LineReader
import java.io.{Reader, InputStream}
import annotation.tailrec
import xerial.core.collection.CyclicArray


object SilkLexer {
  object INIT extends SilkLexerState
  object NODE_NAME extends SilkLexerState
  object NODE_VALUE extends SilkLexerState
  object ATTRIBUTE_NAME extends SilkLexerState
  object ATTRIBUTE_VALUE extends SilkLexerState
  object QNAME extends SilkLexerState
  object JSON extends SilkLexerState

  def parseLine(silk: CharSequence): IndexedSeq[SilkToken] = {
    val tokens = new SilkLineLexer(silk, INIT).scan
    tokens
  }

  def parseJSON(json: CharSequence): IndexedSeq[SilkToken] = {
    val tokens = new SilkLineLexer(json, JSON).scan
    tokens
  }

  def tokenStream(silk: CharSequence): TokenStream = {
    val tokens = new SilkLineLexer(silk, INIT).scan
    new TokenStream(tokens)
  }

}


sealed abstract class SilkLexerState() {
  override def toString = this.getClass.getSimpleName.replaceAll("\\$", "")
}

class TokenStream(token:IndexedSeq[SilkToken]) {
  private var index = 0

  def LA(k:Int) : SilkToken = {
    val i = index + k - 1
    if(i < token.length) token(i) else EOFToken
  }
  def consume {
    index += 1
  }
}



/**
 * Silk Token scanner
 *
 * @author leo
 *
 */
class SilkLexer(reader: LineReader) extends Logger {

  import xerial.silk.text.SilkLexer._

  def this(in: InputStream) = this(LineReader(in))
  def this(in: Reader) = this(LineReader(in))

  private val PREFETCH_SIZE = 10
  private var nProcessedLines = 0L
  private val tokenQueue = new CyclicArray[SilkToken]

  def close = reader.close

  /**
   * Look ahead k tokens. If there is no token at k, return null
   *
   * @param k
   * @return
   * @throws XerialException
   */
  def LA(k: Int): SilkToken = {
    if (k == 0)
      throw new IllegalArgumentException("k must be larger than 0");
    while (tokenQueue.size < k && !noMoreLine) {
      fill(PREFETCH_SIZE)
    }

    if (tokenQueue.size < k)
      null
    else
      tokenQueue.peekFirst(k - 1)
  }

  private def noMoreLine: Boolean = reader.reachedEOF

  /**
   * Read the next token
   *
   * @return next token or null if no more token is available
   * @throws XerialException
   */
  def next: SilkToken = {
    if (!tokenQueue.isEmpty)
      tokenQueue.pollFirst
    else if (noMoreLine)
      null
    else {
      fill(PREFETCH_SIZE);
      next
    }
  }

  def fill(prefetch_lines: Int) {
    // TODO line-based error recovery
    for (i <- 0 until prefetch_lines) {
      for (line <- reader.nextLine) {
        val lexer = new SilkLineLexer(line, INIT)
        val tokens = lexer.scan
        nProcessedLines += 1
        tokens foreach (tokenQueue.append(_))
      }
    }
  }

}


case class SilkParseError(posInLine:Int, message:String) extends Exception(message)

/**
 * @author leo
 */
class SilkLineLexer(line: CharSequence, initialState: SilkLexerState) extends Logger {

  import SilkLexer._

  private val scanner = LineReader(line)
  private var posInLine: Int = 0
  private var state = initialState
  private val tokenQueue = IndexedSeq.newBuilder[SilkToken]

  private def consume {
    scanner.consume
    posInLine += 1
  }

  private def emit(token: SilkToken): Unit = tokenQueue += token
  private def emit(t: TokenType): Unit = emit(Token(scanner.markStart, t))
  private def emit(tokenChar: Int): Unit = emit(Token.toSymbol(tokenChar))
  private def emitWithText(t: TokenType): Unit = emitWithText(t, scanner.selected)
  private def emitWithText(t: TokenType, text: CharSequence): Unit = emit(TextToken(scanner.markStart, t, text))
  private def emitTrimmed(t: TokenType): Unit = emitWithText(t, scanner.trimSelected)
  private def emitString(t: TokenType): Unit = emitWithText(t, scanner.selected(1))
  private def emitWholeLine(t: TokenType): Unit = emitWithText(t, scanner.selectedFromFirstMark)

  def scan: IndexedSeq[SilkToken] = {
    while (!scanner.reachedEOF) {
      scanner.resetMarks
      scanner.mark
      state match {
        case INIT => mInit
        case NODE_NAME => mToken
        case ATTRIBUTE_NAME => mToken
        case ATTRIBUTE_VALUE => mToken
        case NODE_VALUE => mNodeValue
        case QNAME => mQName; state = NODE_NAME
        case JSON => mJSON
      }
    }

    tokenQueue.result()
  }

  private def LA1 = scanner.LA(1)

  def mIndent {
    @tailrec def loop(len: Int): Int = LA1 match {
      case ' ' =>
        consume
        loop(len + 1)
      case '\t' =>
        consume
        // TAB is 4 white spaces
        loop(len + 4)
      case _ => len
    }
    val indentLen = loop(0)
    emit(IndentToken(posInLine, indentLen))
  }

  @inline def isWhiteSpace(c: Int) = c == ' ' || c == '\t'
  @inline def isDigit(ch: Int) = ch >= '0' && ch <= '9'
  @inline def isAlphabet(ch: Int) = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')

  def sUntilEOL = sUntil({
    c: Int => c != LineReader.EOF
  })
  def sWhiteSpace_s = sUntil(isWhiteSpace) // (' ' | '\t') *

  def sEscapeSequence {
    s('\\')
    LA1 match {
      case '"' => consume
      case '\\' => consume
      case '/' => consume
      case 'b' => consume
      case 'f' => consume
      case 'n' => consume
      case 'r' => consume
      case 't' => consume
      case 'u' => for (i <- 0 until 4) sHexDigit
      case _ => error(s"non escape sequence char: ${LA1.toChar}")
    }
  }

  def mJSON {
    skipWhiteSpaces
    val c = LA1
    def ok = { consume; emit(c) }
    c match {
      case '{' => ok
      case '}' => ok
      case '[' => ok
      case ']' => ok
      case ':' => ok
      case ',' => ok
      case _ => mValue
    }
  }

  def mValue {
    LA1 match {
      case '"' => mString
      case c if isDigit(c) => mNumber
      case '-' if isDigit(scanner.LA(2)) => mNumber
      case _ =>
        // Boolean, Null
        val booleanOrNull = sSymbol(Token.True) orElse
          sSymbol(Token.False) orElse
          sSymbol(Token.Null)
        booleanOrNull match {
          case Some(t) => emit(t)
          case None =>
            state match {
              case ATTRIBUTE_VALUE => mPName
              case _ => mName
            }
        }
    }

  }


  def sHexDigit {
    val c = LA1
    if (isDigit(c) || c >= 'A' && c <= 'F' || c >= 'a' && c >= 'f')
      consume
    else
      error("non hex digit char: %s".format(LA1.toChar))
  }

  def sDigit: Boolean =
    if (isDigit(LA1)) {
      consume
      true
    }
    else
      false

  def sDigit_s {
    while (sDigit) {}
  }

  def sDigit_p {
    if (sDigit) sDigit_s else error("non digit char: %s".format(LA1.toChar))
  }

  def sExp: Boolean = {
    val c = LA1
    if (c == 'e' || c == 'E') {
      consume
      val c2 = LA1
      if (c2 == '+' || c2 == '-') consume
      sDigit_p
      true
    }
    else
      false
  }

  def mNumber {
    var c = LA1
    if (c == '-') {
      // negative number
      val c2 = scanner.LA(2)
      if (isDigit(c2)) {
        consume
        c = c2
      }
    }

    if (c == '0') consume
    else if (c >= '1' && c <= '9') {
      consume
      sDigit_s
    }

    LA1 match {
      case '.' => consume; sDigit_p; sExp; emitWithText(Token.Real)
      case _ => if (sExp) emitWithText(Token.Real) else emitWithText(Token.Integer)
    }
  }

  def mString {
    s('"')
    @tailrec def loop {
      LA1 match {
        case '"' => consume
        case '\\' => sEscapeSequence; loop
        case LineReader.EOF => error(s"expected EOF but ${LA1.toChar} found")
        case _ => consume; loop
      }
    }
    loop
    emitString(Token.String)
  }

  def error(message:String): Nothing = throw new SilkParseError(posInLine+1, message)

  def s(expected: Int) {
    val c = scanner.LA(1)
    if (c != expected)
      throw error("expected %s but %s found".format(expected.toChar, c))
    else
      consume
  }

  def sSymbol(token:TokenSymbol) : Option[TokenSymbol] = {
    var cursor = 0
    val text = token.symbol
    while(cursor < text.length) {
      val expected = text.charAt(cursor)
      val c = scanner.LA(cursor+1)
      if(c != expected) {
        return None
      }
      cursor += 1
    }
    while(cursor > 0) {
      scanner.consume
      cursor -= 1
    }
    Some(token)
  }


  def skipWhiteSpaces {
    sWhiteSpace_s
    scanner.mark
  }


  def mInit: Unit = LA1 match {
    case ' ' => mIndent
    case '\t' => mIndent
    case '-' =>
      if (isDigit(scanner.LA(2))) {
        sUntilEOL
        emitWithText(Token.DataLine)
      }
      else {
        consume
        emit(Token.Hyphen)
        state = NODE_NAME
      }
    //    case '>' => consume; emit(Token.SeqNode); state = NODE_NAME
    case '#' => consume; sUntilEOL; emitWithText(Token.LineComment)
    case '%' => consume; emit(Token.Preamble); state = QNAME
    case '@' => sUntilEOL; emitWithText(Token.DataLineWithType)
    case LineReader.EOF => emit(Token.BlankLine)
    case '\\' =>
      val c2 = scanner.LA(2)
      if (c2 == '-') {
        consume
        scanner.mark
      } // escaped '-'
      sUntilEOL
      emitWithText(Token.DataLine)
    case _ =>
      sUntilEOL
      emitWithText(Token.DataLine)
  }

  def mToken: Unit = {

    def transitCh(ch: Int, nextState: SilkLexerState): Unit = transit(Token.toSymbol(ch), nextState)
    def transit(t: TokenSymbol, nextState: SilkLexerState): Unit = {
      consume
      emit(t)
      state = nextState
    }
    def noTransition(ch: Int): Unit = {
      consume
      emit(ch)
    }

    skipWhiteSpaces

    val c = LA1
    c match {
      case '(' => transitCh(c, ATTRIBUTE_NAME)
      case ')' => transitCh(c, ATTRIBUTE_NAME)
      case '-' =>
        state match {
          case NODE_NAME => transit(Token.Separator, ATTRIBUTE_NAME)
          case ATTRIBUTE_NAME => transit(Token.Separator, ATTRIBUTE_NAME)
          case _ => mName
        }
      case ':' =>
        state match {
          case NODE_NAME => transit(Token.Colon, NODE_VALUE)
          case ATTRIBUTE_NAME => transit(Token.Colon, ATTRIBUTE_VALUE)
          case _ => error("colon is not allowed in %s state".format(state))
        }
      case ',' =>
        state match {
          case ATTRIBUTE_VALUE => transit(Token.Comma, ATTRIBUTE_NAME)
          case _ => transit(Token.Comma, state)
        }
      case '<' => noTransition(c)
      case '>' => noTransition(c)
      case '[' => noTransition(c)
      case ']' => noTransition(c)
      case '{' => noTransition(c)
      case '}' => noTransition(c)
      case '?' => noTransition(c)
      case '*' => noTransition(c)
      case '+' =>
        consume
        state match {
          case ATTRIBUTE_VALUE => emitTrimmed(Token.NodeValue)
          case _ => emit(Token.Plus)
        }
      case LineReader.EOF =>
      case _ => mValue
    }
  }


  // qname first:  Alphabet | Dot | '_' | At | Sharp
  private def isQNameFirst(c: Int) = (c == '@' || c == '#' || c == '.' || c == '_' || isAlphabet(c))
  private def isQNameChar(c: Int) = (c == '.' || c == '_' || isAlphabet(c) || isDigit(c))
  private def isNameChar(c: Int) = c == ' '  || isQNameChar(c)
  private def isPNameChar(c: Int) = c == '-' || isNameChar(c)
  private def isValueChar(c:Int) = c != '(' && c != ')' && c != ',' && c != ':' && c != '@' && c != '#' && c != '"'

  def sQNameFirst = {
    if (isQNameFirst(LA1))
      consume
    else
      error(s"expected QName Char but ${LA1.toChar} found")
  }

  private def sUntil(cond: Int => Boolean) : Int = {
    var count = 0
    @tailrec def loop {
      val c = LA1
      if (c != LineReader.EOF && cond(c)) {
        count += 1
        consume
        loop
      }
    }
    loop
    count
  }


  def mQName {
    sQNameFirst
    val len = sUntil(isQNameChar)
    if(len == 0)
      error(s"invalid token: ${LA1.toChar} pos:${posInLine}")
    emitTrimmed(Token.QName)
  }

  def mName {
    val len = sUntil(isNameChar)
    if(len == 0)
      error(s"invalid token: ${LA1.toChar} pos:${posInLine}")
    emitTrimmed(Token.Name)
  }

  def mPName {
    val len = sUntil(isPNameChar)
    if(len == 0)
      error(s"invalid token: ${LA1.toChar} pos:${posInLine}")
    emitTrimmed(Token.PName)
  }


  def mNodeValue {
    skipWhiteSpaces
    sUntilEOL
    emitTrimmed(Token.NodeValue)
  }


}
