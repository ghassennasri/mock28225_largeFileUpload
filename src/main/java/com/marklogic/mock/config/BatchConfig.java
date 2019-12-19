package com.marklogic.mock.config;


import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.ext.helper.DatabaseClientProvider;
import com.marklogic.mock.model.Employee;
import com.marklogic.mock.processor.EmployeeItemProcessor;
import com.marklogic.mock.processor.MarkLogicSimpleWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



import javax.sql.DataSource;


@Configuration
@EnableBatchProcessing
public class BatchConfig {
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource dataSource;

	@Bean
	public DatabaseClientProvider getDatabaseClientProvider(){
		return()->{
			return DatabaseClientFactory.newClient("localhost", 8011,
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
	public EmployeeItemProcessor processor(){
		return new EmployeeItemProcessor();
	}
	
	@Bean
	public MarkLogicSimpleWriter writer(DatabaseClientProvider databaseClientProvider){
		return new MarkLogicSimpleWriter(databaseClientProvider.getDatabaseClient());
	}
	
	@Bean
	public Step step1(DatabaseClientProvider databaseClientProvider){
		DatabaseClient databaseClient = databaseClientProvider.getDatabaseClient();

		return stepBuilderFactory.get("step1").<Employee,DocumentWriteOperation>chunk(1000).reader(reader()).processor(processor()).writer(writer(databaseClientProvider)).build();
	}

	@Bean
	public Job exportPerosnJob(DatabaseClientProvider databaseClientProvider){
		return jobBuilderFactory.get("exportEmployeeJob").incrementer(new RunIdIncrementer()).flow(step1(databaseClientProvider)).end().build();
	}
}
