# Prerequisites
- Docker installation (Docker Desktop or Rancher Desktop or plain Docker daemon)
- Java 11 (Flink 18 doesn't support Java 17 well yet)

# What's inside
- StreamApplication - a draft Flink application
- StreamApplicationTest - an integration test template which
  - deploys a local setup for Kinesis Streams
  - launches the StreamApplication
  - gracefully shuts down the StreamApplication after a timeout
