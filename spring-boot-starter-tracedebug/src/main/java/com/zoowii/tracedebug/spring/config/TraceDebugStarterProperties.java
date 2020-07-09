package com.zoowii.tracedebug.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;

@ConfigurationProperties(prefix = "tracedebug")
public class TraceDebugStarterProperties {

    public static class DataSourceProp {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
    private DataSourceProp datasource;
    private String moduleId;

    public DataSourceProp getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSourceProp datasource) {
        this.datasource = datasource;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
    
}