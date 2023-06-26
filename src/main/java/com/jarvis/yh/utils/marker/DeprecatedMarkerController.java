package com.jarvis.yh.utils.marker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Configuration
@RestController
@ConditionalOnBean(value = DeprecatedMarker.class)
public class DeprecatedMarkerController {

    @Resource
    private DeprecatedMarker deprecatedMarker;

    @GetMapping(value = "/deprecated/marker/all")
    public Object all() {
        List<String> all = deprecatedMarker.all();
        return all;
    }

    @GetMapping(value = "/deprecated/marker/deprecated")
    public Object deprecated() {
        List<String> deprecated = deprecatedMarker.deprecated();
        return deprecated;
    }
}
