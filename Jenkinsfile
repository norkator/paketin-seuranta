pipeline {
  agent any
  // agent {
  //   // Run on a build agent where we have the Android SDK installed
  //   label 'android'
  // }
  environment {
    // Specified for Jenkins server
    JAVA_HOME = "C:/Program Files/Java/jre1.8.0_261"
    ANDROID_SDK_ROOT = "C:/Android/Sdk"
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
  }
  stages {
    stage('Google Services Json') {
      steps {
        bat 'copy "C:\\Projects\\PaketinSeuranta\\google-services.json %WORKSPACE%\\'
      }
    }
    stage('Compile') {
      steps {
        // Compile the app and its dependencies
        bat './gradlew compileDebugSources'
      }
    }
    stage('Unit test') {
      steps {
        // Compile and run the unit tests for the app and its dependencies
        bat './gradlew testDebugUnitTest testDebugUnitTest'

        // Analyse the test results and update the build result as appropriate
        junit '**/TEST-*.xml'
      }
    }
    stage('Build APK') {
      steps {
        // Finish building and packaging the APK
        bat './gradlew assembleDebug'

        // Archive the APKs so that they can be downloaded from Jenkins
        archiveArtifacts '**/*.apk'
      }
    }
    // stage('Static analysis') {
    //   steps {
    //     // Run Lint and analyse the results
    //     bat './gradlew lintDebug'
    //     androidLint pattern: '**/lint-results-*.xml'
    //   }
    // }
    // stage('Deploy') {
    //   when {
    //     // Only execute this stage when building from the `beta` branch
    //     branch 'beta'
    //   }
    //   environment {
    //     // Assuming a file credential has been added to Jenkins, with the ID 'my-app-signing-keystore',
    //     // this will export an environment variable during the build, pointing to the absolute path of
    //     // the stored Android keystore file.  When the build ends, the temporarily file will be removed.
    //     SIGNING_KEYSTORE = credentials('my-app-signing-keystore')

    //     // Similarly, the value of this variable will be a password stored by the Credentials Plugin
    //     SIGNING_KEY_PASSWORD = credentials('my-app-signing-password')
    //   }
    //   steps {
    //     // Build the app in release mode, and sign the APK using the environment variables
    //     bat './gradlew assembleRelease'

    //     // Archive the APKs so that they can be downloaded from Jenkins
    //     archiveArtifacts '**/*.apk'

    //     // Upload the APK to Google Play
    //     androidApkUpload googleCredentialsId: 'Google Play', apkFilesPattern: '**/*-release.apk', trackName: 'beta'
    //   }
    //   post {
    //     success {
    //       mail to: 'nitramite@outlook.com', subject: 'Jenkins - Paketin Seuranta', body: 'New version uploaded!'
    //     }
    //   }
    // }
  }
  // post {
  //   failure {
  //     mail to: 'nitramite@outlook.com', subject: 'Jenkins - Paketin Seuranta', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}"
  //   }
  // }
}
