//--------------------------------------
//
// Grammar.scala
// Since: 2013/11/20 10:46 AM
//
//--------------------------------------

package xerial.silk.text

import xerial.core.log.Logger
import scala.annotation.tailrec


object Grammar extends Logger {
  sealed abstract class ParseError extends Exception
  case class SyntaxError(posInLine: Int, message: String) extends ParseError
  case object NoMatch extends ParseError
  type ParseResult = Either[ParseError, Parser]

  trait Parser {
    def LA1: SilkToken
    def consume: Parser
    def getRule(name:String) : Tree
    def firstTokenOf(tree:Tree) : Seq[TokenType]
  }


  sealed abstract class Tree(val name:String) { a : Tree =>
    def ~(b: Tree) : Tree = SeqNode(Array(a, b))
    def |(b: Tree) : Tree = OrNode(Array(a, b))
    def eval(in:Parser) : ParseResult
    override def toString = name
  }

  case class TreeRef(override val name:String) extends Tree(name) {
    def eval(in: Parser) = {
      trace(s"eval $name")
      val t = in.getRule(name)
      t.eval(in)
    }
  }

  case class Leaf(tt: TokenType) extends Tree(tt.name) {
    def eval(in: Parser) = {
      val t = in.LA1
      trace(s"eval $tt, LA1:$t")
      if (t.tokenType == tt) {
        debug(s"match LA1:$t")
        Right(in.consume)
      }
      else
        Left(NoMatch)
    }
  }


  case class OrNode(seq:Array[Tree]) extends Tree(seq.map(_.name).mkString(" | ")) {
    override def |(b: Tree) : Tree = OrNode(seq :+ b)

    var table : Map[TokenType, Array[Tree]] = null

    private def lookupTable(p:Parser) : Map[TokenType, Array[Tree]] = {
      if(table == null) {
        val tokenToTree = for((tree, index) <- seq.zipWithIndex; tt <- p.firstTokenOf(tree)) yield tt -> index
        val entries = for((token, pairSeq) <- tokenToTree.groupBy(_._1)) yield {
          val indexes = pairSeq.map(_._2).distinct.sorted.map(seq(_)).toArray
          token -> indexes
        }
        table = entries.toMap
      }
      table
    }

    def eval(in: Parser) = {
      @tailrec def loop(i:Int, lst:Array[Tree], p:Parser) : ParseResult = {
        if(i >= lst.length)
          Right(p)
        else {
          lst(i).eval(p) match {
            case Left(NoMatch) => loop(i+1, lst, p)
            case other => other
          }
        }
      }

      trace(s"eval $name")
      val t = in.LA1
      loop(0, lookupTable(in).getOrElse(t.tokenType, seq), in)
    }

  }

  case class SeqNode(seq:Array[Tree]) extends Tree(seq.map(_.name).mkString(" ")) {
    override def ~(b: Tree) : Tree = SeqNode(seq :+ b)
    def eval(in:Parser) = {
      @tailrec def loop(i:Int, p:Parser) : ParseResult = {
        if(i >= seq.length)
          Right(p)
        else {
          seq(i).eval(in) match {
            case l@Left(_) => l
            case Right(next) => loop(i+1, next)
          }
        }
      }
      loop(0, in)
    }
  }

  case class ZeroOrMore(a: Tree) extends Tree("(%s)*".format(a.name)) {
    def eval(in:Parser) = {
      @tailrec def loop(p: Parser): ParseResult = {
        a.eval(p) match {
          case Left(NoMatch) => Right(p)
          case l@Left(_) => l
          case Right(next) => loop(next)
        }
      }
      loop(in)
    }
  }
  case class OptionNode(a: Tree) extends Tree("(%s)?".format(a.name)) {
    def eval(in: Parser) = {
      a.eval(in) match {
        case l@Left(NoMatch) => Right(in)
        case other => other
      }
    }
  }


  case class Repeat(a:Tree, separator:TokenType) extends Tree("rep(%s,%s)".format(a.name, separator.name)) {
    private val p = OptionNode(a ~ ZeroOrMore(Leaf(separator) ~ a))
    def eval(in: Parser) = p.eval(in)
  }


}


trait Grammar extends Logger {

  import Grammar._

  protected class GrammarParser(token: TokenStream) extends Grammar.Parser {
    def LA1 = token.LA(1)
    def consume = {
      token.consume
      this
    }
    def getRule(name: String) = ruleOf(name)
    def firstTokenOf(tree:Tree) = firstTokenListOf(tree)
  }

  def parse(ruleName:String, silk:String) = {
    val p = new GrammarParser(SilkLexer.tokenStream(silk))
    val r = TreeRef(ruleName).eval(p)
    if(r.isRight && p.LA1.tokenType != Token.EOF) {
      // Report errors when non-consumed tokens exists
      Left(NoMatch)
    }
    else
      r
  }

  def repeat(expr: Tree, separator: TokenType): Tree = Repeat(expr, separator)
  def oneOrMore(expr: Tree, separator: TokenType) : Tree = (expr ~ ZeroOrMore(Leaf(separator) ~ expr))
  def option(expr: Tree): Tree = OptionNode(expr)

  import scala.language.implicitConversions

  protected implicit def toLeaf(tt:TokenType) : Tree = Leaf(tt)
  protected implicit def toRule(s:String) = new Rule(s)
  protected implicit def toRef(s:String) = new TreeRef(s)

  class Rule(name:String) {
    def :=(t:Tree) : TreeRef = {
      trace(s"new rule $name := $t")
      ruleTable += name -> t
      TreeRef(name)
    }
  }

  private val ruleTable = collection.mutable.Map[String, Tree]()
  def ruleOf(name:String) : Tree = ruleTable(name)

  /**
   * Returns token types that can activate the tree rule
   * @param tree
   * @return
   */
  def firstTokenListOf(tree:Tree) : Seq[TokenType] = {
    def firstTokenOf(t:Tree, foundRefs:Set[String]) : Seq[TokenType] = {
      t match {
        case TreeRef(ref) =>
          if(foundRefs.contains(ref))
            Seq.empty
          else
            firstTokenOf(ruleOf(ref), foundRefs + ref)
        case OrNode(exprs) => exprs.flatMap(expr => firstTokenOf(expr, foundRefs))
        case SeqNode(exprs) => firstTokenOf(exprs.head, foundRefs)
        case ZeroOrMore(a) => firstTokenOf(a, foundRefs)
        case OptionNode(a) => firstTokenOf(a, foundRefs)
        case Repeat(a, seq) => firstTokenOf(a, foundRefs)
        case Leaf(tokenType) => Seq(tokenType)
      }
    }

    firstTokenOf(tree, Set.empty[String])
  }

}

