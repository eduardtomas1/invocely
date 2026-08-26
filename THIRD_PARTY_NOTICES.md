# Third-party notices

Invoicely is distributed under the Apache License 2.0, but its standalone JAR and macOS application include third-party software under additional licenses.

The build generates a dependency-by-dependency inventory from Maven metadata, fails if a runtime dependency has no known license, and embeds that inventory as `META-INF/invocely-THIRD-PARTY.txt` in the standalone JAR.

Notable runtime components include:

| Component | License |
|---|---|
| JasperReports and its PDF/JDT modules | GNU Lesser General Public License 3.0 |
| Eclipse JDT compiler | Eclipse Public License 2.0 |
| OpenPDF | Mozilla Public License 2.0 or GNU Lesser General Public License 2.1 |
| FlatLaf, Gson, Jackson, Apache Commons, Apache Batik | Apache License 2.0 |
| Adobe XMPCore | BSD 3-Clause License |
| Stax2 API | BSD 2-Clause License |
| ANTLR 2 and CurvesAPI | BSD-style licenses |

The exact versions are defined in `pom.xml` and its resolved dependency graph. The standalone distribution includes Invoicely's Apache 2.0 text and JasperReports' LGPL 3.0 text under `META-INF/licenses/`. Original license and notice files shipped by dependency projects remain authoritative. Source links and complete coordinates are recorded in the generated inventory where Maven metadata supplies them.
