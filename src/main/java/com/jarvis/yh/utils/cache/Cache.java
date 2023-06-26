package com.jarvis.yh.utils.cache;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Cache {

    public static final Logger log = LoggerFactory.getLogger(Cache.class);

    @Resource
    private StringRedisTemplate redis;

    public void delete(String prefix, String key) {
        redis.delete(prefix + key);
    }

    public void delete(String prefix, Long key) {
        delete(prefix,key.toString());
    }

    public void set(String prefix, String key, String value) {
        set(prefix, key, value, 10L, TimeUnit.MINUTES);
    }

    public void set(String prefix, String key, String value, Long timeout, TimeUnit timeUnit) {
        if (StringUtils.isEmpty(prefix + key) || StringUtils.isEmpty(value)) {
            return;
        }
        try {
            redis.opsForValue().set(prefix + key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.error("添加缓存失败,prefix:{},key:{}", prefix, key, e);
        }
    }
    public void set(String prefix, Long key, Object value) {
        set(prefix,key.toString(),JSON.toJSONString(value));
    }

    public void hashSet(String prefix, Map map) {
        try {
            redis.opsForHash().putAll(prefix, map);
        } catch (Exception e) {
            log.error("[hashSet]失败,prefix:{},map:{}", prefix, map, e);
        }
    }

    public String hashGet(String prefix, Object key) {
        try {
            Object o = redis.opsForHash().get(prefix, key);
            if (o != null) {
                return o.toString();
            }
            return null;
        } catch (Exception e) {
            log.error("[hashGet]失败,prefix:{},key:{}", prefix, key, e);
            return null;
        }
    }

    public <T> T get(String prefix, String key, Class<T> clz) {
        try {
            String s = getString(prefix, key);
            if (StringUtils.isEmpty(s)) {
                return null;
            }
            return JSON.parseObject(s, clz);
        } catch (Exception e) {
            log.error("获取缓存失败,prefix:{},key:{}", prefix, key, e);
            return null;
        }
    }

    public <T> T get(String prefix, Long key, Class<T> clz) {
        if (key == null) {
            throw new RuntimeException("key不能为空");
        }
        return get(prefix, key.toString(), clz);
    }

    public String getString(String prefix, String key) {
        try {
            return redis.opsForValue().get(prefix + key);
        } catch (Exception e) {
            log.error("获取缓存失败,prefix:{},key:{}", prefix, key, e);
            return null;
        }
    }

    public String getString(String key) {
        return redis.opsForValue().get(key);
    }

    public StringRedisTemplate getRedis() {
        return redis;
    }

}
