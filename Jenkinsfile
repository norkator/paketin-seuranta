pipeline {
  agent any
  // agent {
  //   // Run on a build agent where we have the Android SDK installed
  //   label 'android'
  // }
  environment {
    // Specified for Jenkins server
    JAVA_HOME = "C:/Program Files/Android/Android Studio/jre"
    ANDROID_SDK_ROOT = "C:/Android/Sdk"
    GRADLE_USER_HOME = "C:/gradle-cache"
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
  }
  stages {
    stage('Google Services Json') {
      steps {
        bat 'copy C:\\Projects\\PaketinSeuranta\\google-services.json %WORKSPACE%\\app\\'
      }
    }
    stage('Google Maps Api File') {
      steps {
        bat 'mkdir %WORKSPACE%\\app\\src\\release\\res\\values\\'
        bat 'copy C:\\Projects\\PaketinSeuranta\\google_maps_api.xml %WORKSPACE%\\app\\src\\release\\res\\values\\'
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
        // archiveArtifacts '**/*.apk'
      }
    }
    stage('Static analysis') {
      steps {
        // Run Lint and analyse the results
        bat './gradlew lintDebug'
        androidLint pattern: '**/lint-results-*.xml'
      }
    }
    stage('Deploy') {
      when {
        // Only execute this stage when building from the `master` branch
        branch 'feature/android-app-bundle'
      }
      steps {

        // Execute bundle release build
        bat './gradlew bundleRelease'

        // Archive the AAB (Android App Bundle) so that it can be downloaded from Jenkins
        archiveArtifacts '**/*.aab'

        // Upload the APK to Google Play (will upload manually from Jenkins Artifacts)
        // androidApkUpload googleCredentialsId: 'Google Play', apkFilesPattern: '**/*-release.apk', trackName: 'beta'
      }
      // post {
      //   success {
      //     mail to: 'nitramite@outlook.com', subject: 'Jenkins - Paketin Seuranta', body: 'New version uploaded!'
      //   }
      // }
    }
  }
  // post {
  //   failure {
  //     mail to: 'nitramite@outlook.com', subject: 'Jenkins - Paketin Seuranta', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}"
  //   }
  // }
}
