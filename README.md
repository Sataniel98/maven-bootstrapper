# Maven Bootstrapper
This is a simple bootstrapper for Maven projects.

Just dump mvnbt.jar (and build.bat) into the root directory of any Maven project.

## Requirements
The only requirement is JDK 8. The project to build may still demand a higher version. Maven doesn't need to be pre-installed (which is the entire point of this).

Developers should remember to add `*apache-maven-*` to their .gitignore.

## Flags
`--version`, `-v`: Specifies the version of Maven to use. Default is the latest found.

`--goal`, `-g`: Specifies the Maven goal (package/install). Default is "package".

`--dir`, `-d`: Specifies the directory of the pom.xml file. Default is the directory the bootstrapper is in.

Example: `java -jar mvnbt.jar -v 3.9.14 -g install -d api`

## License
This tool is dedicated to the public domain. See https://creativecommons.org/publicdomain/zero/1.0/ for details.
