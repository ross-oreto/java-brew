### Java Brew
A home-brewed java library.

- Why?
  - Java doesn't always have the nicest syntax and build in utilities to aid developers, so we have to brew up some Java.
  
### Requirements
 - jdk 8+
 - maven 3.3.1+
  
### compiling  
compile all
```bash
mvn compile
```

compile core only
```bash
mvn compiler:compile@core
```

### packaging
package all
```bash
mvn package
```

package core only
```bash
mvn clean compiler:compile@core jar:jar@core
```

### referencing
Use a classifier to specify core only (omit classifier for all)
```xml
<dependency>
    <groupId>io.oreto</groupId>
    <artifactId>java-brew</artifactId>
    <version>1.0-SNAPSHOT</version>
    <classifier>core</classifier>
</dependency>
```

### deploy
```bash
mvn deploy --settings settings.xml -Dtoken=""
```
