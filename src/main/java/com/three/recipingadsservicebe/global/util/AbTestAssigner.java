package com.three.recipingadsservicebe.global.util;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;

public class AbTestAssigner {

    public static AbTestGroup assign(String userId) {
        int hash = Math.abs(userId.hashCode());
        return switch (hash % 3) {
            case 0 -> AbTestGroup.A;
            case 1 -> AbTestGroup.B;
            default -> AbTestGroup.CONTROL;
        };
    }

}
