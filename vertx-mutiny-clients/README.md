# Vert.x Mutiny Clients

This project generates the Mutiny variant of the Vert.x API.

## Compatibility Report

To generate the compatibility report, you need:
 
* jbang - https://github.com/jbangdev/jbang 
* asciidoctor - http://asciidoctor.org/
 
Generate the report with:

```bash
mvn clean package revapi:report@revapi-check  -Prevapi -DskipTests -Dmaven.javadoc.skip=true
jbang Main.java && asciidoctor target/compatibility-report.adoc
``` 

The HTML report is available in `target/compatibility-report.html`