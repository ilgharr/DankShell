package org.ilghar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.ilghar.handler.MemcachedHandler;

@SpringBootApplication
public class Main implements CommandLineRunner{

    @Autowired
    public MemcachedHandler memcached;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            memcached.memcachedConnect();
        } catch (Exception e) {
            System.err.println("Error connecting to Memcached: " + e.getMessage());
            e.printStackTrace();
        }
    }
}