package com.nongfenqi.nexus.plugin.rundeck;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RundeckXO {
    private String name;
    private String value;
}
