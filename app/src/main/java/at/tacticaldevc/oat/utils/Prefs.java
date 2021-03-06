package at.tacticaldevc.oat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.tacticaldevc.oat.exceptions.OATApplicationException;

import static at.tacticaldevc.oat.utils.Ensurer.ensureNotNull;
import static at.tacticaldevc.oat.utils.Ensurer.ensurePhoneNumberIsValid;
import static at.tacticaldevc.oat.utils.Ensurer.ensureStringIsValid;

/**
 * A helper class for preference management
 * OAT uses Shared Preferences to store all data that is needed.
 * This ensures that the users stay in full control of their data and no data is saved on third-party servers.
 *
 * @version 0.3
 */
public class Prefs {

    // Shared Preference information
    private final static String DOCUMENT_NAME_DATA = "oat-data";
    private final static String DOCUMENT_NAME_PERMISSIONS = "oat-permissions";
    private final static String DOCUMENT_NAME_ENABLED_FEATURES = "oat-enabled-features";
    private final static String DOCUMENT_NAME_ACCEPTED_CONDITIONS = "oat-accepted-conditions";
    private final static String DOCUMENT_NAME_TRUSTED_CONTACTS = "oat-trusted-contacts";
    private final static String KEY_COMMAND_PASSWORD = "password";
    private final static String KEY_COMMAND_PASSWORD_SALT = "pwdsalt";
    private final static String KEY_COMMAND_TRIGGER = "cmd-trigger";
    private final static String KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP = "missing-permission";
    private final static String KEY_LOCKDOWN_STATUS = "lockdown-status";

    // Basic Data

