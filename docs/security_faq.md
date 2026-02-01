## Data handling
Do not log full payment details or secrets.
Mask API keys in logs.

## Webhook signature verification
Verify `X-Signature` using HMAC-SHA256 of the raw request body.
Reject requests that fail verification.

## Key rotation
API keys can be rotated. Keep old keys active during rollout to avoid downtime.

