# Religious Texts Study Platform

A Vaadin-based application for reading, studying and comparing religious texts
across translations, languages and historical orderings.

## Prerequisites

- Docker Desktop (running)
- IntelliJ IDEA Community with JDK 21 (Eclipse Temurin)

That is all. Java, Maven, MySQL and eXist-db all run in Docker.

## First-time setup

### 1. Start the databases

```bash
cd docker
docker-compose up -d
```

Wait ~60 seconds for both containers to become healthy:

```bash
docker-compose ps
```

Both should show `healthy`.

### 2. Verify eXist-db

Open http://localhost:8080 in your browser.
Login with admin / admin.
You should see the eXist-db dashboard.

### 3. Upload the schema to eXist-db

In the eXist-db dashboard:
- Go to Collections -> Create collection: /db/religioustext
- Upload schema/religious-text.xsd to /db/religioustext

### 4. Open the project in IntelliJ

File -> Open -> select the religious-texts folder (the one containing this README).
IntelliJ will detect the Maven multi-module project automatically.
Let it import and download dependencies (first run takes a few minutes).

### 5. Run the app

Run org.religioustext.app.ReligiousTextsApp
Open http://localhost:8090

## Project structure

```
religious-texts/
  schema/          XSD schema + sample XML files
  docker/          docker-compose.yml, MySQL config, init SQL
  app/             Vaadin reader application (port 8090)
  ingestion/       Ingestion pipeline (port 8091)
  README.md
```

## Ingesting the KJV Bible

Run org.religioustext.ingestion.IngestionApp
Then trigger ingestion via the REST endpoint (documented separately).

## Stopping the databases

```bash
cd docker
docker-compose down
```

Full reset including all data:

```bash
docker-compose down -v
```
