package io.github.aikobn26.teamprogressviz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeamProgressVizApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamProgressVizApplication.class, args);
	}

}
