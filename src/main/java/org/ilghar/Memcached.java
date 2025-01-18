package org.ilghar;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;

@RestController
@RequestMapping("/api/memcached")
public class Memcached {

    @Value("${memcached.host}")
    private String memcachedHost;

    @Value("${memcached.port}")
    private int memcachedPort;

//  @Autowired
    private MemcachedClient memcachedClient;

    //does not need api call
    public void memcachedConnect() throws Exception {
        this.memcachedClient = new MemcachedClient(
                new InetSocketAddress(memcachedHost, memcachedPort)
        );
    }

    @PostMapping("/shutdown")
    public String memcachedShutdown() {
        if (this.memcachedClient != null) {
            this.memcachedClient.shutdown();
            return "Memcached connection shut down successfully.";
        }
        return "Memcached client is not initialized or already shut down.";
    }

    @PostMapping("/add")
    public String memcachedAddData(@RequestParam String key) {
        String value = (String) this.memcachedClient.get(key);
        if (value != null) {
            return "Cache hit: Key = " + key + ", Value = " + value;
        } else {
            return "Cache miss for key: " + key;
        }
    }

    @GetMapping("/get")
    public String memcachedGetData(String key) {
        String value = (String) this.memcachedClient.get(key);
        if (value != null) {
            return "Cache hit: Key = " + key + ", Value = " + value;
        } else {
            return "Cache miss for key: " + key;
        }
    }

    @DeleteMapping("/delete")
    public String memcachedDelete(@RequestParam String key) {
        this.memcachedClient.delete(key);
        return "Cache entry deleted for key: " + key;
    }
}