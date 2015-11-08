package com.culpin.team.filter

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

trait Filter {
  def postFilter(parsedFiles: JArray, tagName: String = ""): JArray
}

class ApiErrorFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, tagName: String = "error"): JArray =
    super.postFilter(parsedFiles, "error")
}

class ApiHeaderFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, tagName: String = "header"): JArray =
    super.postFilter(parsedFiles, "header")
}

class ApiSuccessFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, tagName: String = "success"): JArray =
    super.postFilter(parsedFiles, "success")
}

class ApiParamFilter extends Filter {

  def postFilter(parsedFiles: JArray, tagName: String = "parameter"): JArray = {
    parsedFiles.arr.map { parsedFileArray =>
      val JArray(parsedFile) = parsedFileArray
      parsedFile.map { block =>
        if (block \ "local" \ tagName \ "fields" == JNothing) block
        else {
          val JObject(bf) = block \ "local" \ tagName \ "fields"
          block.replace(List("local", tagName, "fields"), Filter.filterDuplicateKeys(JObject(bf)))
        }
      }
    }
  }

}

object Filter {

  def filterDuplicateKeys(jobject: JObject): JObject = {
    val obj: List[(String, JValue)] = jobject.obj
      .groupBy { case (key, value) => key }
      .mapValues(_.head)
      .valuesIterator.toList

    JObject(obj)
  }

  val filters = List(
    new ApiErrorFilter,
    new ApiHeaderFilter,
    new ApiParamFilter,
    new ApiSuccessFilter
  )

  def apply(parsedFiles: JArray): JArray = {
    val filteredFiles = filters.foldLeft(parsedFiles) {
      case (pf, filter) =>
        filter.postFilter(pf)
    }
    val res = filteredFiles.arr.flatMap {
      case JArray(parsedFile) =>
        parsedFile
          .collect {
            case block if (block \ "global").children.isEmpty && (block \ "local").children.nonEmpty =>
              block \ "local"
          }
      case _ => List()
    }
    JArray(res)
  }

}