package com.culpin.team.worker

import java.io.File

import com.culpin.team.core.SbtApidocConfiguration
import com.culpin.team.parser.Parser
import com.culpin.team.util.Util
import org.json4s.JsonAST.JArray
import org.scalatest.{ Matchers, FlatSpec }

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class WorkerSpec extends FlatSpec with Matchers {

  val conf = SbtApidocConfiguration("name", "description", Option("https://api.github.com/v1"), "1.2")

  "ApiParamTitleWorker" should " preProcess parsed Files" in {
    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiParamTitleWorker
    val result = worker.preProcess(JArray(json), "defineErrorTitle")

    val defineErrorTitle = result \ "defineErrorTitle"
    val createUser = defineErrorTitle \ "CreateUserError" \ "0.2.0"
    assert(createUser \ "name" === JString("CreateUserError"))
    assert(createUser \ "title" === JString(""))
    assert(createUser \ "description" === JString(""))

    val admin_0_3 = defineErrorTitle \ "admin" \ "0.3.0"
    assert(admin_0_3 \ "name" === JString("admin"))
    assert(admin_0_3 \ "title" === JString("Admin access rights needed."))
    assert(admin_0_3 \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val admin_0_1 = defineErrorTitle \ "admin" \ "0.1.0"
    assert(admin_0_1 \ "name" === JString("admin"))
    assert(admin_0_1 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(admin_0_1 \ "description" === JString(""))

  }

  "ApiUseWorker" should " preProcess parsed Files" in {

    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiUseWorker
    val result = worker.preProcess(JArray(json), "defineErrorStructure")
    val structure = result \ "defineErrorStructure"
    assert(structure == JObject())
  }

  "ApiUseWorker" should " preProcess parsed Files 2" in {

    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiUseWorker
    val result = worker.preProcess(JArray(json))

    val createUser = result \ "define" \ "CreateUserError" \ "0.2.0"
    assert(createUser \ "version" === JString("0.2.0"))
    val errorField = createUser \ "error" \ "fields" \ "Error 4xx"

    val error1 = errorField.children(0)
    assert(error1 \ "group" === JString("Error 4xx"))
    assert(error1 \ "optional" === JString("false"))
    assert(error1 \ "field" === JString("NoAccessRight"))
    assert(error1 \ "description" === JString("Only authenticated Admins can access the data."))

    val error2 = errorField.children(1)
    assert(error2 \ "group" === JString("Error 4xx"))
    assert(error2 \ "optional" === JString("false"))
    assert(error2 \ "field" === JString("UserNameTooShort"))
    assert(error2 \ "description" === JString("Minimum of 5 characters required."))

    val examples = createUser \ "error" \ "examples"
    assert(examples \ "title" === JString("Response (example):"))
    assert(examples \ "content" === JString("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(examples \ "type" === JString("json"))

  }

  "ApiPermissionWorker" should " preProcess parsed Files" in {
    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiPermissionWorker
    val result = worker.preProcess(JArray(json))

    val defineErrorTitle = result \ "definePermission"
    val createUser = defineErrorTitle \ "CreateUserError" \ "0.2.0"
    assert(createUser \ "name" === JString("CreateUserError"))
    assert(createUser \ "title" === JString(""))
    assert(createUser \ "description" === JString(""))

    val admin_0_3 = defineErrorTitle \ "admin" \ "0.3.0"
    assert(admin_0_3 \ "name" === JString("admin"))
    assert(admin_0_3 \ "title" === JString("Admin access rights needed."))
    assert(admin_0_3 \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val admin_0_1 = defineErrorTitle \ "admin" \ "0.1.0"
    assert(admin_0_1 \ "name" === JString("admin"))
    assert(admin_0_1 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(admin_0_1 \ "description" === JString(""))

  }

  "Worker" should " process filename" in {

    val sources = List(new File(getClass.getResource("/_apidoc.js").getFile),
      new File(getClass.getResource("/full-example.js").getFile))

    val (json, filenames) = Parser(sources)

    val JArray(List(file1, file2)) = Worker.processFilename(json, filenames.toList)

    val JArray(blocks1) = file1

    assert(blocks1(3) \ "local" \ "filename" === JString("_apidoc.js"))
    assert(blocks1(4) \ "local" \ "filename" === JString("_apidoc.js"))
    assert(blocks1(5) \ "local" \ "filename" === JString("_apidoc.js"))

    val JArray(blocks2) = file2

    assert(blocks2(0) \ "local" \ "filename" === JString("full-example.js"))
    assert(blocks2(1) \ "local" \ "filename" === JString("full-example.js"))
    assert(blocks2(2) \ "local" \ "filename" === JString("full-example.js"))

  }

  "Worker" should " preProcess " in {

    def checkCreateUser(createUserField: JValue) = {
      assert(createUserField \ "name" === JString("CreateUserError"))
      assert(createUserField \ "title" === JString(""))
      assert(createUserField \ "description" === JString(""))
    }

    def checkAdmin3(admin0_3_0: JValue) = {
      assert(admin0_3_0 \ "name" === JString("admin"))
      assert(admin0_3_0 \ "title" === JString("Admin access rights needed."))
      assert(admin0_3_0 \ "description" === JString("""Optionallyyou can write here further Informations about the permission.An "apiDefinePermission"-block can have an "apiVersion", so you can attach the block to a specific version."""))
    }

    def checkAdmin1(admin0_1_0: JValue) = {
      assert(admin0_1_0 \ "name" === JString("admin"))
      assert(admin0_1_0 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
      assert(admin0_1_0 \ "description" === JString(""))
    }

    def checkDefinition(definition: JValue) {
      val createUserError0_2_0 = definition \ "CreateUserError" \ "0.2.0"
      checkCreateUser(createUserError0_2_0)
      val admin0_3_0 = definition \ "admin" \ "0.3.0"
      checkAdmin3(admin0_3_0)
      val admin0_1_0 = definition \ "admin" \ "0.1.0"
      checkAdmin1(admin0_1_0)
    }

    val sources = List(new File(getClass.getResource("/_apidoc.js").getFile),
      new File(getClass.getResource("/full-example.js").getFile))

    val (json, filenames) = Parser(sources)

    val parsedFiles = Worker.processFilename(json, filenames.toList)

    val preProcessResults = Worker.preProcess(parsedFiles)

    assert(preProcessResults.children.size === 11)
    assert(preProcessResults \ "defineErrorStructure" === JObject())
    assert(preProcessResults \ "defineHeaderStructure" === JObject())
    assert(preProcessResults \ "defineSuccessStructure" === JObject())

    val defineErrorTitle = preProcessResults \ "defineErrorTitle"

    checkDefinition(defineErrorTitle)

    val defineGroup = preProcessResults \ "defineGroup"

    checkDefinition(defineGroup)

    val defineParamTitle = preProcessResults \ "defineParamTitle"

    checkDefinition(defineParamTitle)

    val definePermission = preProcessResults \ "definePermission"

    checkDefinition(definePermission)

    val defineSuccessTitle = preProcessResults \ "defineSuccessTitle"

    checkDefinition(defineSuccessTitle)

    val define = preProcessResults \ "define"

    val createUserField = define \ "CreateUserError" \ "0.2.0"

    assert(createUserField \ "version" === JString("0.2.0"))

    val error = createUserField \ "error" \ "fields" \ "Error 4xx"
    assert(error(0) \ "group" === JString("Error 4xx"))
    assert(error(0) \ "optional" === JString("false"))
    assert(error(0) \ "field" === JString("NoAccessRight"))
    assert(error(0) \ "description" === JString("Only authenticated Admins can access the data."))

    assert(error(1) \ "group" === JString("Error 4xx"))
    assert(error(1) \ "optional" === JString("false"))
    assert(error(1) \ "field" === JString("UserNameTooShort"))
    assert(error(1) \ "description" === JString("Minimum of 5 characters required."))

    val example = createUserField \ "error" \ "examples"
    assert(example(0) \ "title" === JString("Response (example):"))
    assert(example(0) \ "content" === JString("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(example(0) \ "type" === JString("json"))

    assert(example(0) \ "type" === JString("json"))

    assert(define \ "admin" \ "0.3.0" \ "version" === JString("0.3.0"))
    assert(define \ "admin" \ "0.1.0" \ "version" === JString("0.1.0"))

  }

  "ApiParamTitle Worker" should " postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = Util.readFile(parsedFilesFiles)

    val JArray(l) = parse(parsedFileString)
    val worker = new ApiParamTitleWorker

    val result = worker.postProcess(JArray(l), List(), preProcessJson, conf)

    assert(result === JArray(l))
  }

  "ApiUser Worker" should " postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/apiusePreprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val blocksFiles = new File(getClass.getResource("/apiuseBlocks.json").getFile)
    val blocksString = Util.readFile(blocksFiles)

    val JArray(l) = parse(blocksString)
    val worker = new ApiUseWorker

    val result = worker.postProcess(JArray(l), List(), preProcessJson, conf)
    val error = result \ "local" \ "error"

    val error4XX = error \ "fields" \ "Error 4xx"
    assert(error4XX(0) \ "group" === JString("Error 4xx"))
    assert(error4XX(0) \ "optional" === JString("false"))
    assert(error4XX(0) \ "field" === JString("NoAccessRight"))
    assert(error4XX(0) \ "description" === JString("Only authenticated Admins can access the data."))

    assert(error4XX(1) \ "group" === JString("Error 4xx"))
    assert(error4XX(1) \ "optional" === JString("false"))
    assert(error4XX(1) \ "field" === JString("UserNameTooShort"))
    assert(error4XX(1) \ "description" === JString("Minimum of 5 characters required."))

    val examples = error \ "examples"
    assert(examples(0) \ "title" === JString("Response (example):"))
    assert(examples(0) \ "content" === JString("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(examples(0) \ "type" === JString("json"))

    val actual: JObject = ("local" ->
      ("error" -> error)
    )

    assert(actual === JArray(l).diff(result).added)
  }

  "ApiSampleRequestWorker" should " postprocess - local url with sampleURL" in {

    val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = Util.readFile(parsedFilesFiles)

    val JArray(l) = parse(parsedFileString)
    val worker = new ApiSampleRequestWorker

    val result = worker.postProcess(JArray(l), List("_apidoc.js", "full-example.js"), preProcessJson, conf)
    assert(result === JArray(l))

  }

  "ApiSampleRequestWorker" should " postprocess with sampleURL" in {

    val (parsedFiles, filenames) = Parser.apply(List(new File(getClass.getResource("/sampleRequest.js").getFile)))

    val worker = new ApiSampleRequestWorker

    val result = worker.postProcess(parsedFiles, filenames, JObject(), conf)

    val JArray(List(block1, block2, block3)) = result(0)
    assert((block1 \ "local" \ "sampleRequest")(0) \ "url" === JString("http://www.example.com/user/4711"))
    assert((block2 \ "local" \ "sampleRequest")(0) \ "url" === JString("https://api.github.com/v1/car/4711"))
    assert(block3 \ "local" \ "sampleRequest" === JNothing)
  }

  "ApiSampleRequestWorker" should " postprocess without sampleURL" in {

    val (parsedFiles, filenames) = Parser.apply(List(new File(getClass.getResource("/sampleRequest.js").getFile)))

    val worker = new ApiSampleRequestWorker

    val conf2 = SbtApidocConfiguration("name", "description", None, "1.2")

    val result = worker.postProcess(parsedFiles, filenames.toList, JObject(), conf2)

    val file1 = result(0)
    val JArray(List(block1, block2, block3)) = file1
    assert((block1 \ "local" \ "sampleRequest")(0) \ "url" === JString("http://www.example.com/user/4711"))
    assert((block2 \ "local" \ "sampleRequest")(0) \ "url" === JString("/car/4711"))
    assert(block3 \ "local" \ "sampleRequest" === JNothing)
  }

  "ApiGroupWorker" should " postprocess" in {

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = Util.readFile(parsedFilesFiles)

    val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val JArray(l) = parse(parsedFileString)
    val worker = new ApiGroupWorker

    val result = worker.postProcess(JArray(l), List("_apidoc.js", "full-example.js"), preProcessJson, conf)

    val JArray(List(file1, file2)) = result

    val JArray(List(_, _, _, block4, block5, block6)) = file1
    val JArray(List(block2_1, block2_2, block2_3)) = file2
    assert(block4 \ "local" \ "groupTitle" === JString("User"))
    assert(block5 \ "local" \ "groupTitle" === JString("User"))
    assert(block6 \ "local" \ "groupTitle" === JString("User"))
    assert(block2_1 \ "local" \ "groupTitle" === JString("User"))
    assert(block2_2 \ "local" \ "groupTitle" === JString("User"))
    assert(block2_3 \ "local" \ "groupTitle" === JString("User"))
    assert((result \\ "groupTitle").children.size === 6)

  }

  "ApiNameWorker" should " postprocess" in {

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = Util.readFile(parsedFilesFiles)

    val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val JArray(l) = parse(parsedFileString)
    val worker = new ApiNameWorker

    val result = worker.postProcess(JArray(l), List("_apidoc.js", "full-example.js"), preProcessJson, conf)

    val JArray(List(file1, file2)) = result

    val JArray(List(_, _, _, block4, block5, block6)) = file1
    val JArray(List(block2_1, block2_2, block2_3)) = file2
    assert(block4 \ "local" \ "name" === JString("GetUser"))
    assert(block5 \ "local" \ "name" === JString("GetUser"))
    assert(block6 \ "local" \ "name" === JString("PostUser"))
    assert(block2_1 \ "local" \ "name" === JString("GetUser"))
    assert(block2_2 \ "local" \ "name" === JString("PostUser"))
    assert(block2_3 \ "local" \ "name" === JString("PutUser"))

  }

  "ApiPermissionWorker" should " postprocess" in {

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = Util.readFile(parsedFilesFiles)

    val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
    val preProcessString = Util.readFile(preProcessFiles)
    val preProcessJson = parse(preProcessString)

    val JArray(l) = parse(parsedFileString)
    val worker = new ApiPermissionWorker

    val result = worker.postProcess(JArray(l), List("_apidoc.js", "full-example.js"), preProcessJson, conf)

    val JArray(List(file1, file2)) = result
    val JArray(List(_, _, _, block4, block5, block6)) = file1

    val permission4 = block4 \ "local" \ "permission"
    assert(permission4(0) \ "name" === JString("admin"))
    assert(permission4(0) \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(permission4(0) \ "description" === JString(""))

    val permission5 = block5 \ "local" \ "permission"

    assert(permission5(0) \ "name" === JString("admin"))
    assert(permission5(0) \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(permission5(0) \ "description" === JString(""))

    val permission6 = block6 \ "local" \ "permission"

    assert(permission6(0) \ "name" === JString("none"))
    assert(permission6(0) \ "title" === JNothing)
    assert(permission6(0) \ "description" === JNothing)

    val JArray(List(block1, block2, block3)) = file2

    //    println(pretty(render(block1)))

    val permission1 = block1 \ "local" \ "permission"

    assert(permission1(0) \ "name" === JString("admin"))
    assert(permission1(0) \ "title" === JString("Admin access rights needed."))
    assert(permission1(0) \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val permission2 = block2 \ "local" \ "permission"

    assert(permission2(0) \ "name" === JString("none"))
    assert(permission2(0) \ "title" === JNothing)
    assert(permission2(0) \ "description" === JNothing)

    val permission3 = block3 \ "local" \ "permission"

    assert(permission3(0) \ "name" === JString("none"))
    assert(permission3(0) \ "title" === JNothing)
    assert(permission3(0) \ "description" === JNothing)

    assert(JArray(l).diff(result).deleted === JNothing)
    assert(JArray(l).diff(result).changed === JNothing)

  }

}