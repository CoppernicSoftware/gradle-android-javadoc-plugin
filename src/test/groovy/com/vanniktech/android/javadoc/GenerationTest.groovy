package com.vanniktech.android.javadoc

import com.android.build.gradle.internal.SdkHandler
import com.vanniktech.android.javadoc.extensions.AndroidJavadocExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

class GenerationTest {
    @Rule public ExpectedException expectedException = ExpectedException.none()
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    def generation
    def project

    @Before
    void setUp() {
        generation = new Generation()
        project = ProjectBuilder.builder().withName('project').build()
        copyManifest()
    }

    @Test
    void testNullProject() throws Exception {
        expectedException.expect(UnsupportedOperationException.class)
        expectedException.expectMessage('Project is null')

        generation.apply(null)
    }

    @Test
    void testThatExtensionIsAdded() {
        project.plugins.apply('com.android.application')
        generation.apply(project)

        assert project.androidJavadoc instanceof AndroidJavadocExtension
        assert project.androidJavadoc.variantFilter instanceof Closure
        assert project.androidJavadoc.taskNameTransformer instanceof Closure
        assert project.androidJavadoc.outputDir instanceof Closure
    }

    @Test
    void testNotAndroidProject() {
        generation.apply(project)

        assert !project.hasProperty("generateReleaseJavadoc")
        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateReleaseJavadocJar")
        assert !project.hasProperty("generateDebugJavadocJar")
    }

    @Test
    void testJavaProject() {
        project.plugins.apply('java')
        generation.apply(project)

        assert !project.hasProperty("generateReleaseJavadoc")
        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateReleaseJavadocJar")
        assert !project.hasProperty("generateDebugJavadocJar")
    }

    @Test
    void testAndroidAppProject() {
        doNotRunOnTravis()
        withAndroidAppProject()

        // These tasks are only added after project.afterEvaluated() is called.
        assert !project.hasProperty("generateReleaseJavadoc")
        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateReleaseJavadocJar")
        assert !project.hasProperty("generateDebugJavadocJar")

        project.evaluate()

        assert project.generateReleaseJavadoc instanceof Javadoc
        assert project.generateDebugJavadoc instanceof Javadoc
        assert project.generateReleaseJavadocJar instanceof Jar
        assert project.generateDebugJavadocJar instanceof Jar
    }


    @Test
    void testAndroidLibraryProject() {
        doNotRunOnTravis()
        withAndroidLibProject()

        // These tasks are only added after project.afterEvaluated() is called.
        assert !project.hasProperty("generateReleaseJavadoc")
        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateReleaseJavadocJar")
        assert !project.hasProperty("generateDebugJavadocJar")

        project.evaluate()

        assert project.generateReleaseJavadoc instanceof Javadoc
        assert project.generateDebugJavadoc instanceof Javadoc
        assert project.generateReleaseJavadocJar instanceof Jar
        assert project.generateDebugJavadocJar instanceof Jar
    }

    @Test
    void filterVariant() {
        doNotRunOnTravis()
        withAndroidAppProject()

        project.androidJavadoc.variantFilter { variant ->
            variant.buildType.name == "release"
        }

        project.evaluate()

        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateDebugJavadocJar")
        assert project.generateReleaseJavadoc instanceof Javadoc
        assert project.generateReleaseJavadocJar instanceof Jar
    }

    @Test
    void transformTaskName() {
        doNotRunOnTravis()
        withAndroidAppProject()

        project.androidJavadoc.taskNameTransformer { variant ->
            "yeah"
        }

        project.evaluate()

        assert !project.hasProperty("generateReleaseJavadoc")
        assert !project.hasProperty("generateDebugJavadoc")
        assert !project.hasProperty("generateReleaseJavadocJar")
        assert !project.hasProperty("generateDebugJavadocJar")
        assert project.generateYeahJavadoc instanceof Javadoc
        assert project.generateYeahJavadocJar instanceof Jar
    }

    private void withAndroidAppProject() {
        applyAndroidPlugin('com.android.application')
        generation.apply(project)
    }

    private void withAndroidLibProject() {
        applyAndroidPlugin('com.android.library')
        generation.apply(project)
    }

    private void applyAndroidPlugin(String plugin) {
        //SdkHandler.setTestSdkFolder(temporaryFolder.newFolder())
        project.plugins.apply(plugin)
        project.android.compileSdkVersion 27
        project.android.buildToolsVersion "27.0.3"
        project.android.defaultConfig {
            minSdkVersion 17
            targetSdkVersion 27
            versionCode 1
            versionName "dev"

            testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        }
    }

    private void copyManifest() {
        Path source = Paths.get("./src/test/res/AndroidManifest.xml").toAbsolutePath().normalize()
        Path dest = Paths.get(project.projectDir.getPath(), "src/main")
        dest.toFile().mkdirs()
        dest = Paths.get(dest.toString(), "AndroidManifest.xml")
        dest.toFile() << source.toFile().text
    }

    private static void doNotRunOnTravis() { // Unless we know how to have a good ANDROID_HOME env var on travis.
        String res = System.getenv("TRAVIS")
        if (res != null) {
            Assume.assumeFalse(res == "true")
        }
    }
}
