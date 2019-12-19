package com.marklogic.mock.processor;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.ext.batch.RestBatchWriter;
import com.marklogic.client.impl.DocumentWriteOperationImpl;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;


public class MarkLogicSimpleWriter implements ItemWriter<DocumentWriteOperation> {

    protected DatabaseClient client;

    private RestBatchWriter batchWriter;


    public MarkLogicSimpleWriter(DatabaseClient client) {
        this.client = client;
        batchWriter = new RestBatchWriter(client);
        batchWriter.initialize();


    }
    @Override
    public void write(List<? extends DocumentWriteOperation> items) throws Exception {
        List<DocumentWriteOperation> newItems = new ArrayList<>();
        for (DocumentWriteOperation op : items) {
            newItems.add(
                    new DocumentWriteOperationImpl(
                            DocumentWriteOperation.OperationType.DOCUMENT_WRITE,
                            op.getUri(),
                            op.getMetadata(),
                            op.getContent()));
        }
        batchWriter.write(newItems);
    }
}
