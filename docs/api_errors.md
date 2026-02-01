## Error format
Errors are returned as JSON:
`{ "errorCode": "...", "message": "...", "requestId": "..." }`

## RATE_LIMITED
HTTP status: 429
Meaning: too many requests per minute for an API key.

## INVALID_SIGNATURE
HTTP status: 400
Meaning: webhook signature verification failed.
Resolution: compute HMAC-SHA256 over the raw body and compare to `X-Signature`.

## INVALID_INPUT
HTTP status: 400
Meaning: request payload failed validation.
Resolution: check required fields and data types.