    /**
     * Updates the Password used to send commands to the App
     *
     * @param context     the Context of the Application
     * @param password    the new Password
     * @param oldPassword the old password, is ignored if no password was previously set
     */
    public static void savePassword(Context context, String password, String oldPassword) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(password, "new User Password");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        byte[] salt;
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw OATApplicationException.forLibraryDeprecatedError("MessageDigest", ex);
        }

        if (prefs.getString(KEY_COMMAND_PASSWORD_SALT, null) == null) {
            SecureRandom rnd = new SecureRandom();
            salt = new byte[16];
            rnd.nextBytes(salt);
            editor.putString(KEY_COMMAND_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP));
        } else {
            salt = Base64.decode(prefs.getString(KEY_COMMAND_PASSWORD_SALT, null), Base64.NO_WRAP);
        }

        String loadedPassword = prefs.getString(KEY_COMMAND_PASSWORD, null);
        if (loadedPassword != null) {
            ensureStringIsValid(oldPassword, "old password");
            if (!verifyApplicationPassword(context, oldPassword))
                throw OATApplicationException.forPasswordMismatch();
        }

        algorithm.update(salt);
        String hash = Base64.encodeToString(algorithm.digest(password.getBytes()), Base64.NO_WRAP);

        editor.putString(KEY_COMMAND_PASSWORD, hash);
        editor.apply();
    }

    /**
     * Verifies that a given Password is correct
     *
     * @param context         the Context of the Application
     * @param passwordToCheck the password to be verified
     * @return true if the password matches the password that was stored
     * @throws OATApplicationException if no Password has been set
     */
    public static boolean verifyApplicationPassword(Context context, String passwordToCheck) {
        ensureNotNull(context, "Application Context");
        try {
            ensureStringIsValid(passwordToCheck, "password to check");
        } catch (IllegalArgumentException ex) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);

        String salt = prefs.getString(KEY_COMMAND_PASSWORD_SALT, null);
        String hash = prefs.getString(KEY_COMMAND_PASSWORD, null);
        if (hash == null) throw OATApplicationException.forNoPasswordSet();
        if (salt == null) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.remove(KEY_COMMAND_PASSWORD);
            edit.remove(KEY_COMMAND_PASSWORD_SALT);
            edit.apply();
            throw OATApplicationException.forCorruptedPasswordHash();
        }
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw OATApplicationException.forLibraryDeprecatedError("MessageDigest", ex);
        }
        algorithm.update(Base64.decode(salt, Base64.NO_WRAP));
        byte[] hashToCheck = algorithm.digest(passwordToCheck.getBytes());

        return Arrays.equals(Base64.decode(hash, Base64.NO_WRAP), hashToCheck);
    }

    /**
     * Saves a new Application trigger word
     *
     * @param context the Context of the Application
     * @param trigger the new trigger word
     * @return the saved trigger phrase
     */
    public static String saveCommandTriggerWord(Context context, String trigger) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(trigger, "Application trigger");
        if (trigger.contains(" "))
            throw new IllegalArgumentException("trigger cannot contain a space character!");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        edit.putString(KEY_COMMAND_TRIGGER, trigger);

        edit.apply();
        return trigger;
    }

    /**
     * Fetches the trigger word for commands issued to this application
     *
     * @param context the Context of the Application
     * @return the trigger word or "oat" if no trigger word has been set
     */
    public static String fetchCommandTriggerWord(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        return prefs.getString(KEY_COMMAND_TRIGGER, "oat");
    }

    // App state

    /**
     * Fetches the status, if the Phone is under lockdown
     *
     * @param context the Context of the Application
     * @return true if the phone is under lockdown, false if the phone is not under lockdown
     */
    public static boolean getLockdownStatus(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOCKDOWN_STATUS, false);
    }

    /**
     * Saves the Loockdown status of the phone (if lockdown is enabled)
     *
     * @param context        the Application Context
     * @param lockdownStatus true if lockdown was enabled, false if it was disabled
     */
    public static void setLockdownStatus(Context context, boolean lockdownStatus) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        edit.putBoolean(KEY_LOCKDOWN_STATUS, lockdownStatus);

        edit.apply();
    }

    // Trusted Contacts

    /**
     * Returns all trusted contacts
     *
     * @param context the {@link Context} of the Application
     * @return A {@link Map} <String,String> containing the phone number and name of all trusted contacts
     */
    public static Map<String, String> fetchTrustedContacts(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_TRUSTED_CONTACTS, Context.MODE_PRIVATE);
        Set<String> keys = prefs.getAll().keySet();
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, prefs.getString(key, null));
        }
        return result;
    }

    /**
     * Saves a trusted contact
     *
     * @param context     the {@link Context} of the Application
     * @param phoneNumber the phone number to be added
     * @return the saved trusted contact
     * @throws IllegalArgumentException if context is null, phone number is invalid or name is invalid (see {@link Ensurer})
     */
    public static String saveTrustedContact(Context context, String phoneNumber, String name) {
        ensureNotNull(context, "Application Context");
        ensurePhoneNumberIsValid(phoneNumber, "new trusted contact");
        ensureStringIsValid(name, "trusted contact name");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_TRUSTED_CONTACTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(phoneNumber, name);
        editor.apply();
        return phoneNumber;
    }

    /**
     * deletes an existing trusted contact
     *
     * @param context     the {@link Context} of the Application
     * @param phoneNumber the phone number of the contact to be removed
     * @return the phone number of the deleted trusted contact
     */
    public static String deleteTrustedContact(Context context, String phoneNumber) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(phoneNumber, "phone number");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_TRUSTED_CONTACTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove(phoneNumber);
        editor.apply();
        return phoneNumber;
    }

    // Permissions

    /**
     * Fetches the permissions of the App
     *
     * @param context the {@link Context} of the Application
     * @return a HashMap with all permissions
     */
    public static Map<String, Boolean> fetchPermissions(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_PERMISSIONS, Context.MODE_PRIVATE);

        HashMap<String, Boolean> permissions = new HashMap<>();

        Set<String> permissionKeys = prefs.getAll().keySet();
        for (String s : permissionKeys) {
            permissions.put(s, prefs.getBoolean(s, false));
        }
        return permissions;
    }

    /**
     * Checks if a specific Permission was granted
     *
     * @param context the {@link Context} of the Application
     * @param key     the key of the Permission to check
     * @return if the Permission was granted, false no data could be found
     */
    public static boolean isPermissionGranted(Context context, String key) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(key, "permission key");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_PERMISSIONS, Context.MODE_PRIVATE);

        return prefs.getBoolean(key, false);
    }

    /**
     * Updates the saved permissions
     *
     * @param context     the {@link Context} of the Application
     * @param permissions the new status that should be saved
     * @return
     */
    public static Map<String, Boolean> savePermissions(Context context, Map<String, Boolean> permissions) {
        ensureNotNull(context, "Application Context");
        ensureNotNull(permissions, "Permission Map");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_PERMISSIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Map<String, Boolean> savedPermissions = fetchPermissions(context);
        if (savedPermissions.size() > permissions.size()) {
            editor.clear();
            savedPermissions = new HashMap<>();
        }

        for (String key : permissions.keySet()) {
            if (!permissions.get(key).equals(savedPermissions.get(key)))
                editor.putBoolean(key, permissions.get(key));
        }

        editor.apply();
        return permissions;
    }

    // Enabled Features

    /**
     * Fetches the saved enabled Features of the App
     *
     * @param context the {@link Context} of the Application
     * @return a HashMap with the enabled Status of all Features
     */
    public static Map<String, Boolean> fetchFeaturesEnabled(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ENABLED_FEATURES, Context.MODE_PRIVATE);

        HashMap<String, Boolean> enabledFeatures = new HashMap<>();

        Set<String> featureKeys = prefs.getAll().keySet();
        for (String s : featureKeys) {
            enabledFeatures.put(s, prefs.getBoolean(s, false));
        }
        return enabledFeatures;
    }

    /**
     * Fetches if target Feature is set to enabled
     *
     * @param context the {@link Context} of the Application
     * @param key     the key of the target Feature
     * @return if the Feature is set to enabled, false if the feature could not be found
     */
    public static boolean fetchFeatureEnabledStatus(Context context, String key) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(key, "Feature key");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ENABLED_FEATURES, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    /**
     * Saves the enabled Status of target Feature
     *
     * @param context  the {@link Context} of the Application
     * @param key      the key of the target Feature whose enabled status should be saved
     * @param newValue the new value of the Feature's enabled status
     * @return the current enabled status of the Feature
     */
    public static boolean saveFeatureEnabledStatus(Context context, String key, boolean newValue) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(key, "Feature key");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ENABLED_FEATURES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(key, newValue);
        editor.apply();
        return newValue;
    }

    // Accepted Terms & Conditions

    /**
     * Fetches the saved accepted Conditions
     *
     * @param context the {@link Context} of the Application
     * @return a HashMap with all conditions
     */
    public static Map<String, Boolean> fetchConditionsAccepted(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ACCEPTED_CONDITIONS, Context.MODE_PRIVATE);

        HashMap<String, Boolean> conditionsAccepted = new HashMap<>();

        Set<String> conditionKeys = prefs.getAll().keySet();
        for (String s : conditionKeys) {
            conditionsAccepted.put(s, prefs.getBoolean(s, false));
        }
        return conditionsAccepted;
    }

    /**
     * Fetches if target Condition was accepted
     *
     * @param context the {@link Context} of the Application
     * @param key     the key (name) of the target Condition
     * @return if the Condition was accepted, false if the feature could not be found
     */
    public static boolean fetchConditionAccepted(Context context, String key) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(key, "Condition key");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ACCEPTED_CONDITIONS, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    /**
     * Saves the condition accepted status ot target condition
     *
     * @param context  the {@link Context} of the Application
     * @param key      the key (name) of the target condition that should be saved
     * @param newValue the new value of the Condition's accepted status
     * @return the current accepted status of the Condition
     */
    public static boolean saveConditionAccepted(Context context, String key, boolean newValue) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(key, "Condition key");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_ACCEPTED_CONDITIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(key, newValue);
        editor.apply();
        return newValue;
    }

    // Request Permission on Startup

    /**
     * Adds a permission to the permissions that have to be requested when the user opens the App the next time.
     * These permissions are necessary for an activated feature to work and have been revoked by the user.
     *
     * @param context    the {@link Context} of the Application
     * @param permission the key of the permission that is missing
     * @return the added permission
     */
    public static String addNewOnStartupPermissionRequest(Context context, String permission) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(permission, "missing permission");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> missingPermissions = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<String>());
        if (!missingPermissions.contains(permission)) {
            missingPermissions.add(permission);
            editor.putStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, missingPermissions);
            editor.apply();
        }
        return permission;
    }

    /**
     * Removes a permission from the Set of permissions that are requested when the user opens the App the next time.
     *
     * @param context    the {@link Context} of the Application
     * @param permission the key of the permission to be removed
     * @return the removed permission
     */
    public static String removeOnStartupPermissionRequest(Context context, String permission) {
        ensureNotNull(context, "Application Context");
        ensureStringIsValid(permission, "missing permission to be removed");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> missingPermissions = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());
        if (missingPermissions.contains(permission)) {
            missingPermissions.remove(permission);
            editor.putStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, missingPermissions);
            editor.apply();
        }
        return permission;
    }

    /**
     * Fetches all permissions that need to be requested from the user when the user opens the App.
     *
     * @param context the {@link Context} of the Application
     * @return all permissions that need to be requested
     */
    public static Set<String> fetchOnStartupPermissionRequests(Context context) {
        ensureNotNull(context, "Application Context");

        SharedPreferences prefs = context.getSharedPreferences(DOCUMENT_NAME_DATA, Context.MODE_PRIVATE);

        return prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());
    }

}
