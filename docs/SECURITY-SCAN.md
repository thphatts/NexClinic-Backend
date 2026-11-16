# Trivy investigation: 2026-11-16

The scan step uses `--exit-code 1 --ignore-unfixed --severity CRITICAL,HIGH`.
A completed scan with findings intentionally fails the release gate. The report
is printed to the job log and retained as `image-security-report`.

Dependency updates (kept on Spring Boot 3):

| Component | Previous | Updated |
| --- | --- | --- |
| Spring Boot parent/BOM | 3.4.13 | 3.5.16 |
| Jackson BOM | 2.18.5 | 2.21.7 |
| Netty BOM | 4.1.130.Final | 4.1.140.Final |
| Embedded Tomcat | managed previous version | 10.1.62 |
| PostgreSQL JDBC | managed previous version | 42.7.14 |

Local verification: 93 tests, zero failures/errors, one skipped. Maven package
succeeds. Trivy 0.76.2 scanning an SBOM of all 130 resolved runtime Maven
components still reports two CRITICAL findings for `spring-webmvc:6.2.19`:

- CVE-2026-47884: https://spring.io/security/cve-2026-47884/
- CVE-2026-47890: https://spring.io/security/cve-2026-47890/

The vendor advisories classify these as MEDIUM and LOW respectively, and describe
specific view-rendering/SSE-fragment prerequisites. The scanner's gate is left
unchanged; neither finding is suppressed. A different vendor severity alone is
not treated as proof the application is unaffected.

The vendor lists Spring Framework 6.2.20 as an Enterprise-only fix and 7.0.9 as
an OSS fix. Staying on Boot 3 therefore needs an authorized Enterprise artifact
source, or a separately validated Boot 4/Framework 7 migration. Do not insert
Framework 7 alone into the Boot 3 dependency set.

This local dependency SBOM scan is diagnostic, not a replacement for the CI
container-image scan. It does not cover OS packages or confirm the exact GitHub
artifact. Local full-image rescans were interrupted by Java DB download EOFs;
CI must complete the full scan before a release can proceed.
