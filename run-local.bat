@echo off
REM Runs the app with an in-memory database and the mock scraper (no PostgreSQL / PhantomBuster needed).
set DB_URL=jdbc:h2:mem:alumni
set DB_USERNAME=sa
set DB_PASSWORD=
set PHANTOMBUSTER_MOCK=true
mvn spring-boot:run
