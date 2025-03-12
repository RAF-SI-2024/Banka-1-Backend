package com.banka1.banking.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
        @Bean
        public ModelMapper modelMapper() {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT)
                    .setFieldMatchingEnabled(true)
                    .setSkipNullEnabled(true);
            return modelMapper;
        }
}


