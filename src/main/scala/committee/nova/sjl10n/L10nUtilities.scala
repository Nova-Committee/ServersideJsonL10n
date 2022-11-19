package committee.nova.sjl10n

import com.google.common.collect.ImmutableMap
import com.google.gson._
import com.sun.istack.internal.Nullable
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager

import java.io.{IOException, InputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import scala.collection.JavaConversions._

object L10nUtilities {
  final val gson = new Gson()
  final val tokenPattern = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]")
  final val defaultLang = "en_us"
  final val logger = LogManager.getLogger

  def create(modId: String, lang: String): JsonText = {
    val builderDefault = ImmutableMap.builder[String, String]()
    val builder = ImmutableMap.builder[String, String]()
    val consumerDefault = (s1: String, s2: String) => builderDefault.put(s1, s2)
    val consumer = (s1: String, s2: String) => builder.put(s1, s2)
    val resourceLocation = s"/assets/$modId/lang/$lang.json"
    try {
      val streamDefault = classOf[JsonText].getResourceAsStream(s"/assets/$modId/lang/$defaultLang.json")
      var stream = classOf[JsonText].getResourceAsStream(resourceLocation)
      if (stream == null) {
        logger.info(s"No lang file for current language '$lang' found. Defaulted it to $defaultLang")
        stream = streamDefault
      }
      try {
        load(streamDefault, consumerDefault)
        load(stream, consumer)
      } catch {
        case t: Throwable => throw t
      } finally {
        streamDefault.close()
        stream.close()
      }
    } catch {
      case e@(_: JsonParseException | _: IOException) => logger.error(s"Couldn't read strings from $resourceLocation", e)
    }
    val map = builder.build()
    val defaultMap = builderDefault.build()
    JsonText(key => map.getOrElse(key, defaultMap.getOrDefault(key, key)))
  }


  def load(inputStream: InputStream, entryConsumer: (String, String) => Any): Unit = {
    val jsonObject = gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), classOf[JsonObject])
    for (entry <- jsonObject.entrySet) {
      val string = tokenPattern.matcher(convertToString(entry.getValue, entry.getKey)).replaceAll("%$1s")
      entryConsumer.apply(entry.getKey, string)
    }
  }

  def convertToString(json: JsonElement, string: String): String = {
    if (json.isJsonPrimitive) json.getAsString
    else throw new JsonSyntaxException("Expected " + string + " to be a string, was " + getType(json))
  }

  def getType(@Nullable obj: JsonElement): String = {
    val s = StringUtils.abbreviateMiddle(String.valueOf(obj.asInstanceOf[Any]), "...", 10)
    if (obj == null) return "null (missing)"
    if (obj.isJsonNull) return "null (json)"
    if (obj.isJsonArray) return "an array (" + s + ")"
    if (obj.isJsonObject) return "an object (" + s + ")"
    if (obj.isJsonPrimitive) {
      val json = obj.getAsJsonPrimitive
      if (json.isNumber) return "a number (" + s + ")"
      if (json.isBoolean) return "a boolean (" + s + ")"
    }
    s
  }

  abstract class JsonText {
    def get(key: String): String
  }

  object JsonText {
    def apply(getter: String => String): JsonText = {
      new JsonText {
        override def get(key: String): String = getter.apply(key)
      }
    }
  }
}
