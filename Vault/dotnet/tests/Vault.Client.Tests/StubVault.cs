//Copyright Notice!
//This document is protected under the trade secret and copyright
//laws as the property of Fidelity National Information Services, Inc.
//Copying, reproduction or distribution should be limited and only to
//employees with a "need to know" to do their job.
//Any disclosure of this document to third parties is strictly prohibited.

//copyright 2026 Fidelity National Information Services.
//All rights reserved worldwide.

// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

using System.Net;
using System.Text;

namespace Fis.Foundation.Vault.Tests
{
    /// <summary>
    /// In-process fake of Vault's HTTP API. Routes are "METHOD path-under-v1"; unmatched requests get 404.
    /// All values here are test fixtures, not real credentials.
    /// </summary>
    internal sealed class StubVault : HttpMessageHandler
    {
        internal const string Address = "https://vault.test:8200/";
        internal const string WrappingToken = "hvs.WRAPPING-FIXTURE";
        internal const string SecretId = "SECRET-ID-FIXTURE";
        internal const string ClientToken = "hvs.CLIENT-FIXTURE";
        internal const string SecretValue = "PASSWORD-FIXTURE";

        private readonly Dictionary<string, Func<RecordedRequest, HttpResponseMessage>> _routes = new();
        private readonly List<RecordedRequest> _requests = new();

        internal IReadOnlyList<RecordedRequest> Requests
        {
            get { lock (_requests) return _requests.ToList(); }
        }

        internal StubVault On(string method, string path, Func<RecordedRequest, HttpResponseMessage> respond)
        {
            _routes[$"{method} {path}"] = respond;
            return this;
        }

        internal StubVault On(string method, string path, int status, string json = "{}") =>
            On(method, path, _ => Json(status, json));

        internal int Count(string method, string path) => Requests.Count(r => r.Method == method && r.Path == path);

        /// <summary>Routes for a working AppRole unwrap + login and a KV secret at app/db.</summary>
        internal static StubVault WithHappyPath(int leaseSeconds = 3600)
        {
            return new StubVault()
                .On("POST", "sys/wrapping/unwrap", r => r.Token == WrappingToken
                    ? Json(200, $"{{\"data\":{{\"secret_id\":\"{SecretId}\"}}}}")
                    : Json(400, "{\"errors\":[\"wrapping token is not valid or does not exist\"]}"))
                .On("POST", "auth/approle/login", _ => Json(200,
                    $"{{\"auth\":{{\"client_token\":\"{ClientToken}\",\"lease_duration\":{leaseSeconds},\"renewable\":true}}}}"))
                .On("GET", "secret/data/app/db", 200,
                    $"{{\"data\":{{\"data\":{{\"username\":\"svc_app\",\"password\":\"{SecretValue}\",\"port\":443}},\"metadata\":{{\"version\":3}}}}}}")
                .On("GET", "auth/token/lookup-self", 200, "{\"data\":{}}")
                .On("POST", "auth/token/revoke-self", 204, "")
                .On("POST", "auth/token/renew-self", _ => Json(200,
                    $"{{\"auth\":{{\"client_token\":\"{ClientToken}\",\"lease_duration\":{leaseSeconds},\"renewable\":true}}}}"));
        }

        internal static HttpResponseMessage Json(int status, string json)
        {
            var response = new HttpResponseMessage((HttpStatusCode)status);
            if (json.Length > 0) response.Content = new StringContent(json, Encoding.UTF8, "application/json");
            return response;
        }

        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            string path = request.RequestUri!.AbsolutePath;
            if (path.StartsWith("/v1/", StringComparison.Ordinal)) path = path.Substring(4);

            string? body = request.Content == null ? null : await request.Content.ReadAsStringAsync(cancellationToken);
            var recorded = new RecordedRequest(
                request.Method.Method,
                path,
                Header(request, "X-Vault-Token"),
                Header(request, "X-Vault-Namespace"),
                body);

            lock (_requests) _requests.Add(recorded);

            return _routes.TryGetValue($"{recorded.Method} {path}", out var respond)
                ? respond(recorded)
                : Json(404, "{\"errors\":[]}");
        }

        private static string? Header(HttpRequestMessage request, string name) =>
            request.Headers.TryGetValues(name, out var values) ? values.FirstOrDefault() : null;
    }

    internal sealed record RecordedRequest(string Method, string Path, string? Token, string? Namespace, string? Body);

    /// <summary>A clock the test moves by hand.</summary>
    internal sealed class ManualTime : TimeProvider
    {
        private DateTimeOffset _now = new DateTimeOffset(2026, 10, 15, 12, 0, 0, TimeSpan.Zero);

        public override DateTimeOffset GetUtcNow() => _now;

        internal void Advance(TimeSpan by) => _now += by;
    }
}
