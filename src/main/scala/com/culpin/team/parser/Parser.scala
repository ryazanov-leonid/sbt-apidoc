package com.culpin.team.parser

import java.io.File

import com.culpin.team.core._
import com.culpin.team.util.Util
import sbt.Logger
import scala.util.matching.Regex

trait Parser {

  val name: String

  val regex: Regex

  val path: String

  protected def parse(input: String): List[Option[String]] = Parser.parse(regex)(input)

  def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block]
}

class ApiParser extends Parser {

  override val name = "api"

  override val regex = """^(?:(?:\{(.+?)\})?\s*)?(.+?)(?:\s+(.+?))?$""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val matches = parse(content)
    if (matches.isEmpty)
      None
    else {
      Some(Block(`type` = matches(0), url = matches(1), title = matches(2)))
    }
  }

}

class ApiDescriptionParser extends Parser {

  override val name = "apidescription"

  override val regex = """""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val description = Util.trim(content)
    if (description.isEmpty)
      None
    else
      Some(Block(description = Some(description)))
  }
}

class ApiErrorExampleParser extends ApiExampleParser {

  override val name = "apierrorexample"

  override val path: String = "local.error.examples"

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] = {
    val example = processBlock(content, source)
    Some(Block(error = Some(Error(List(example)))))
  }

}

class ApiExampleParser extends Parser {

  override val name = "apiexample"

  override val regex = """(@\w*)?(?:(?:\s*\{\s*([a-zA-Z0-9\.\/\\\[\]_-]+)\s*\}\s*)?\s*(.*)?)""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] = {
    val example = processBlock(content, source)
    Some(Block(examples = Some(List(example))))
  }

  protected def processBlock(content: String, source: Option[String]): Example = {
    val trimmedSource = Util.trim(content)

    val matches = parse(trimmedSource)
    val `type` = matches(1) orElse Some("json")
    val title = matches(2)

    val regexFollowingLines = """(?m)(^.*\s?)""".r
    val matches2 = Parser.parse(regexFollowingLines)(trimmedSource)
    val text = if (matches2.length <= 1) None
    else matches2.tail.foldLeft(Option("")) {
      case (acc, elem) =>
        acc.map(s => s + elem.getOrElse(""))
    }
    val example = Example(title, text.map(Util.unindent), `type`)
    example
  }
}

class ApiGroupParser extends Parser {

  override val name = "apigroup"

  override val regex = """(\s+)""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val group = Util.trim(content)
    if (group.isEmpty)
      None
    else
      Some(Block(group = Some(group.replaceAll("\\s+", "_"))))
  }
}

class ApiNameParser extends Parser {

  override val name = "apiname"

  override val regex = """(\s+)""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val name = Util.trim(content)
    if (name.isEmpty)
      None
    else
      Some(Block(name = Some(name.replaceAll("\\s+", "_"))))
  }
}

class ApiParamParser(val defaultGroup: String = "Parameter") extends Parser {

  override val name = "apiparam"

  override val regex = """^\s*(?:\(\s*(.+?)\s*\)\s*)?\s*(?:\{\s*([a-zA-Z0-9()#:\.\/\\\[\]_-]+)\s*(?:\{\s*(.+?)\s*\}\s*)?\s*(?:=\s*(.+?)(?=\s*\}\s*))?\s*\}\s*)?(\[?\s*([a-zA-Z0-9\.\/\\_-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|(.*?)(?:\s|\]|$)))?\s*\]?\s*)(.*)?$|@""".r

  override val path = ""

  val allowedValuesWithDoubleQuoteRegExp = """\"[^\"]*[^\"]\"""".r
  val allowedValuesWithQuoteRegExp = """\'[^\']*[^\']\'""".r
  val allowedValuesRegExp = """[^,\s]+""".r

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content);
    val contentNoLineBreak = Parser.replaceLineWithUnicode(c)
    val matches = parse(contentNoLineBreak)
    if (matches.isEmpty)
      None
    else {
      val allowedValues = matches(3)
      val av = if (allowedValues.isDefined) {
        val regex = allowedValues.get.charAt(0) match {
          case '\"' => allowedValuesWithDoubleQuoteRegExp
          case '\'' => allowedValuesWithQuoteRegExp
          case _ => allowedValuesRegExp
        }
        Parser.parse(regex)(allowedValues.get)
      } else Nil
      val group = matches(0).orElse(Some(defaultGroup))
      val description = matches(9).map(Parser.reverseUnicodeLinebreak(_))
      Some(Block(group = group, `type` = matches(1), size = matches(2), optional = matches(4).map(s => (s.charAt(0) == '[').toString),
        field = matches(5), defaultValue = matches(6).orElse(matches(7)).orElse(matches(8)), description = description))

    }

  }
}

class ApiSuccessParser extends ApiParamParser("Success 200") {

  override val name = "apisuccess"

  override val path: String = "local.use.success.fields." + defaultGroup //TODO improve

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] =
    super.parseBlock(content, source, messages)
}

class ApiSuccessExampleParser extends ApiExampleParser {

  override val name = "apisuccessexample"

  override val path: String = "local.sucsess.examples"

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] = {
    val example = processBlock(content, source)
    Some(Block(success = Some(Success(List(example)))))
  }

}

class ApiUseParser extends Parser {

  override val name = "apiuse"

  override val regex: Regex = new Regex("")

  override val path: String = "local.use"

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      Some(Block(name = Some(c)))
  }
}

class ApiVersionParser extends Parser {

  override val name = "apiversion"

  override val regex: Regex = new Regex("")

  override val path: String = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      //TODO validate c semver
      Some(Block(version = Some(c)))
  }
}

