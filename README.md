# mock28225_largeFileUpload

This is a mock of case 28225, the projects is done using spring boot, spring batch and the community library
marklogic ml-javaclient-util version 3.13.4. Tha application connect 
to the well known Mysql sample database Employees, reads employees table and serialize each tuple into a unique on disk xml document.
The large xml document (~60MB) will be uploaded by the JobEventListener when the job is finished.
The upload uses the class  com.marklogic.client.ext.batch.RestBatchWriter.
The job implemented is single step with a chunk oriented tasklet with commit-interval set at 1000 up to 50000 when testing. 
The objective here was to provoke a java SocketException, but actually it didn't occurred.


### prequisites:
*Both Mysql and Marklogic 9/10 need to be running on localhost The docker-compose file used to test the project 
is included into this repository

* Change the xml file path in spring boot application.properties file

* change the host and port of Marklogic database client into the bean injection in the class com.marklogic.mock.config.BatchConfig 

*Mysql employees sample database need to be imported ( guidelines on https://dev.mysql.com/doc/employee/en/employees-installation.html)

*Set up a Marklogic HTTP server on port 8011
