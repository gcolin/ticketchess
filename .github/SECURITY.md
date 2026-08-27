# Security Policy

## Reporting a Vulnerability

**Do not** open a public issue for security problems.

### Preferred: GitHub Security Advisories

On the GitHub repository, use **Security → Advisories → Report a vulnerability** for private responsible disclosure.

If Security Advisories are not enabled on a fork or mirror, contact the repository maintainer via a private GitHub message.

We aim to respond within **72 hours** and provide a fix or mitigation plan within **30 days** depending on severity.

Please include:

- Description and potential impact
- Steps to reproduce
- Affected version or commit
- Suggested fix, if any

## Scope

This includes, among others:

- Authentication and authorization (OAuth, JWT, sessions)
- SQL injection / XSS / CSRF
- Secret or personal data exposure
- Stripe payment bypass
- Privilege escalation (impersonation, admin access)

Misconfiguration in production (missing OAuth, default `jwt.key`, exposed `/auth-sim`) is documented in the [README](README.md), not treated as application vulnerabilities.

## Deployment Best Practices

- Never commit `params.properties` — use `params.properties.example`
- In production: `CONFIG_DIR=/opt/ticketchess/config` mounted outside git
- Configure **OAuth** and a unique **JWT key**
- Set **`source.url`** to the public repository (AGPL compliance)
- CI/CD secrets: GitHub Actions variables (masked + protected), not in code

## Sensitive Files

Must **never** be versioned:

- `params.properties` / `params_prod.properties`
- `src/docker/db.sql`
- `rib.pdf`, logos, local deploy scripts

## Git History

If secrets were ever committed, rotate them **before** any public release, then purge history with `git filter-repo` (see [SECURITY.md](SECURITY.md) in French for commands).

## Supported Versions

Only the `main` branch (latest release) receives security fixes.
