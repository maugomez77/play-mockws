package mockws

import java.io.{ByteArrayInputStream, InputStream}
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpHeaders}
import org.asynchttpclient.Response
import org.asynchttpclient.cookie.{Cookie, CookieDecoder}
import org.asynchttpclient.uri.Uri
import org.asynchttpclient.util.HttpUtils
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import play.api.mvc.Result

import scala.collection.JavaConversions._

/**
 * A simulated response from the async-http-client.
 *
 * The [[play.api.libs.ws.ahc.AhcWSResponse]] is intended to wrap this.
 *
 * Implementation is mostly based of [[org.asynchttpclient.netty.NettyResponse]].
 *
 * We're faking at this level as opposed to the [[play.api.libs.ws.WSResponse]] level
 * to preserve any behavior specific to the NingWSResponse which is likely to be used
 * in the real (non-fake) WSClient.
 */
class FakeAhcResponse(result: Result, body: Array[Byte]) extends Response {

  private val NettyDefaultCharset: Charset = Charset.forName("ISO-8859-1")

  override def getLocalAddress: SocketAddress = ???

  override def getRemoteAddress: SocketAddress = ???

  override def getResponseBody(charset: Charset): String = new String(getResponseBodyAsBytes(), computeCharset(charset))

  override def getResponseBodyAsByteBuffer: ByteBuffer = ByteBuffer.wrap(body)

  override def getStatusCode: Int = result.header.status

  override def getResponseBodyAsBytes: Array[Byte] = body

  override def getResponseBodyAsStream: InputStream = new ByteArrayInputStream(body)

  override def isRedirected: Boolean = Set(301, 302, 303, 307, 308).contains(getStatusCode)

  override def getCookies: util.List[Cookie] = getHeaders("Set-Cookie").map(CookieDecoder.decode)

  override def hasResponseBody: Boolean = body.nonEmpty

  override def getStatusText: String = HttpResponseStatus.valueOf(getStatusCode).toString

  override def getHeaders(name: String): util.List[String] = getHeaders.getAll(name)

  override def getHeaders: HttpHeaders = {
    val scalaHeaders = FakeWSResponseHeaders.toMultiMap(result.header)

    val headers = new DefaultHttpHeaders()
    scalaHeaders.foreach(e ⇒ headers.add(e._1, asJavaCollection(e._2)))
    result.body.contentType foreach (ct ⇒ headers.add("Content-Type", ct))
    headers
  }

  override def hasResponseHeaders: Boolean = true // really asking if the request has been completed.

  override def getResponseBody: String = getResponseBody(null)

  override def getContentType: String = getHeader("Content-Type")

  override def hasResponseStatus: Boolean = true // really asking if the request has been completed.

  override def getUri: Uri = throw new NotImplementedError("unavailable here and unused by NingWSResponse")

  override def getHeader(name: String): String = getHeaders.get(name)

  private def computeCharset(charset: Charset): Charset =
    Option(charset)
      .orElse(charsetFromContentType)
      .getOrElse(NettyDefaultCharset)

  private def charsetFromContentType: Option[Charset] =
    Option(getContentType)
      .flatMap(ct => Option(HttpUtils.parseCharset(ct)))

}
