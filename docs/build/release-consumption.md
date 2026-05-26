# Release asset consumption

`1.0.1` release artifacts are GitHub Release assets, not Maven Central publications. The release
workflow uploads a zipped Maven-layout repository plus checksums, release notes, and an artifact
manifest. Consumers must unpack the Maven-layout repository and point Gradle at that local path.

## Local validation command

The repository keeps a deterministic consumer smoke lane:

```bash
./gradlew releaseConsumerSmoke --console=plain
```

The lane stages the approved artifacts, zips and unpacks the Maven-layout repository like the
GitHub Release asset, creates a clean downstream Gradle project, and verifies offline plugin/runtime
resolution plus generated read/write/validate behavior. It does not publish remotely, sign
artifacts, retag a release, or contact Maven Central/package registries.

## Consumer repository sketch

After unpacking the release asset, configure plugin and dependency resolution to use the local
Maven-layout repository:

```groovy
pluginManagement {
    repositories {
        maven { url = uri('/path/to/unpacked/staging-repository') }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri('/path/to/unpacked/staging-repository') }
    }
}
```

Then apply the plugin with the release version:

```groovy
plugins {
    id 'java'
    id 'io.github.mundanej.mxjb' version '1.0.1'
}

mxjb {
    schema('src/main/resources/schema/order.xsd')
    namespacePackage('urn:orders', 'com.example.orders')
    profile = 'XP-XSD10-FULL'
}
```

Maven Central publication, package-registry publication, signing, and remote staging remain
non-claims for the current release process.
