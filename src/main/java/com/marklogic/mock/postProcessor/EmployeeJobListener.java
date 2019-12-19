package com.marklogic.mock.postProcessor;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.ext.batch.RestBatchWriter;
import com.marklogic.client.impl.DocumentWriteOperationImpl;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class EmployeeJobListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeJobListener.class);

    protected DatabaseClient client;

    private RestBatchWriter batchWriter;
    public EmployeeJobListener(DatabaseClient client){
        this.client=client;
        batchWriter = new RestBatchWriter(client);
        batchWriter.initialize();
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {

    }

    @Override
    public void afterJob(JobExecution jobExecution) {


        if(jobExecution.getStatus() == BatchStatus.COMPLETED){
            LOGGER.info("Employee job terminated, now sending file to Marklogic server");
            DocumentMetadataHandle metadata = new DocumentMetadataHandle();
            metadata.withCollections("mysqlImportLargeFile2");
            StringBuilder content= new StringBuilder();
            try {
                Files.lines(Paths.get("C:\\work\\training\\java\\mock28255\\data.xml"), StandardCharsets.UTF_8).forEach(s->content.append(s));
            } catch (IOException e) {
                LOGGER.info(String.valueOf(e));
            }
            DocumentWriteOperation doc1 = new DocumentWriteOperationImpl(DocumentWriteOperation.OperationType.DOCUMENT_WRITE,
                    UUID.randomUUID().toString() + ".xml",metadata,new StringHandle(content.toString()));

            batchWriter.write(Arrays.asList(doc1));
            batchWriter.waitForCompletion();
            LOGGER.info("Document injested with success into Marklogic DB");

        }else if(jobExecution.getStatus() == BatchStatus.FAILED){
            LOGGER.info("Employee job failed");
            List<Throwable> exceptionList = jobExecution.getAllFailureExceptions();
            for(Throwable th : exceptionList){
                LOGGER.info("exception :" +th.getLocalizedMessage());
            }
        }
    }

    private long getTimeInMillis(Date start, Date stop){
        return stop.getTime()- start.getTime();
    }

}