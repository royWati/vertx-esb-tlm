package ekenya.co.ke.vertxspringtlm.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 *
 * @version 1.0
 */
@Configuration
@EnableRedisRepositories
public class RedisConfigurations {



    @Bean
    public LettuceConnectionFactory connectionFactory() {

//        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
//        configuration.setPort(6379);
//        configuration.setHostName("10.20.15.50");
//        configuration.setPassword("23TeJKovv7NkWSR");
//
//        return new LettuceConnectionFactory(configuration);
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
