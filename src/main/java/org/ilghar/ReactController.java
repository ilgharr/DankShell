package org.ilghar;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReactController {

    @GetMapping(value={"/", "/home"})
    public String serveReactApp() {
        return "index.html";
    }
}
