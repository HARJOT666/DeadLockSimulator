package com.deadlocksimulator.enums;

public enum WorkDistribution {
    /** Every cycle member has done the same amount of work. */
    EQUAL,
    /** Work drawn uniformly from [minWork, maxWork]. */
    UNIFORM,
    /** One heavy process per cycle (maxWork); the rest close to minWork. */
    SKEWED
}
