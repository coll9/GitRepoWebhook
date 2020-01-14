# Code to understand the GitHub webhook 
##### This is an example to demonstrate creation of webHooks in github using the GitHub V3 REST API
  - git webhook is called when Repo is created
  - modify the repo properties (restricted repository)
  - create an issue
  - also has couple of test rest api 
  
##### About the application -
  This is a SpringBoot web application exposing REST api to be consumed by the GitHub webhook
  
##### Instructions to use this code
1. Download or Clone the repository
2. Update the application.properties with git user name, user token (generated from GitHub)
    ```
    git.user=<user>
    git.credential=<token>
    ```
3. Run the application - 
  `mvnw spring-boot:run`
  
The URL for the github webhook should be
`http://<servername:port>/repo`

##### Links to API documentation 
[GitHub API Documentation Home Page](https://developer.github.com/v3/auth/)

[GitHub Generate token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line)

[GitHub Create an issue](https://developer.github.com/v3/issues/#create-an-issue)

[GitHub Organization Permission](https://help.github.com/en/github/setting-up-and-managing-organizations-and-teams/repository-permission-levels-for-an-organization)



