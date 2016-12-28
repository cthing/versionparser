/*
 * Copyright 2016 C Thing Software
 * All rights reserved.
 */
package org.cthing.versionparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents a version number string that has been parsed into a canonical form and is comparable with other
 * parsed versions.
 */
public final class Version implements Comparable<Version> {

    /** Captures the leading numeric components of a version number string. */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^(\\d+)(?:[.\\-](\\d+))?(?:[.\\-](\\d+))?(?:[.\\-](\\d+))?(?:[.\\-](\\d+))?(?:[.\\-](\\d+|\\+))?");

    /** Captures the trailing non-numeric portion of a version number string, if any. */
    private static final Pattern TRAILING_PATTERN = Pattern.compile("^[\\d]+(?:[.\\-](?:\\d+|\\+))*[\\-.]?(.*?)$");

    /** Alpha pattern. */
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^alpha[\\-.]?(\\d*)$");

    /** Beta pattern. */
    private static final Pattern BETA_PATTERN = Pattern.compile("^beta[\\-.]?(\\d*)$");

    /** Release candidate pattern. */
    private static final Pattern RC_PATTERN = Pattern.compile("^rc[\\-.]?(\\d*)$");

    /** Milestone pattern. */
    private static final Pattern MILESTONE_PATTERN = Pattern.compile("^m[\\-.]?(\\d+)$");

    /** Alphabetic pattern. */
    private static final Pattern LETTER_PATTERN = Pattern.compile("^([a-z])$");

    private static final int SNAPSHOT = -3000;
    private static final int ALPHA_BASE = -2000;
    private static final int MILESTONE_BASE = -1000;
    private static final int BETA_BASE = -500;
    private static final int RC_BASE = -100;
    private static final int RELEASE_BASE = 0;

    private static final int BASE_TEN = 10;
    private static final String WILDCARD_CHAR = "+";
    private static final long WILDCARD_VALUE = Long.MAX_VALUE;

    private final String version;
    private final List<Long> components;
    private String trailing;
    private boolean trailingRecognized;
    private int trailingValue;
    private boolean released;

    public Version(final String version) {
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }

        this.version = version;
        this.components = new ArrayList<>();
        this.trailingValue = RELEASE_BASE;
        this.trailingRecognized = true;
        this.released = true;

        parse();
    }

    public String getVersion() {
        return this.version;
    }

    public List<Long> getComponents() {
        return Collections.unmodifiableList(this.components);
    }

    public String getTrailing() {
        return this.trailing;
    }

    boolean isTrailingRecognized() {
        return this.trailingRecognized;
    }

    int getTrailingValue() {
        return this.trailingValue;
    }

    public boolean isReleased() {
        return this.released;
    }

    private void parse() {
        final String trimmedVersion = this.version.trim();

        // Extract the leading numerical components of the version.
        final Matcher numericMatcher = NUMERIC_PATTERN.matcher(trimmedVersion);
        if (numericMatcher.lookingAt()) {
            for (int idx = 1; idx <= numericMatcher.groupCount(); idx++) {
                final String component = numericMatcher.group(idx);
                if (component != null) {
                    if (WILDCARD_CHAR.equals(component)) {
                        this.components.add(WILDCARD_VALUE);
                    } else {
                        this.components.add(Long.parseUnsignedLong(component, BASE_TEN));
                    }
                }
            }
        }

        // Extract and process the trailing portion of the version, if any.
        final Matcher trailingMatcher = TRAILING_PATTERN.matcher(trimmedVersion);
        if (trailingMatcher.matches()) {
            this.trailing = trailingMatcher.group(1);
            parseTrailing();
        } else if (this.components.isEmpty()) {
            this.trailing = trimmedVersion;
            parseTrailing();
        }
    }

    private void parseTrailing() {
        if ((this.trailing == null) || (this.trailing.isEmpty())) {
            return;
        }

        final String lowerTrailing = this.trailing.toLowerCase();

        if ("release".equals(lowerTrailing) || "final".equals(lowerTrailing)) {
            this.trailingValue = RELEASE_BASE;
            return;
        }

        if (lowerTrailing.equals("snapshot")) {
            this.trailingValue = SNAPSHOT;
            this.released = false;
            return;
        }

        if (parsePreRelease(lowerTrailing, ALPHA_PATTERN, ALPHA_BASE)
                || parsePreRelease(lowerTrailing, BETA_PATTERN, BETA_BASE)
                || parsePreRelease(lowerTrailing, RC_PATTERN, RC_BASE)
                || parsePreRelease(lowerTrailing, MILESTONE_PATTERN, MILESTONE_BASE)) {
            return;
        }

        final Matcher matcher = LETTER_PATTERN.matcher(lowerTrailing);
        if (matcher.matches()) {
            this.trailingValue = RELEASE_BASE;
            this.trailingValue += (int)matcher.group(1).charAt(0);
            return;
        }

        this.trailingRecognized = false;
    }

    private boolean parsePreRelease(final String tail, final Pattern pattern, final int baseValue) {
        final Matcher matcher = pattern.matcher(tail);
        final boolean matches = matcher.matches();
        if (matches) {
            this.trailingValue = baseValue;
            final String value = matcher.group(1);
            if ((value != null) && !value.isEmpty()) {
                this.trailingValue += Integer.parseInt(value, BASE_TEN);
            }
            this.released = false;
        }
        return matches;
    }

    @Override
    public int compareTo(final Version other) {
        if (this == other) {
            return 0;
        }

        final List<Long> otherComponents = new ArrayList<>(other.components);
        final List<Long> thisComponents = new ArrayList<>(this.components);
        final int maxSize = Math.max(otherComponents.size(), thisComponents.size());

        for (int idx = otherComponents.size() - 1; idx < maxSize; idx++) {
            otherComponents.add(0L);
        }
        for (int idx = thisComponents.size() - 1; idx < maxSize; idx++) {
            thisComponents.add(0L);
        }

        for (int idx = 0; idx < maxSize; idx++) {
            final long value1 = thisComponents.get(idx);
            final long value2 = otherComponents.get(idx);

            if ((value1 == WILDCARD_VALUE) || (value2 == WILDCARD_VALUE)) {
                continue;
            }

            if (value1 < value2) {
                return -1;
            }
            if (value1 > value2) {
                return 1;
            }
        }

        if (this.trailingRecognized) {
            if (this.trailingValue < other.trailingValue) {
                return -1;
            }
            if (this.trailingValue > other.trailingValue) {
                return 1;
            }
            return 0;
        }

        final int result = this.trailing.compareTo(other.trailing);
        return (int)Math.signum(result);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return compareTo((Version)obj) == 0;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Long component : this.components) {
            if (component != 0) {
                result = 31 * result + component.hashCode();
            }
        }
        if (this.trailingRecognized) {
            result = 31 * result + 1;
            result = 31 * result + this.trailingValue;
        } else {
            result = 31 * result + (this.trailing != null ? this.trailing.hashCode() : 0);
        }
        return result;
    }

    @Override
    public String toString() {
        return this.version;
    }
}
