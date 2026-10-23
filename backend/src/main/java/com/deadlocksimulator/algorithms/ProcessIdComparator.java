package com.deadlocksimulator.algorithms;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Natural ordering for ids such as P2 &lt; P10 (prefix, then numeric suffix, then raw string). */
public final class ProcessIdComparator implements Comparator<String> {

    public static final ProcessIdComparator INSTANCE = new ProcessIdComparator();
    private static final Pattern ID = Pattern.compile("^(.*?)(\\d+)$");

    private ProcessIdComparator() {
    }

    @Override
    public int compare(String a, String b) {
        Matcher ma = ID.matcher(a);
        Matcher mb = ID.matcher(b);
        if (ma.matches() && mb.matches()) {
            int prefix = ma.group(1).compareTo(mb.group(1));
            if (prefix != 0) {
                return prefix;
            }
            int num = new java.math.BigInteger(ma.group(2)).compareTo(new java.math.BigInteger(mb.group(2)));
            if (num != 0) {
                return num;
            }
        }
        return a.compareTo(b);
    }
}
