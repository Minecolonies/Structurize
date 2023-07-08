package patches.projects

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.GitHubIssueTracker
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the root project
accordingly, and delete the patch script.
*/
changeProject(DslContext.projectId) {
    params {
        expect {
            param("Current Minecraft Version", "1.20")
        }
        update {
            param("Current Minecraft Version", "main")
        }
    }

    features {
        val feature1 = find<GitHubIssueTracker> {
            githubIssues {
                id = "PROJECT_EXT_22"
                displayName = "ldtteam/structurize"
                repositoryURL = "https://github.com/ldtteam/structurize"
                authType = accessToken {
                    accessToken = "credentialsJSON:47381468-aceb-4992-93c9-1ccd4d7aa67f"
                }
            }
        }
        feature1.apply {
        }
    }
}
