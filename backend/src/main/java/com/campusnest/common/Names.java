package com.campusnest.common;

/** Privacy helper: how one user's name is shown to other users before they have chosen to share more. */
public final class Names {

    private Names() {
    }

    /** "Asha Kulkarni (sample)" -> "Asha K." : first name and the initial of the last name only. */
    public static String shortName(String displayName) {
        String cleaned = displayName.replaceAll("\\(.*?\\)", " ").trim().replaceAll("\\s+", " ");
        String[] parts = cleaned.split(" ");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "Student";
        }
        return parts.length == 1 ? parts[0] : parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
