package org.gradlex.javamodule.packaging.test


import org.gradlex.javamodule.packaging.test.fixture.GradleBuild
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.hostOs
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnLinux
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnMacos
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnWindows

class JavaModulePackagingTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "can use plugin on #os with success=#success"() {
        given:
        def taskToRun = ":app:jpackage"
        def taskToCheck = ":app:jpackage${label.capitalize()}"
        def macosArch = System.getProperty('os.arch').contains('aarch') ? 'aarch64' : 'x86-64'
        appBuildFile << """
            version = "1.0"
            javaModulePackaging {
                target("macos") {
                    operatingSystem = "macos"
                    architecture = "$macosArch"
                }
                target("ubuntu") {
                    operatingSystem = "linux"
                    architecture = "x86-64"
                }
                target("windows") {
                    operatingSystem = "windows"
                    architecture = "x86-64"
                }
            }
        """
        appModuleInfoFile << '''
            module org.example.app {
            }
        '''

        when:
        def result = success ? build(taskToRun) : fail(taskToRun)

        then:
        result.task(taskToCheck).outcome == (success ? SUCCESS : FAILED)
        success || result.output.contains("> Running on ${hostOs()}; cannot build for $os")

        where:
        label     | os        | success
        'windows' | 'windows' | runsOnWindows()
        'macos'   | 'macos'   | runsOnMacos()
        'ubuntu'  | 'linux'   | runsOnLinux()
    }
}
