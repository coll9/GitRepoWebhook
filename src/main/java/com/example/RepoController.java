package com.example;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class RepoController {

	Logger logger = LoggerFactory.getLogger(RepoController.class);

	private final String BASEURL = "https://api.github.com/repos/";
	private final String BRANCHAPI = "/branches/master/protection";
	private final String ISSUEAPI = "/issues";

	@Value("${git.user}")
	private String gitUser;
	@Value("${git.credential}")
	private String gitCredential;

	@Value("${sample.testrepo}")
	private String sampleTestRepo;

	/**
	 * webhook for git repo creation. when repo is created the master branch should be protected
	 * and a issue to be created with @mention tag
	 * 
	 * @param payload
	 * @return
	 */
	@PostMapping(path = "/repo", consumes = "application/json")
	public String azure2home(@RequestBody String payload) {
		JSONObject payloadJSONObj = new JSONObject(payload);
		String action = payloadJSONObj.getString("action");
		String restrictedBranchDetails = "";

		JSONObject repo = payloadJSONObj.getJSONObject("repository");
		String repoName = repo.getString("full_name");

		JSONObject sender = payloadJSONObj.getJSONObject("sender");
		String login = sender.getString("login");

		logger.info("action -> " + action);
		logger.info("full_name -> " + repoName);
		logger.info("login -> " + login);

		/*
		 * hack - sleep for 5 sec so that master branch is initialized 
		 * better solution can be to keep checking for master branch ref
		 */
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (action.equals("created")) {
			restrictedBranchDetails = restrictedMasterBranch(repoName);
			createIssue(repoName, login, restrictedBranchDetails);
		}

		return HttpStatus.CREATED.toString();
	}

	private String restrictedMasterBranch(String repoName) {
		StringBuffer response = new StringBuffer();

		String reqStr = "{\"required_status_checks\": {\"strict\": false, \"contexts\": [" + "      \"" + repoName
				+ "\"" + "    ]  },\"enforce_admins\": true,"
				+ "	\"required_pull_request_reviews\": {\"dismiss_stale_reviews\": false,"
				+ "        \"require_code_owner_reviews\": false},\"restrictions\": {"
				+ "    \"users\": [ ],   \"teams\": []," + "    \"apps\": []}}";

		String url = BASEURL + repoName + BRANCHAPI;

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(gitUser, gitCredential);
		HttpEntity<String> httpEntity = new HttpEntity<String>(reqStr, headers);

		ResponseEntity<String> responseEntityStr = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, String.class);
		String respStr = responseEntityStr.getBody();
		// String respStr = testjson1;
		JSONObject respJsonObj = new JSONObject(respStr);
		JSONObject obj1 = respJsonObj.getJSONObject("enforce_admins");
		boolean is_enforce_admins = obj1.getBoolean("enabled");
		response.append("enforce_admins : " + is_enforce_admins + " | ");

		obj1 = respJsonObj.getJSONObject("required_linear_history");
		boolean is_required_linear_history = obj1.getBoolean("enabled");
		response.append("required_linear_history : " + is_required_linear_history + " | ");

		obj1 = respJsonObj.getJSONObject("allow_force_pushes");
		boolean is_allow_force_pushes = obj1.getBoolean("enabled");
		response.append("allow_force_pushes : " + is_allow_force_pushes + " | ");

		logger.info(respStr);
		obj1 = respJsonObj.getJSONObject("allow_deletions");
		boolean is_allow_deletions = obj1.getBoolean("enabled");
		response.append("allow_deletions : " + is_allow_deletions);

		logger.info("restrict repo json request - " + reqStr);

		logger.info("values of restrictions -> " + response.toString());

		return response.toString();
	}

	/*
	 * create issue in the git repo
	 */
	private void createIssue(String repoName, String login, String restr) {
		String url = BASEURL + repoName + ISSUEAPI;
		JSONObject requestJsonObj = new JSONObject();
		requestJsonObj.put("title", "Issue created upon Repo creation");
		requestJsonObj.put("body", "Notifying to @" + login + "\n" + restr);

		String[] assignees = { login };
		requestJsonObj.put("assignees", assignees);

		logger.info("issue request json -> " + requestJsonObj.toString());

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(gitUser, gitCredential);
		HttpEntity<String> httpEntity = new HttpEntity<String>(requestJsonObj.toString(), headers);
		ResponseEntity<String> responseEntityStr = restTemplate.postForEntity(url, httpEntity, String.class);
		logger.info(responseEntityStr.getBody());

	}

	/*
	 * to test the git apis on sample git repo
	 */
	@GetMapping("/testApi")
	public String testApi() {

		String s = restrictedMasterBranch(sampleTestRepo);
		createIssue(sampleTestRepo, gitUser, s);

		return "testing git api suceeded";
	}

	/*
	 * to test the sample repo event payload. check fo the action
	 */
	@GetMapping("/testRepoPayload")
	public String testRepoPayload() {

		JSONObject payloadJSONObj = new JSONObject(testjson);
		String action = payloadJSONObj.getString("action");

		JSONObject repo = payloadJSONObj.getJSONObject("repository");
		String repoName = repo.getString("full_name");

		JSONObject sender = payloadJSONObj.getJSONObject("sender");
		String login = sender.getString("login");

		return "Action -> " + action + " | Repo Name -> " + repoName + " | Login - " + login;
	}

	/*
	 * to test the connectivity with the server
	 */
	@GetMapping("/testConn")
	public String testConn() {
		logger.info("user - " + gitUser);
		logger.info("credentials - " + gitCredential);
		return "Connection Successfull : in cloud";
	}

	String testjson = "{\"action\":\"created\",\"repository\":{\"id\":233212626,\"node_id\":\"MDEwOlJlcG9zaXRvcnkyMzMyMTI2MjY=\",\"name\":\"sample1\",\"full_name\":\"coll9/sample1\",\"private\":false,\"owner\":{\"login\":\"coll9\",\"id\":59756663,\"node_id\":\"MDEyOk9yZ2FuaXphdGlvbjU5NzU2NjYz\",\"avatar_url\":\"https://avatars3.githubusercontent.com/u/59756663?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/coll9\",\"html_url\":\"https://github.com/coll9\",\"followers_url\":\"https://api.github.com/users/coll9/followers\",\"following_url\":\"https://api.github.com/users/coll9/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/coll9/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/coll9/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/coll9/subscriptions\",\"organizations_url\":\"https://api.github.com/users/coll9/orgs\",\"repos_url\":\"https://api.github.com/users/coll9/repos\",\"events_url\":\"https://api.github.com/users/coll9/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/coll9/received_events\",\"type\":\"Organization\",\"site_admin\":false},\"html_url\":\"https://github.com/coll9/sample1\",\"description\":null,\"fork\":false,\"url\":\"https://api.github.com/repos/coll9/sample1\",\"forks_url\":\"https://api.github.com/repos/coll9/sample1/forks\",\"keys_url\":\"https://api.github.com/repos/coll9/sample1/keys{/key_id}\",\"collaborators_url\":\"https://api.github.com/repos/coll9/sample1/collaborators{/collaborator}\",\"teams_url\":\"https://api.github.com/repos/coll9/sample1/teams\",\"hooks_url\":\"https://api.github.com/repos/coll9/sample1/hooks\",\"issue_events_url\":\"https://api.github.com/repos/coll9/sample1/issues/events{/number}\",\"events_url\":\"https://api.github.com/repos/coll9/sample1/events\",\"assignees_url\":\"https://api.github.com/repos/coll9/sample1/assignees{/user}\",\"branches_url\":\"https://api.github.com/repos/coll9/sample1/branches{/branch}\",\"tags_url\":\"https://api.github.com/repos/coll9/sample1/tags\",\"blobs_url\":\"https://api.github.com/repos/coll9/sample1/git/blobs{/sha}\",\"git_tags_url\":\"https://api.github.com/repos/coll9/sample1/git/tags{/sha}\",\"git_refs_url\":\"https://api.github.com/repos/coll9/sample1/git/refs{/sha}\",\"trees_url\":\"https://api.github.com/repos/coll9/sample1/git/trees{/sha}\",\"statuses_url\":\"https://api.github.com/repos/coll9/sample1/statuses/{sha}\",\"languages_url\":\"https://api.github.com/repos/coll9/sample1/languages\",\"stargazers_url\":\"https://api.github.com/repos/coll9/sample1/stargazers\",\"contributors_url\":\"https://api.github.com/repos/coll9/sample1/contributors\",\"subscribers_url\":\"https://api.github.com/repos/coll9/sample1/subscribers\",\"subscription_url\":\"https://api.github.com/repos/coll9/sample1/subscription\",\"commits_url\":\"https://api.github.com/repos/coll9/sample1/commits{/sha}\",\"git_commits_url\":\"https://api.github.com/repos/coll9/sample1/git/commits{/sha}\",\"comments_url\":\"https://api.github.com/repos/coll9/sample1/comments{/number}\",\"issue_comment_url\":\"https://api.github.com/repos/coll9/sample1/issues/comments{/number}\",\"contents_url\":\"https://api.github.com/repos/coll9/sample1/contents/{+path}\",\"compare_url\":\"https://api.github.com/repos/coll9/sample1/compare/{base}...{head}\",\"merges_url\":\"https://api.github.com/repos/coll9/sample1/merges\",\"archive_url\":\"https://api.github.com/repos/coll9/sample1/{archive_format}{/ref}\",\"downloads_url\":\"https://api.github.com/repos/coll9/sample1/downloads\",\"issues_url\":\"https://api.github.com/repos/coll9/sample1/issues{/number}\",\"pulls_url\":\"https://api.github.com/repos/coll9/sample1/pulls{/number}\",\"milestones_url\":\"https://api.github.com/repos/coll9/sample1/milestones{/number}\",\"notifications_url\":\"https://api.github.com/repos/coll9/sample1/notifications{?since,all,participating}\",\"labels_url\":\"https://api.github.com/repos/coll9/sample1/labels{/name}\",\"releases_url\":\"https://api.github.com/repos/coll9/sample1/releases{/id}\",\"deployments_url\":\"https://api.github.com/repos/coll9/sample1/deployments\",\"created_at\":\"2020-01-11T10:15:54Z\",\"updated_at\":\"2020-01-11T10:15:54Z\",\"pushed_at\":null,\"git_url\":\"git://github.com/coll9/sample1.git\",\"ssh_url\":\"git@github.com:coll9/sample1.git\",\"clone_url\":\"https://github.com/coll9/sample1.git\",\"svn_url\":\"https://github.com/coll9/sample1\",\"homepage\":null,\"size\":0,\"stargazers_count\":0,\"watchers_count\":0,\"language\":null,\"has_issues\":true,\"has_projects\":true,\"has_downloads\":true,\"has_wiki\":true,\"has_pages\":false,\"forks_count\":0,\"mirror_url\":null,\"archived\":false,\"disabled\":false,\"open_issues_count\":0,\"license\":null,\"forks\":0,\"open_issues\":0,\"watchers\":0,\"default_branch\":\"master\"},\"organization\":{\"login\":\"coll9\",\"id\":59756663,\"node_id\":\"MDEyOk9yZ2FuaXphdGlvbjU5NzU2NjYz\",\"url\":\"https://api.github.com/orgs/coll9\",\"repos_url\":\"https://api.github.com/orgs/coll9/repos\",\"events_url\":\"https://api.github.com/orgs/coll9/events\",\"hooks_url\":\"https://api.github.com/orgs/coll9/hooks\",\"issues_url\":\"https://api.github.com/orgs/coll9/issues\",\"members_url\":\"https://api.github.com/orgs/coll9/members{/member}\",\"public_members_url\":\"https://api.github.com/orgs/coll9/public_members{/member}\",\"avatar_url\":\"https://avatars3.githubusercontent.com/u/59756663?v=4\",\"description\":null},\"sender\":{\"login\":\"bhakat29\",\"id\":52036787,\"node_id\":\"MDQ6VXNlcjUyMDM2Nzg3\",\"avatar_url\":\"https://avatars0.githubusercontent.com/u/52036787?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/bhakat29\",\"html_url\":\"https://github.com/bhakat29\",\"followers_url\":\"https://api.github.com/users/bhakat29/followers\",\"following_url\":\"https://api.github.com/users/bhakat29/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/bhakat29/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/bhakat29/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/bhakat29/subscriptions\",\"organizations_url\":\"https://api.github.com/users/bhakat29/orgs\",\"repos_url\":\"https://api.github.com/users/bhakat29/repos\",\"events_url\":\"https://api.github.com/users/bhakat29/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/bhakat29/received_events\",\"type\":\"User\",\"site_admin\":false}}";
	String testjson1 = "{\"url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection\",\"required_status_checks\":{\"url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/required_status_checks\",\"strict\":false,\"contexts\":[\"coll9/sample2\"],\"contexts_url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/required_status_checks/contexts\"},\"restrictions\":{\"url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/restrictions\",\"users_url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/restrictions/users\",\"teams_url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/restrictions/teams\",\"apps_url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/restrictions/apps\",\"users\":[],\"teams\":[],\"apps\":[]},\"required_pull_request_reviews\":{\"url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/required_pull_request_reviews\",\"dismiss_stale_reviews\":false,\"require_code_owner_reviews\":false},\"enforce_admins\":{\"url\":\"https://api.github.com/repos/coll9/sample2/branches/master/protection/enforce_admins\",\"enabled\":true},\"required_linear_history\":{\"enabled\":false},\"allow_force_pushes\":{\"enabled\":false},\"allow_deletions\":{\"enabled\":false}}";
}
