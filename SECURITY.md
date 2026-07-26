# Security policy

## Supported code

The latest `main` branch is supported. This research repository does not yet
publish a stable compatibility line.

## Reporting a vulnerability

Please report vulnerabilities privately through
[GitHub Security Advisories](https://github.com/wasiliy-strecker/java-performance-lab/security/advisories/new).
Do not open a public issue containing exploit details, secrets, personal data,
or sensitive recordings.

Include:

- affected commit and module
- reproduction steps or a minimal sanitized fixture
- expected and observed impact
- suggested mitigation, if known

## Data and diagnostic safety

Never submit real customer order rows or production JFR recordings. Custom JFR
events are designed to avoid raw input, but recordings can still contain JVM and
environment metadata. Review diagnostic artifacts before sharing them.

The parser treats all input as untrusted: numeric and aggregate overflow must
remain explicit, malformed rows must not partially mutate totals, and error
reporting must not echo row contents.
