package com.marklogic.mock.config;


import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.ext.helper.DatabaseClientProvider;
import com.marklogic.mock.model.Employee;
import com.marklogic.mock.postProcessor.EmployeeJobListener;
import com.marklogic.mock.processor.EmployeeItemProcessor;
import com.marklogic.mock.processor.MarkLogicSimpleWriter;
import com.marklogic.mock.processor.SimpleEmployeeProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;


import javax.sql.DataSource;


@Configuration
@EnableBatchProcessing
public class BatchConfig {

	private static final String PROPERTY_XML_EXPORT_FILE_PATH = "database.to.xml.job.export.file.path";
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource dataSource;

	@Bean
	public DatabaseClientProvider getDatabaseClientProvider(){
		return()->{
			return DatabaseClientFactory.newClient("gnatestvm.eng.marklogic.com", 8020,
					new DatabaseClientFactory.DigestAuthContext("admin", "admin"));
		};
	}
	@Bean
	public JdbcCursorItemReader<Employee> reader(){
		JdbcCursorItemReader<Employee> cursorItemReader = new JdbcCursorItemReader<>();
		cursorItemReader.setDataSource(dataSource);
		cursorItemReader.setSql("SELECT emp_no,first_name,last_name,gender,birth_date,hire_date FROM employees");
		cursorItemReader.setRowMapper(new EmployeeRowMapper());
		return cursorItemReader;
	}
	@Bean
	ItemWriter<Employee> databaseXmlItemWriter(Environment environment) {
		StaxEventItemWriter<Employee> xmlFileWriter = new StaxEventItemWriter<>();

		String exportFilePath = environment.getRequiredProperty(PROPERTY_XML_EXPORT_FILE_PATH);
		xmlFileWriter.setResource(new FileSystemResource(exportFilePath));

		xmlFileWriter.setRootTagName("Employees");

		Jaxb2Marshaller employeeMarshaller = new Jaxb2Marshaller();
		employeeMarshaller.setClassesToBeBound(Employee.class);
		xmlFileWriter.setMarshaller(employeeMarshaller);

		return xmlFileWriter;
	}
	
	@Bean
	public EmployeeItemProcessor processor(){
		return new EmployeeItemProcessor();
	}
	@Bean
	public SimpleEmployeeProcessor simpleprocessor(){
		return new SimpleEmployeeProcessor();
	}
	@Bean
	public EmployeeJobListener listener(DatabaseClientProvider databaseClientProvider){
		return new EmployeeJobListener(databaseClientProvider.getDatabaseClient());
	}
	
	@Bean
	public MarkLogicSimpleWriter writer(DatabaseClientProvider databaseClientProvider){
		return new MarkLogicSimpleWriter(databaseClientProvider.getDatabaseClient());
	}
	
	/*@Bean
	public Step step1(DatabaseClientProvider databaseClientProvider){
		DatabaseClient databaseClient = databaseClientProvider.getDatabaseClient();

		return stepBuilderFactory.get("step1").<Employee,DocumentWriteOperation>chunk(1000).reader(reader()).processor(processor()).writer(writer(databaseClientProvider)).build();
	}

	@Bean
	public Job exportPerosnJob(DatabaseClientProvider databaseClientProvider){
		return jobBuilderFactory.get("exportEmployeeJob").incrementer(new RunIdIncrementer()).flow(step1(databaseClientProvider)).end().build();
	}*/
	@Bean
	Step databaseToXmlFileStep(ItemReader<Employee> jdbcCursorItemReader,
							   ItemProcessor<Employee, Employee> databaseXmlItemProcessor,
							   ItemWriter<Employee> databaseXmlItemWriter,
							   StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get("databaseToXmlFileStep")
				.<Employee, Employee>chunk(1000)
				.reader(jdbcCursorItemReader)
				.processor(databaseXmlItemProcessor)
				.writer(databaseXmlItemWriter)
				.build();
	}

	@Bean
	Job databaseToXmlFileJob(JobBuilderFactory jobBuilderFactory,
							 @Qualifier("databaseToXmlFileStep") Step employeeStep,DatabaseClientProvider databaseClientProvider) {
		return jobBuilderFactory.get("databaseToXmlFileJob")
				.incrementer(new RunIdIncrementer())
				.listener(listener(databaseClientProvider))
				.flow(employeeStep)
				.end()
				.build();
	}
}
