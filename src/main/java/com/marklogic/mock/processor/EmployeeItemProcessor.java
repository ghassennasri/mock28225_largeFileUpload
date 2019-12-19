package com.marklogic.mock.processor;


import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.client.io.marker.DocumentMetadataWriteHandle;
import com.marklogic.mock.model.Employee;
import org.springframework.batch.item.ItemProcessor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.UUID;

public class EmployeeItemProcessor implements ItemProcessor<Employee, DocumentWriteOperation> {

    @Override
    public DocumentWriteOperation process(Employee employee) throws Exception {
        DocumentWriteOperation dwo = new DocumentWriteOperation() {

            @Override
            public OperationType getOperationType() {
                return OperationType.DOCUMENT_WRITE;
            }

            @Override
            public String getUri() {
                return UUID.randomUUID().toString() + ".xml";
            }

            @Override
            public DocumentMetadataWriteHandle getMetadata() {
                DocumentMetadataHandle metadata = new DocumentMetadataHandle();
                metadata.withCollections("mysqlImport");
                return metadata;
            }

            @Override
            public AbstractWriteHandle getContent() {
                JAXBContext jaxbContext = null;
                String xmlContent="";
                try {
                    jaxbContext = JAXBContext.newInstance(Employee.class);
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                    StringWriter sw = new StringWriter();
                    jaxbMarshaller.marshal(employee, sw);
                    xmlContent = sw.toString();
                } catch (JAXBException e) {
                    e.printStackTrace();
                }

                return new StringHandle(xmlContent);
            }

            @Override
            public String getTemporalDocumentURI() {
                return null;
            }
        };
        return dwo;
    }
}
