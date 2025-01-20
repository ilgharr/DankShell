package org.ilghar;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
public class Memcached {

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

    // Get data from Memcached
    public String memcachedGetData(String key) {
        if (this.memcachedClient == null) {
            return "Memcached client is not connected!";
        }

        String value = (String) this.memcachedClient.get(key);
        if (value != null) {
            return "Cache hit: Key = " + key + ", Value = " + value;
        } else {
            return "Cache miss for key: " + key;
        }
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