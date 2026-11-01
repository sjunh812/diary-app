import Foundation

// 드라이브 REST v3를 가장 단순하게 부른다.
// 토큰은 어디에도 남기지 않는다.
enum DriveRest {
    static let fields = "id,name,size,createdTime,appProperties,sha256Checksum,md5Checksum"
    static let probeFiles = "appProperties has { key='diary.kind' and value='probe-file' } and trashed = false"
    static let legacyBackup = "name = 'note.db' and trashed = false"

    static func createFolder(token: String, name: String, props: [String: String]) async throws -> [String: Any] {
        let meta: [String: Any] = ["name": name, "mimeType": "application/vnd.google-apps.folder", "appProperties": props]
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/drive/v3/files?fields=\(fields)")!)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: meta)
        return try await send(req, token: token)
    }

    static func createTextFile(token: String, parentId: String, name: String, text: String, props: [String: String]) async throws -> [String: Any] {
        let boundary = "drivecheck\(Int(Date().timeIntervalSince1970))"
        let meta: [String: Any] = ["name": name, "parents": [parentId], "appProperties": props]
        let metaJson = String(data: try JSONSerialization.data(withJSONObject: meta), encoding: .utf8)!
        let body = "--\(boundary)\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n\(metaJson)\r\n--\(boundary)\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n\(text)\r\n--\(boundary)--\r\n"
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=\(fields)")!)
        req.httpMethod = "POST"
        req.setValue("multipart/related; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        req.httpBody = body.data(using: .utf8)
        return try await send(req, token: token)
    }

    static func list(token: String, query: String) async throws -> [[String: Any]] {
        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files")!
        comps.queryItems = [
            URLQueryItem(name: "spaces", value: "drive"),
            URLQueryItem(name: "pageSize", value: "100"),
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "fields", value: "files(\(fields))"),
        ]
        let json = try await send(URLRequest(url: comps.url!), token: token)
        return json["files"] as? [[String: Any]] ?? []
    }

    private static func send(_ request: URLRequest, token: String) async throws -> [String: Any] {
        var req = request
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: req)
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else {
            throw NSError(domain: "Drive", code: code, userInfo: [NSLocalizedDescriptionKey: "HTTP \(code) \(String(data: data, encoding: .utf8)?.prefix(300) ?? "")"])
        }
        return (try JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }
}
