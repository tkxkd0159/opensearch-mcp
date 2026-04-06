# Ad-Hoc OpenSearch Auth Header Redesign

Date: 2026-04-06

## Context

The current HTTP ad-hoc mode accepts `X-OpenSearch-Username` and `X-OpenSearch-Password` when a tool call uses `clusterUrl`. This is limited to Basic authentication and exposes a split-header contract that does not scale well to additional auth schemes.

The repository already has a clean internal separation:

- Caller-to-MCP authentication is outside the OpenSearch resolver contract.
- Registered clusters (`clusterName`) use server-configured credentials.
- Ad-hoc clusters (`clusterUrl`) build a per-request client and send an outbound `Authorization` header to OpenSearch.

The redesign should keep those trust boundaries explicit and make ad-hoc auth extensible.

## Goals

- Support multiple ad-hoc OpenSearch auth schemes over time, not only Basic auth.
- Keep caller-to-MCP authentication separate from MCP-to-OpenSearch authentication.
- Make `clusterName` and `clusterUrl` resolution unambiguous.
- Replace the current split username/password header contract with a single header.
- Keep the implementation small and consistent with the existing resolver design.

## Non-Goals

- Changing how configured `clusterName` clients load credentials.
- Adding a general-purpose auth framework for future non-header-based schemes.
- Supporting backward compatibility for `X-OpenSearch-Username` and `X-OpenSearch-Password`.
- Reusing standard `Authorization` for two different trust boundaries.

## Options Considered

### Option 1: Reuse standard `Authorization`

Interpret `Authorization` as ad-hoc OpenSearch credentials for `clusterUrl`, while also using it for caller-to-MCP authentication in `clusterName` flows.

Rejected because one standard header would represent two unrelated identities depending on tool inputs. That is ambiguous, harder to document, and a poor base for future auth growth.

### Option 2: `X-OpenSearch-Auth` with raw base64

Replace the two existing headers with one custom header that carries `base64(username:password)`.

Rejected because it still hardcodes Basic auth semantics and discards the auth scheme name. Future support for token-based schemes would require redefining the contract again.

### Option 3: `X-OpenSearch-Authorization`

Use a dedicated OpenSearch-scoped header whose value follows normal authorization syntax, such as `Basic <base64(username:password)>` or `Bearer <token>`.

Accepted because it keeps the trust boundary clear, matches standard authorization value syntax, and stays open to multiple header-based auth schemes without overloading standard `Authorization`.

## Decision

Adopt `X-OpenSearch-Authorization` as the only ad-hoc OpenSearch auth header.

Rules:

- `Authorization` is reserved for caller-to-MCP authentication.
- `X-OpenSearch-Authorization` is reserved for ad-hoc `clusterUrl` authentication to the target OpenSearch cluster.
- Tool calls must provide exactly one of `clusterName` or `clusterUrl`.
- `clusterName` always uses a server-configured client.
- `clusterUrl` always uses a request-scoped ad-hoc client.

## Request Contract

### `clusterName`

- Uses the configured cluster client only.
- Ignores `X-OpenSearch-Authorization` if present.
- Ignores `X-OpenSearch-SSL-Disabled` if present because TLS behavior is part of the configured client.

### `clusterUrl`

- Requires `X-OpenSearch-Authorization`.
- Uses `X-OpenSearch-SSL-Disabled` as it does today.
- Forwards the `X-OpenSearch-Authorization` value to the outbound OpenSearch request as the normal `Authorization` header.

## Validation Rules

Validation is centralized so every tool gets identical behavior.

- Neither `clusterName` nor `clusterUrl` provided: reject.
- Both `clusterName` and `clusterUrl` provided: reject.
- `clusterUrl` without `X-OpenSearch-Authorization`: reject.
- `clusterUrl` with blank `X-OpenSearch-Authorization`: reject.
- `clusterUrl` over a non-HTTP transport with no request context: reject.

Validation intentionally does not reject `clusterName` when `X-OpenSearch-Authorization` is present. In that case the header is ignored.

## Header Format

`X-OpenSearch-Authorization` is treated as opaque auth material after minimal format validation.

Expected format:

- `<scheme> <credentials>`

Examples:

- `Basic YWRtaW46c2VjcmV0`
- `Bearer eyJ...`

Minimal validation:

- Header value must be non-blank.
- Header value must contain a non-blank scheme.
- Header value must contain non-blank credentials after the first space.

The server does not parse Basic credentials and does not specialize behavior by auth scheme.

## Error Messages

Use explicit, action-oriented messages:

- `Provide exactly one of clusterName or clusterUrl.`
- `clusterUrl requires X-OpenSearch-Authorization.`
- `X-OpenSearch-Authorization must use the format '<scheme> <credentials>'.`
- `Ad-hoc mode (clusterUrl) is only supported over HTTP transport.`

## Implementation Scope

The change remains intentionally small:

- Update `ClusterResolver` validation and ad-hoc client construction.
- Replace username/password header lookup with `X-OpenSearch-Authorization`.
- Remove the current precedence rule where `clusterUrl` silently wins over `clusterName`.
- Update tool parameter descriptions to document the new header and the exact-one-of requirement.
- Update README and any other user-facing docs that mention ad-hoc auth headers.
- Update unit tests for the new exclusivity and header rules.

No additional auth abstraction is introduced in this change.

## Security Notes

- Never log `X-OpenSearch-Authorization` values.
- Keep caller-to-MCP auth and target-cluster auth separate in documentation and code.
- Do not silently reinterpret caller `Authorization` as OpenSearch credentials.

## Testing

Update and add unit tests for:

- `clusterName` returns a configured client.
- Unknown `clusterName` fails clearly.
- Missing both `clusterName` and `clusterUrl` fails clearly.
- Providing both `clusterName` and `clusterUrl` fails clearly.
- `clusterUrl` with valid `X-OpenSearch-Authorization` returns a new client.
- `clusterUrl` without `X-OpenSearch-Authorization` fails clearly.
- `clusterUrl` with blank `X-OpenSearch-Authorization` fails clearly.
- `clusterUrl` with malformed `X-OpenSearch-Authorization` fails clearly.
- `clusterUrl` without HTTP request context fails clearly.
- `clusterName` ignores `X-OpenSearch-Authorization`.

Update tool-level descriptions and tests where the old username/password wording appears.

## Migration Impact

This is a clean breaking change.

Clients that use ad-hoc mode must stop sending:

- `X-OpenSearch-Username`
- `X-OpenSearch-Password`

They must instead send:

- `X-OpenSearch-Authorization`

Example:

```http
X-OpenSearch-Authorization: Basic YWRtaW46c2VjcmV0
```

Configured-cluster flows using `clusterName` do not change.
