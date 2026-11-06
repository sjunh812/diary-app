package org.sjhstudio.diary.drivecheck

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// 드라이브 REST v3를 가장 단순하게 부른다.
// 확인용이라 라이브러리 없이 HttpURLConnection만 쓴다.
// 토큰은 어디에도 남기지 않는다.
object DriveRest {
    private const val FIELDS = "id,name,size,createdTime,appProperties,sha256Checksum,md5Checksum"

    fun userEmail(token: String): String {
        val body = request("GET", "https://openidconnect.googleapis.com/v1/userinfo", token)
        return JSONObject(body).optString("email", "?")
    }

    fun createFolder(token: String, name: String, props: Map<String, String>): JSONObject {
        val meta = JSONObject()
            .put("name", name)
            .put("mimeType", "application/vnd.google-apps.folder")
            .put("appProperties", JSONObject(props))
        val body = request(
            "POST",
            "https://www.googleapis.com/drive/v3/files?fields=$FIELDS",
            token,
            meta.toString().toByteArray(),
            "application/json; charset=UTF-8",
        )
        return JSONObject(body)
    }

    fun createTextFile(token: String, parentId: String, name: String, text: String, props: Map<String, String>): JSONObject {
        val boundary = "drivecheck${System.currentTimeMillis()}"
        val meta = JSONObject()
            .put("name", name)
            .put("parents", org.json.JSONArray().put(parentId))
            .put("appProperties", JSONObject(props))
        val payload = buildString {
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta.toString())
            append("\r\n--$boundary\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n")
            append(text)
            append("\r\n--$boundary--\r\n")
        }
        val body = request(
            "POST",
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=$FIELDS",
            token,
            payload.toByteArray(),
            "multipart/related; boundary=$boundary",
        )
        return JSONObject(body)
    }

    fun list(token: String, query: String): List<JSONObject> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=drive&pageSize=100&q=$q&fields=files($FIELDS)"
        val files = JSONObject(request("GET", url, token)).optJSONArray("files") ?: return emptyList()
        return (0 until files.length()).map { files.getJSONObject(it) }
    }

    // 지우지 않고 드라이브 휴지통으로 옮긴다(30일 안에 복원할 수 있음).
    fun trash(token: String, fileId: String) {
        request(
            "PATCH",
            "https://www.googleapis.com/drive/v3/files/$fileId?fields=id,trashed",
            token,
            """{"trashed":true}""".toByteArray(),
            "application/json; charset=UTF-8",
        )
    }

    private fun request(method: String, url: String, token: String, body: ByteArray? = null, contentType: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        // HttpURLConnection은 PATCH를 몰라서 POST에 덮어쓰기 머리글을 단다.
        if (method == "PATCH") {
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            conn.requestMethod = method
        }
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", contentType)
            conn.outputStream.use { it.write(body) }
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code ${text.take(300)}")
        return text
    }
}
