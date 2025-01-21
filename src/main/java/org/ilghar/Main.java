package org.ilghar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main{

//    @Autowired
//    public Memcached memcached;

    public static void main(String[] args) throws Exception {

        SpringApplication.run(Main.class, args);
//        testToken();
    }

//    @Override
//    public void run(String... args) throws Exception {
//        try {
//            memcached.memcachedConnect();
//        } catch (Exception e) {
//            System.err.println("Error connecting to Memcached: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

}