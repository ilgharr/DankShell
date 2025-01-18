package org.ilghar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    // Inject the Memcached component
//    @Autowired
//    private Memcached memcached;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    // Logic will execute after application startup
//    @Override
//    public void run(String... args) throws Exception {
//        memcached.memcachedConnect();
//    }
}