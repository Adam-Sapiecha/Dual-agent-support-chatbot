
## HTTP 401 Unauthorized
Common causes:
- Missing `X-API-Key` header
- Using a sandbox key against production
Resolution: verify the header is present and the key matches the environment.

## HTTP 429 Too Many Requests
This indicates rate limiting.
Resolution:
- Reduce request frequency
- Add client-side retries with backoff
- Consider batching requests

## Webhook failures
If your endpoint returns non-2xx or times out (>3s), we mark it as failed and retry up to 5 times.
Resolution:
- Ensure a 2xx response within 3 seconds
- Log and inspect payloads and signature verification
