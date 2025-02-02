package org.ilghar.handler;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
public class MemcachedHandler {

    @Value("${memcached.host}")
    private String memcachedHost;

    @Value("${memcached.port}")
    private int memcachedPort;

    private MemcachedClient memcachedClient;

    // Initialize the Memcached client connection
    public void memcachedConnect() throws Exception {
        if (this.memcachedClient == null || this.memcachedClient.getAvailableServers().isEmpty()) {
            this.memcachedClient = new MemcachedClient(
                    new InetSocketAddress(memcachedHost, memcachedPort)
            );
            System.out.println("Connected to Memcached server at " + memcachedHost + ":" + memcachedPort);
        } else {
            System.out.println("Memcached client is already connected!");
        }
    }

    // Shutdown the Memcached connection
    public void memcachedShutdown() {
        if (this.memcachedClient != null) {
            this.memcachedClient.shutdown();
            System.out.println("Memcached connection shut down successfully.");
        } else {
            System.out.println("Memcached client is not initialized or already shut down.");
        }
    }

    // Add data to Memcached
    public String memcachedAddData(String key, String value, int expiration) {
        if (this.memcachedClient == null) {
            return "Memcached client is not connected!";
        }

        this.memcachedClient.set(key, expiration, value);
        return "Key = " + key + " added to cache with value = " + value;
    }

    public boolean checkKeyExists(String key){
        return this.memcachedClient.get(key) != null;
    }

    // Get data from Memcached
    public String memcachedGetData(String key) {
        if (this.memcachedClient == null) {
            throw new IllegalStateException("Memcached client is not connected!");
        }
        return (String) this.memcachedClient.get(key);
    }


    // Delete data from Memcached
    public String memcachedDelete(String key) {
        if (this.memcachedClient == null) {
            return "Memcached client is not connected!";
        }

        this.memcachedClient.delete(key);
        return "Cache entry deleted for key: " + key;
    }
}