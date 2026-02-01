
## Authentication
Requests must include an API key using header `X-API-Key`.
Keys are environment-specific: `sandbox` keys will not work in `production`.

## Webhooks setup
Webhook endpoint must respond within 3 seconds.
We retry failed webhooks up to 5 times with exponential backoff.
We sign webhooks using header `X-Signature` (HMAC-SHA256 of raw request body).

## Rate limits
Default limit is 60 requests per minute per API key.
If exceeded, API returns HTTP 429 with error code `RATE_LIMITED`.
