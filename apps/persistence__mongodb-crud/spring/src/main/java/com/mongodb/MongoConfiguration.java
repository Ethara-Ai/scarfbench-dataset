package com.mongodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Spring Data MongoDB stores a {@code _class} discriminator in every document by
 * default. The source variant maps documents with the plain driver codec and writes
 * only {@code _id}, {@code name} and {@code age}, so the type mapper is disabled to
 * keep the persisted state identical across variants.
 */
@Configuration
public class MongoConfiguration {

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory databaseFactory,
            MongoMappingContext mappingContext,
            MongoCustomConversions conversions) {
        MappingMongoConverter converter =
                new MappingMongoConverter(new DefaultDbRefResolver(databaseFactory), mappingContext);
        converter.setCustomConversions(conversions);
        converter.setCodecRegistryProvider(databaseFactory);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.afterPropertiesSet();
        return converter;
    }
}
