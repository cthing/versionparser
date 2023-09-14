/*
 * Copyright 2023 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cthing.versionparser.calver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.versionparser.VersionParsingException;


/**
 * Parses a calendar version according to the specified format. There are two ways to parse a calendar version.
 * <ol>
 *     <li>Construct an instance of this class, specifying the format of the version string (e.g. {@code YYYY.MINOR}).
 *         Call the {@link #parse(String)} method to parse a version string. Because the version format is only
 *         parsed once, this is the preferred approach when parsing multiple versions all in the same format.</li>
 *     <li>Call the {@link #parse(String, String)} method, specifying both the format of the version string and
 *         the version. Because the version format is parsed each time this method is called, this approach is best
 *         suited for parsing a single version or multiple versions in different formats.</li>
 * </ol>
 */
public class CalendarVersionScheme {

    private static final Pattern FORMAT_REGEX = Pattern.compile("(?<=[\\-._])|(?=[\\-._])");

    private final String format;
    private final List<ComponentFormat> componentFormats;
    private final Pattern versionRegex;

    /**
     * Creates a calendar version parser.
     *
     * @param format Layout of the version to parse according to the <a href="https://calver.org/">Calendar
     *      Versioning</a> specification (e.g. {@code YYYY.MM.DD})
     * @throws IllegalArgumentException if there is a problem parsing the specified format.
     */
    public CalendarVersionScheme(final String format) {
        this.format = format.trim().toUpperCase(Locale.ROOT);
        if (format.isBlank()) {
            throw new IllegalArgumentException("Format must not be empty");
        }

        this.componentFormats = new ArrayList<>();
        this.versionRegex = parseFormat(this.format, this.componentFormats);
    }

    /**
     * Parses the specified calendar version according to the format specified during construction of the parser.
     * Use this method along with the constructor to parse multiple versions that all have the same format.
     *
     * @param version Calendar version to parse
     * @return Object representing the parsed calendar version.
     * @throws VersionParsingException if there is a problem parsing the specified version
     */
    public CalendarVersion parse(final String version) throws VersionParsingException {
        final Matcher matcher = this.versionRegex.matcher(version);
        if (!matcher.matches()) {
            throw new VersionParsingException(String.format("Version '%s' does not match format '%s'", version,
                                                            this.format));
        }

        final List<Component> components = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            final ComponentFormat componentFormat = this.componentFormats.get(i - 1);
            final String componentStr = matcher.group(i);
            if (componentStr != null) {
                final Component component = new Component(componentFormat, componentStr);
                components.add(component);
            }
        }

        return new CalendarVersion(version, components);
    }

    /**
     * Parses the specified calendar version according to the specified format. Use this method to parse a single
     * version or multiple versions that have different formats.
     *
     * @param formatSpec Layout of the version to parse according to the <a href="https://calver.org/">Calendar
     *      Versioning</a> specification (e.g. {@code YYYY.MM.DD})
     * @param version Calendar version to parse
     * @return Object representing the parsed calendar version.
     * @throws IllegalArgumentException if there is a problem parsing the specified format.
     * @throws VersionParsingException if there is a problem parsing the version
     */
    public static CalendarVersion parse(final String formatSpec, final String version) throws VersionParsingException {
        return new CalendarVersionScheme(formatSpec).parse(version);
    }

    /**
     * Parses the specified format.
     *
     * @param formatSpec Layout of the version to parse according to the <a href="https://calver.org/">Calendar
     *      Versioning</a> specification (e.g. YYYY.MM.DD)
     * @param compFormats Format components parsed from the specified format specification
     * @return Regular expression for parsing a version according to the specified format.
     * @throws IllegalArgumentException if there is a problem parsing the specified format
     */
    private static Pattern parseFormat(final String formatSpec, final List<ComponentFormat> compFormats) {
        final String[] formatParts = FORMAT_REGEX.split(formatSpec);
        final StringBuilder regex = new StringBuilder();

        // Based on the format specification, construct a single regular expression for parsing a version string
        // in that format.
        for (final String formatPart : formatParts) {
            final Optional<Separator> sepOpt = Separator.from(formatPart);
            if (sepOpt.isPresent()) {
                regex.append(sepOpt.get().getRegex());
                continue;
            }

            final Optional<ComponentFormat> componentFormatOpt = ComponentFormat.from(formatPart);
            if (componentFormatOpt.isPresent()) {
                final ComponentFormat componentFormat = componentFormatOpt.get();
                regex.append(componentFormat.getRegex());
                compFormats.add(componentFormat);
                continue;
            }

            throw new IllegalArgumentException("Unrecognized format specifier '" + formatPart + "'");
        }

        compFormats.add(ComponentFormat.MODIFIER);

        return Pattern.compile("^" + regex + ComponentFormat.MODIFIER.getRegex() + "$");
    }
}
