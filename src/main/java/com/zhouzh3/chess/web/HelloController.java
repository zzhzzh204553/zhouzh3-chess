package com.zhouzh3.chess.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/hello")
@RestController
public class HelloController {

    @RequestMapping("/world")
    public String world() {
        return "hello world";
    }


}
