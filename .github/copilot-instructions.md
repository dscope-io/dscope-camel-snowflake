<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->
- [x] Verify that the copilot-instructions.md file in the .github directory is created.

- [x] Clarify Project Requirements
	Apache Camel Snowflake component with package name io.dscope.camel.snowflake

 - [x] Scaffold the Project
 	Maven-based Java project with Apache Camel component structure is in place (component: io.dscope.camel:camel-snowflake).

 - [x] Customize the Project
 	Snowflake-specific endpoint, producer, consumer, configuration, parameter binding, auth modes (private key, OAuth), JDBC pass-through, and output formats implemented.

 - [x] Install Required Extensions
 	No additional extensions required; workspace uses standard Java/Camel setup.

 - [x] Compile the Project
 	Project builds successfully; dependencies resolved.

 - [x] Create and Run Task
 	VS Code tasks present for Maven build/test and verified.

 - [x] Launch the Project
 	Debug/run configurations available; component and sample verified via tests and runnable YAML sample under samples/dynamic-query-yaml.

 - [x] Ensure Documentation is Complete
 	README.md, component docs, sample README, and additional guides updated; CHANGELOG includes Unreleased notes.

- [x] Tidy Docs
	Fixed README headings and links, removed unverifiable benchmark/coverage claims, added `privateKeyFile` and OAuth/system properties notes across docs.