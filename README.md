## protoquill-finagle-mysql

![CI](https://github.com/krrrr38/protoquill-finagle-mysql/workflows/CI/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.krrrr38/protoquill-finagle-mysql_2.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.krrrr38%20AND%20a:protoquill-finagle-mysql_3)

Scala3 [zio/zio-protoquill](https://github.com/zio/zio-protoquill) integration with [finagle-mysql](https://twitter.github.io/finagle/). 

## Develop

```sh
> docker compose up -d
> ./setup/setup.sh
> sbt test
```

## Publish

- SNAPSHOT Release
  - Each snapshot release is published by main branch ci.
- Release
  - Push git tag by following command and released by tag ci.

```sh
TAG=0.1.0
git tag -a v$TAG -m "v$TAG"
git push origin v$TAG
```
