pipeline {
    parameters {
        string(name: 'tag_name', defaultValue: '')
        string(name: 'mailto', defaultValue: '' )
    }
    agent {
        node {
            label 'slave'
            customWorkspace "/data/jenkins/workspace/"
        }
    }
    stages {
        stage('Clone code from gitlab') {
            steps {
                checkout([$class: 'GitSCM',
                branches: [[name: "${tag_name}"]],
                userRemoteConfigs: [[
                    credentialsId: 'xxx-xxx-xxx',
                    refspec: "+refs/heads/*:refs/remotes/origin/*",
                    url: 'git@git.example.com:example.git']]
                ])
            }
        }
        stage('Static code analysis') {
            steps {
                withSonarQubeEnv('my-sonarqube') {
                    withMaven(maven: 'M3', mavenSettingsConfig: 'maven-settings') {
                        sh 'mvn sonar:sonar -Dmaven.test.skip=true -Dsonar.projectKey={{ PROJECK_KEY }} -Dsonar.host.url={{ SONAR_URL }} -Dsonar.login={{ TOKEN }}'
                    }
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
            post {
                always {
                    emailext subject: "[Code analysis report] -- {{ PROJECK_KEY }}",
                        body: '''
                            查看详细的报告, 请点击下方链接:<br>
                            <h2>
                                <a href="{{ SONAR_URL }}/dashboard?id={{ PROJECK_KEY }}">{{ SONAR_URL }}/dashboard?id={{ PROJECK_KEY }}</a>
                            </h2>
                            ''',
                        from: "from@163.com",
                        to: "${mailto}"
                }
            }
        }
    }
}