object Parser {

  def apply(sources: Seq[File], log: Logger): Seq[Seq[Block]] = {
    sources.map { s =>
      parseFile(s)
    }
  }

  def parseFile(file: File): Seq[Block] = {
    val rawBlocks = findBlocks(file)
    val elements = rawBlocks.map { b =>
      findElements(b)
    }
    parseBlockElement(elements, file.getName)
  }

  val parser = List(
    new ApiParser,
    new ApiDescriptionParser,
    new ApiNameParser,
    new ApiGroupParser,
    new ApiParamParser,
    new ApiSuccessExampleParser,
    new ApiSuccessParser,
    new ApiUseParser,
    new ApiVersionParser
  )

  def parseBlockElement(detectedElements: Seq[Seq[Element]], filename: String): Seq[Block] = {
    def isApiBlock(elements: Seq[Element]): Boolean = {
      val apiIgnore = elements.exists { elem =>
        val elementName = elem.name
        (elementName.length() >= 9 && elementName.substring(0, 9) == "apiignore")
      }
      val apiElem = elements.exists { elem =>
        val elementName = elem.name
        elementName.length() >= 3 && elementName.substring(0, 3) == "api"
      }
      !apiIgnore && apiElem
    }

    detectedElements.filter(isApiBlock).map { elements =>
      elements.foldLeft(Block()) {
        case (block, element) =>
          val parserMap = parser.map(p => (p.name, p)).toMap

          //TODO handle non existing parser
          val Some(elementParser) = parserMap.get(element.name)

          //TODO handle empty block
          val Some(values) = elementParser.parseBlock(element.content, Some(element.source))
          merge(block, values)
      }
    }
  }

  def merge(b1: Block, b2: Block): Block = {
    val `type` = b1.`type`.orElse(b2.`type`)
    val title = b1.title.orElse(b2.title)
    val name = b1.name.orElse(b2.name)
    val url = b1.url.orElse(b2.url)
    val group = b1.group.orElse(b2.group)
    val version = b1.version.orElse(b2.version)
    val description = b1.description.orElse(b2.description)
    val size = b1.size.orElse(b2.size)
    val optional = b1.optional.orElse(b2.optional)
    val field = b1.field.orElse(b2.field)
    val defaultValue = b1.defaultValue.orElse(b2.defaultValue)
    val success = (b1.success, b2.success) match {
      case (None, s2) => s2
      case (s1, None) => s1
      case (Some(s1), Some(s2)) => Some(Success(s1.examples ++ s2.examples))
    }
    val error = (b1.error, b2.error) match {
      case (None, e2) => e2
      case (e1, None) => e1
      case (Some(e1), Some(e2)) => Some(Error(e1.examples ++ e2.examples))
    }
    Block(`type`, title, name, url, group, version, description, success, size, optional, field, defaultValue, None, error)
  }

  /**
   * Determine Blocks
   */
  def findElements(block: String): Seq[Element] = {
    val blockUnicode = replaceLineWithUnicode(block)
    val elementRegex = """(?m)(@(\w*)\s?(.+?)(?=\uffff[\s\*]*@|$))""".r

    elementRegex.findAllIn(blockUnicode).matchData.map { m =>
      Element(m.group(1), m.group(2).toLowerCase, reverseUnicodeLinebreak(m.group(2)), reverseUnicodeLinebreak(m.group(3)))
    }.toSeq
  }

  /**
   * Find block in the file give as a parameter
   * @param file the input file
   * @return The list of blocks found
   */
  def findBlocks(file: File): Seq[String] = {
    val src: String = readFile(file)
    findBlocks(src)
  }

  def readFile(file: File): String = {
    val source = scala.io.Source.fromFile(file)
    val src = try source.mkString finally source.close()
    src
  }

  /**
   * Find block in the string give as a parameter
   * @param src the input string
   * @return The list of blocks found
   */
  def findBlocks(src: String): List[String] = {
    val srcNoLines = replaceLineWithUnicode(src)
    //TODO handle language

    val regexForFile = defaultLanguageBlockRegex

    val matches = parseAndFilterNullGroup(regexForFile)(srcNoLines)
    matches.map { block =>
      // Reverse Unicode Linebreaks
      val blockLine = reverseUnicodeLinebreak(block)
      //&   remove not needed ' * ' and tabs at the beginning
      inlineRegex.replaceAllIn(blockLine, "")
    }
  }

  /**
   * Look up for the match within the string
   * @param regex the regex to match against
   * @param input the input string
   * @return
   */
  def parse(regex: Regex)(input: String): List[Option[String]] = {
    val matchIt = regex.findAllIn(input).matchData
    val result = for {
      m <- matchIt
      i <- 1 to m.groupCount
    } yield m.group(i)
    result.map(Option(_)).toList
  }

  def parseAndFilterNullGroup(regex: Regex)(input: String): List[String] =
    for {
      maybeString <- parse(regex)(input) if (maybeString.isDefined)
    } yield maybeString.getOrElse("")

  val defaultLanguageBlockRegex = """\/\*\*\uffff?(.+?)\uffff?(?:\s*)?\*\/""".r

  //Regex Multiline
  //http://daily-scala.blogspot.co.uk/2010/01/regular-expression-2-rest-regex-class.html
  val inlineRegex = """(?m)^(\s*)?(\*)[ ]?""".r

  def reverseUnicodeLinebreak(block: String): String = {
    block.replaceAll("\uffff", "\n")
  }

  def replaceLineWithUnicode(src: String): String = {
    src.replaceAll("\n", "\uffff")
  }

}
