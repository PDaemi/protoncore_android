# Usage
Add the desired library as following
```kotlin
implementation("me.proton.core:<name-of-the-lib>:<version>")
```
where `<name-of-the-lib>` reflect the one listed below, on lowercase, with `-` instead of spaces
`Util Android Shared Preferences: **0.1**` can be resolved as `me.proton.core:util-android-share-preferences:0.1`

Where there are all or some of `domain`, `data`, `presentation`, they can be imported together with the parent module.
```kotlin
implementation("~:network-domain:~")
implementation("~:network-data:~")
```
or
```kotlin
implementation("~:network:~")
```
while the first one is suitable for multi-module projects, the latter is the suggested solution for monolithic clients.

## Setup Detekt
In order to use the all-in-one Detekt configuration, you have to:

1. Setup Plugin in your **root** gradle file
    Kotlin Gradle DSL:
    ```kotlin
    plugins {
        id("me.proton.detekt") version protonDetektVersion
    }
    ```
    Regular Groovy script:
    ```groovy
    buildscript {
      
        dependencies {
            classpath("me.proton:detekt:$protonDetektVersion")
        }
    }
    
    apply(plugin = "me.proton.detekt")
    ```
    
2. Setup the following stage in your `.gitlab-ci`

    ```yaml
    stages:
      - analyze
      # - others ...
    
    #####################
    detekt analysis:
      stage: analyze
      tags:
        - android
      script:
        - ./gradlew multiModuleDetekt
      artifacts:
        reports:
          codequality: config/detekt/reports/mergedReport.json
    ```

    


# Last versions

## Plugins

Detekt: **0.4** - _released on: Sep 13, 2021_

Jacoco: **0.1** - _released on: Nvo 10, 2021_

Kotlin: **0.1** - _released on: Oct 09, 2020_

Tests: **0.1** - _released on: Oct 09, 2020_

# Coordinates
me.proton.core:account
me.proton.core:account-data
me.proton.core:account-domain
me.proton.core:account-manager
me.proton.core:account-manager-dagger
me.proton.core:account-manager-data
me.proton.core:account-manager-data-db
me.proton.core:account-manager-domain
me.proton.core:account-manager-presentation
me.proton.core:auth
me.proton.core:auth-data
me.proton.core:auth-domain
me.proton.core:auth-presentation
me.proton.core:contact
me.proton.core:contact-data
me.proton.core:contact-domain
me.proton.core:contact-hilt
me.proton.core:country
me.proton.core:country-data
me.proton.core:country-domain
me.proton.core:country-presentation
me.proton.core:crypto
me.proton.core:crypto-android
me.proton.core:crypto-common
me.proton.core:data
me.proton.core:data-room
me.proton.core:domain
me.proton.core:event-manager
me.proton.core:event-manager-data
me.proton.core:event-manager-domain
me.proton.core:human-verification
me.proton.core:human-verification-data
me.proton.core:human-verification-domain
me.proton.core:human-verification-presentation
me.proton.core:key
me.proton.core:key-data
me.proton.core:key-domain
me.proton.core:mail-message
me.proton.core:mail-message-data
me.proton.core:mail-message-domain
me.proton.core:mail-settings
me.proton.core:mail-settings-data
me.proton.core:mail-settings-domain
me.proton.core:network
me.proton.core:network-data
me.proton.core:network-domain
me.proton.core:payment
me.proton.core:payment-data
me.proton.core:payment-domain
me.proton.core:payment-presentation
me.proton.core:plan
me.proton.core:plan-data
me.proton.core:plan-domain
me.proton.core:plan-presentation
me.proton.core:presentation
me.proton.core:test-android
me.proton.core:test-android-instrumented
me.proton.core:test-kotlin
me.proton.core:user
me.proton.core:user-data
me.proton.core:user-domain
me.proton.core:user-settings
me.proton.core:user-settings-data
me.proton.core:user-settings-domain
me.proton.core:user-settings-presentation
me.proton.core:util-android-shared-preferences
me.proton.core:util-android-work-manager
me.proton.core:util-kotlin
