package com.ezreal.security.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Rule {
    private boolean enable;
    private String path;
    private int windowPeriod;
    private int windowSize;
}
