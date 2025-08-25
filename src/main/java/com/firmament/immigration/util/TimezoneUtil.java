package com.firmament.immigration.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class TimezoneUtil {

    private static final Map<String, String> TIMEZONE_DISPLAY_NAMES = Map.of(
            "America/Montreal", "Eastern Time (Montreal)",
            "America/Toronto", "Eastern Time (Toronto)",
            "America/New_York", "Eastern Time (New York)",
            "America/Los_Angeles", "Pacific Time (Los Angeles)",
            "Europe/Paris", "Central European Time (Paris)",
            "Europe/London", "British Time (London)",
            "Africa/Casablanca", "Western European Time (Casablanca)",
            "Asia/Dubai", "Gulf Standard Time (Dubai)"
    );

    /**
     * Convert UTC time to user's local time
     */
    public ZonedDateTime convertToUserTimezone(ZonedDateTime utcTime, String userTimezone) {
        try {
            ZoneId targetZone = ZoneId.of(userTimezone);
            return utcTime.withZoneSameInstant(targetZone);
        } catch (Exception e) {
            log.error("Invalid timezone: {}, defaulting to UTC", userTimezone);
            return utcTime;
        }
    }

    /**
     * Format datetime for email display
     */
    public String formatForEmail(ZonedDateTime utcTime, String userTimezone, Locale locale) {
        ZonedDateTime localTime = convertToUserTimezone(utcTime, userTimezone);

        // Format: "Monday, January 15, 2025 at 2:00 PM EST"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE, MMMM d, yyyy 'at' h:mm a z",
                locale != null ? locale : Locale.ENGLISH
        );

        return localTime.format(formatter);
    }

    /**
     * Format datetime for email display (French)
     */
    public String formatForEmailFrench(ZonedDateTime utcTime, String userTimezone) {
        ZonedDateTime localTime = convertToUserTimezone(utcTime, userTimezone);

        // Format: "Lundi 15 janvier 2025 à 14h00 EST"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE d MMMM yyyy 'à' HH'h'mm z",
                Locale.FRENCH
        );

        return localTime.format(formatter);
    }

    /**
     * Get user-friendly timezone name
     */
    public String getDisplayName(String timezoneId) {
        return TIMEZONE_DISPLAY_NAMES.getOrDefault(timezoneId, timezoneId);
    }

    /**
     * Get timezone abbreviation (EST, PST, etc.)
     */
    public String getAbbreviation(ZonedDateTime dateTime, String timezoneId) {
        ZoneId zone = ZoneId.of(timezoneId);
        ZonedDateTime zonedTime = dateTime.withZoneSameInstant(zone);
        return zonedTime.format(DateTimeFormatter.ofPattern("z"));
    }

    /**
     * Get offset from UTC
     */
    public String getUtcOffset(String timezoneId) {
        ZoneId zone = ZoneId.of(timezoneId);
        ZonedDateTime now = ZonedDateTime.now(zone);
        return now.format(DateTimeFormatter.ofPattern("O")); // e.g., "+0100", "-0500"
    }

    /**
     * Check if timezone uses DST (Daylight Saving Time)
     */
//    public boolean usesDST(String timezoneId) {
//        ZoneId zone = ZoneId.of(timezoneId);
//        ZoneRules rules = zone.getRules();
//
//        // Check if there are any DST transitions
//        Instant now = Instant.now();
//        return !rules.getTransitionRules().isEmpty() ||
//                rules.isDaylightSavings(now) ||
//                rules.getNextTransition(now) != null;
//    }

    /**
     * Get list of common timezones for dropdown
     */
    public static List<TimezoneOption> getCommonTimezones() {
        return Arrays.asList(
                // North America
                new TimezoneOption("America/New_York", "Eastern Time (New York)"),
                new TimezoneOption("America/Chicago", "Central Time (Chicago)"),
                new TimezoneOption("America/Denver", "Mountain Time (Denver)"),
                new TimezoneOption("America/Los_Angeles", "Pacific Time (Los Angeles)"),
                new TimezoneOption("America/Toronto", "Eastern Time (Toronto)"),
                new TimezoneOption("America/Montreal", "Eastern Time (Montreal)"),
                new TimezoneOption("America/Vancouver", "Pacific Time (Vancouver)"),

                // Europe
                new TimezoneOption("Europe/London", "British Time (London)"),
                new TimezoneOption("Europe/Paris", "Central European Time (Paris)"),
                new TimezoneOption("Europe/Berlin", "Central European Time (Berlin)"),
                new TimezoneOption("Europe/Madrid", "Central European Time (Madrid)"),
                new TimezoneOption("Europe/Rome", "Central European Time (Rome)"),

                // Africa
                new TimezoneOption("Africa/Casablanca", "Western European Time (Casablanca)"),
                new TimezoneOption("Africa/Cairo", "Eastern European Time (Cairo)"),
                new TimezoneOption("Africa/Lagos", "West Africa Time (Lagos)"),
                new TimezoneOption("Africa/Johannesburg", "South Africa Time (Johannesburg)"),

                // Middle East
                new TimezoneOption("Asia/Dubai", "Gulf Standard Time (Dubai)"),
                new TimezoneOption("Asia/Jerusalem", "Israel Time (Jerusalem)"),
                new TimezoneOption("Asia/Riyadh", "Arabia Standard Time (Riyadh)"),

                // Asia
                new TimezoneOption("Asia/Tokyo", "Japan Time (Tokyo)"),
                new TimezoneOption("Asia/Shanghai", "China Time (Shanghai)"),
                new TimezoneOption("Asia/Singapore", "Singapore Time"),
                new TimezoneOption("Asia/Kolkata", "India Time (Kolkata)"),

                // Oceania
                new TimezoneOption("Australia/Sydney", "Australian Eastern Time (Sydney)"),
                new TimezoneOption("Pacific/Auckland", "New Zealand Time (Auckland)")
        );
    }

    /**
     * Validate if a timezone ID is valid
     */
    public boolean isValidTimezone(String timezoneId) {
        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Inner class for timezone dropdown options
     */
    public static class TimezoneOption {
        public final String id;
        public final String displayName;

        public TimezoneOption(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }
}