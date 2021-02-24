package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.sql.DataSourceDefinition;

@Data
@AllArgsConstructor
public class AppInfo {

    private String appId;

    private String key;

}
