package com.zoowii.tracedebug.components;

import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Component;

@Component
public class AppEhcacheManager extends EhCacheCacheManager {
    @Override
    protected Cache getMissingCache(String name) {
        Cache cache = super.getMissingCache(name);
        if (cache == null) {
            Ehcache ehcache = super.getCacheManager().addCacheIfAbsent(name);
            cache = new EhCacheCache(ehcache);
        }
        return cache;
    }
}
