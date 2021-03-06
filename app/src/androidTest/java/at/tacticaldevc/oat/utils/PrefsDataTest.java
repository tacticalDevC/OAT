package at.tacticaldevc.oat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import at.tacticaldevc.oat.exceptions.OATApplicationException;

import static at.tacticaldevc.oat.utils.Prefs.addNewOnStartupPermissionRequest;
import static at.tacticaldevc.oat.utils.Prefs.fetchCommandTriggerWord;
import static at.tacticaldevc.oat.utils.Prefs.fetchOnStartupPermissionRequests;
import static at.tacticaldevc.oat.utils.Prefs.getLockdownStatus;
import static at.tacticaldevc.oat.utils.Prefs.removeOnStartupPermissionRequest;
import static at.tacticaldevc.oat.utils.Prefs.saveCommandTriggerWord;
import static at.tacticaldevc.oat.utils.Prefs.savePassword;
import static at.tacticaldevc.oat.utils.Prefs.verifyApplicationPassword;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


public class PrefsDataTest {
    private static final String DOCUMENT_NAME_TEST = "oat-data";
    private final static String KEY_COMMAND_PASSWORD = "password";
    private final static String KEY_COMMAND_PASSWORD_SALT = "pwdsalt";
    private final static String KEY_COMMAND_TRIGGER = "cmd-trigger";
    private final static String KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP = "missing-permission";
    private final static String KEY_LOCKDOWN_STATUS = "lockdown-status";

    @Before
    public void init() {
        clean();
    }

    // password
    @Test
    public void savePasswordWithInvalidValues() {
        // test
        assertThrows(IllegalArgumentException.class, () -> savePassword(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), null, null));
        assertThrows(IllegalArgumentException.class, () -> savePassword(null, "new Password", null));
        assertThrows(IllegalArgumentException.class, () -> savePassword(null, "", null));
        assertThrows(IllegalArgumentException.class, () -> savePassword(null, "\n\r", null));
    }

    @Test
    public void savePasswordWithoutData() {
        // prepare
        String newPassword = "NewPassword";

        // test
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), newPassword, null);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        String psswdHash = prefs.getString(KEY_COMMAND_PASSWORD, null);
        byte[] salt = Base64.decode(prefs.getString(KEY_COMMAND_PASSWORD_SALT, null), Base64.NO_WRAP);
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
            algorithm.update(salt);
            String expectedHash = Base64.encodeToString(algorithm.digest(newPassword.getBytes()), Base64.NO_WRAP);
            assertThat(psswdHash).isEqualTo(expectedHash);
            assertThat(salt).isNotEmpty();
        } catch (NoSuchAlgorithmException ex) {
            fail();
        }
    }

    @Test
    public void savePasswordWithData() {
        // prepare
        String newPassword = "NewPassword";
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), "Password", null);

        // test
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), newPassword, "Password");

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        String psswdHash = prefs.getString(KEY_COMMAND_PASSWORD, null);
        byte[] salt = Base64.decode(prefs.getString(KEY_COMMAND_PASSWORD_SALT, null), Base64.NO_WRAP);
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
            algorithm.update(salt);
            String expectedHash = Base64.encodeToString(algorithm.digest(newPassword.getBytes()), Base64.NO_WRAP);
            assertThat(psswdHash).isEqualTo(expectedHash);
        } catch (NoSuchAlgorithmException ex) {
            fail();
        }
    }

    @Test
    public void savePasswordWithInvalidOldPassword() {
        // prepare
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), "Password", null);

        // assert
        assertThrows(OATApplicationException.class, () -> savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), "NewPassword", "InvalidPassword"));
    }

    @Test
    public void verifyApplicationPasswordWithInvalidValues() {
        // test
        assertThrows(IllegalArgumentException.class, () -> verifyApplicationPassword(null, null));
        assertThrows(IllegalArgumentException.class, () -> verifyApplicationPassword(null, ""));
    }

    @Test
    public void verifyApplicationPasswordWithoutExistingData() {
        // test
        assertThrows(OATApplicationException.class, () -> verifyApplicationPassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), "Password"));
    }

    @Test
    public void verifyApplicationPasswordWithDataWithValidPassword() {
        // prepare
        String passwordToCheck = "Password";
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), passwordToCheck, null);

        // test
        boolean result = verifyApplicationPassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), passwordToCheck);

        // assert
        assertThat(result).isTrue();
    }

    @Test
    public void verifyApplicationPasswordWithDataWithInvalidPassword() {
        // prepare
        String passwordToCheck = "Password";
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), "Password1", null);

        // test
        boolean result = verifyApplicationPassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), passwordToCheck);

        // assert
        assertThat(result).isFalse();
    }

    @Test
    public void verifyApplicationPasswordWithCorruptedHash() {
        // prepare
        String password = "Password";
        savePassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), password, null);
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(KEY_COMMAND_PASSWORD_SALT);
        edit.apply();

        // assert
        assertThrows(OATApplicationException.class, () -> verifyApplicationPassword(InstrumentationRegistry.getInstrumentation().getTargetContext(), password));
        assertThat(prefs.getString(KEY_COMMAND_PASSWORD, null)).isNull();
        assertThat(prefs.getString(KEY_COMMAND_PASSWORD_SALT, null)).isNull();
    }

    // Command trigger word

    @Test
    public void saveCommandTriggerWordWithInvalidValues() {
        // test
        assertThrows(IllegalArgumentException.class, () -> saveCommandTriggerWord(null, null));
        assertThrows(IllegalArgumentException.class, () -> saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), null));
        assertThrows(IllegalArgumentException.class, () -> saveCommandTriggerWord(null, "oat"));
        assertThrows(IllegalArgumentException.class, () -> saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), "trigger phrase"));
    }

    @Test
    public void saveCommandTriggerWordWithoutExistingData() {
        // prepare
        String trigger = "lockdown";

        // test
        String result = saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), trigger);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        String loaded = prefs.getString(KEY_COMMAND_TRIGGER, null);
        assertThat(result).isSameAs(trigger);
        assertThat(loaded).isEqualTo(trigger);
    }

    @Test
    public void saveCommandTriggerWordWithData() {
        // prepare
        String trigger = "lockdown";
        saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), "oat");

        // test
        String result = saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), trigger);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        String loaded = prefs.getString(KEY_COMMAND_TRIGGER, null);

        assertThat(result).isSameAs(trigger);
        assertThat(loaded).isEqualTo(result);
    }

    @Test
    public void fetchCommandTriggerWordWithInvalidValues() {
        // test
        assertThrows(IllegalArgumentException.class, () -> fetchCommandTriggerWord(null));
    }

    @Test
    public void fetchCommandTriggerWordWithoutExistingData() {
        // test
        String result = fetchCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // assert
        assertThat(result).isEqualTo("oat");
    }

    @Test
    public void fetchCommandTriggerWordWithExistingData() {
        // prepare
        String trigger = "trigger";
        saveCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext(), trigger);

        // test
        String result = fetchCommandTriggerWord(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // assert
        assertThat(result).isEqualTo(trigger);
    }

    // Application state

    @Test
    public void getLockdownStatusWithoutExistingData() {
        // test
        boolean result = getLockdownStatus(InstrumentationRegistry.getInstrumentation().getTargetContext());

        //assert
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void getLockdownStatusWithExistingData() {
        // prepare
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(KEY_LOCKDOWN_STATUS, true);
        edit.apply();

        // test
        boolean result = getLockdownStatus(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // assert
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void setLockdownStatus() {
        // prepare
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(KEY_LOCKDOWN_STATUS, false);
        edit.apply();

        // test
        Prefs.setLockdownStatus(InstrumentationRegistry.getInstrumentation().getTargetContext(), true);

        // assert
        boolean result = prefs.getBoolean(KEY_LOCKDOWN_STATUS, false);
        assertThat(result).isTrue();
    }

    // on startup permission request

    @Test
    public void addNewOnStartupPermissionRequestWithValidValueAndExistingData() {
        // prepare
        Set<String> setup = onStartupPermissionPermissionRequestSetup();
        String missingPermission = "ACCESS_COARSE_LOCATION";

        // test
        String result = addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), missingPermission);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        Set<String> load = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());
        setup.add(missingPermission);

        assertThat(result).isSameAs(missingPermission);
        assertThat(load).isEqualTo(setup);
    }

    @Test
    public void addNewOnStartupPermissionRequestWithValidValueWithoutExistingData() {
        // prepare
        String missingPermission = "ACCESS_COARSE_LOCATION";
        Set<String> expectedResult = new HashSet<>();
        expectedResult.add(missingPermission);

        // test
        String result = addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), missingPermission);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        Set<String> load = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());
        expectedResult.add(missingPermission);

        assertThat(result).isSameAs(missingPermission);
        assertThat(load).isEqualTo(expectedResult);
    }

    @Test
    public void addNewOnStartupPermissionRequestWithNullValues() {
        // assert
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(null, null));
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(null, "missing permission"));
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), null));
    }

    @Test
    public void addNewOnStartupPermissionRequestWithInvalidPermissions() {
        // prepare
        String invalidPermission1 = "";
        String invalidPermission2 = " ";
        String invalidPermission3 = "\r\n";

        // assert
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission1));
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission2));
        assertThrows(IllegalArgumentException.class, () -> addNewOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission3));
    }

    @Test
    public void removeOnStartupPermissionRequestWithValidValuesAndExistingData() {
        // prepare
        Set<String> setup = onStartupPermissionPermissionRequestSetup();
        String grantedPermission = "SEND_SMS";

        // test
        String result = removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), grantedPermission);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        Set<String> load = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());
        setup.remove(grantedPermission);

        assertThat(result).isSameAs(grantedPermission);
        assertThat(load).isEqualTo(setup);
    }

    @Test
    public void removeOnStartupPermissionRequestWithValidValuesWithoutExistingData() {
        // prepare
        String missingPermission = "ACCESS_COARSE_LOCATION";

        // test
        String result = removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), missingPermission);

        // assert
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        Set<String> load = prefs.getStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, new HashSet<>());

        assertThat(result).isSameAs(missingPermission);
        assertThat(load).isEmpty();
    }

    @Test
    public void removeOnStartupPermissionRequestWithNullValues() {
        // assert
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(null, null));
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(null, "missing permission"));
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), null));
    }

    @Test
    public void removeOnStartupPermissionRequestWithInvalidPermissions() {
        // prepare
        String invalidPermission1 = "";
        String invalidPermission2 = " ";
        String invalidPermission3 = "\r\n";

        // assert
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission1));
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission2));
        assertThrows(IllegalArgumentException.class, () -> removeOnStartupPermissionRequest(InstrumentationRegistry.getInstrumentation().getTargetContext(), invalidPermission3));
    }

    @Test
    public void fetchOnStartupPermissionRequestsWithExistingValues() {
        // prepare
        Set<String> setup = onStartupPermissionPermissionRequestSetup();

        // test
        Set<String> result = fetchOnStartupPermissionRequests(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // assert
        assertThat(result).isEqualTo(setup);
    }

    @Test
    public void fetchOnStartupPermissionRequestWithoutData() {
        // test
        Set<String> result = fetchOnStartupPermissionRequests(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // assert
        assertThat(result).isEmpty();
    }

    @Test
    public void fetchOnStartupPermissionRequestWithNullValues() {
        // assert
        assertThrows(IllegalArgumentException.class, () -> fetchOnStartupPermissionRequests(null));
    }

    @AfterEach
    public void cleanup() {
        clean();
    }

    private void clean() {
        SharedPreferences pref = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
    }

    private Set<String> onStartupPermissionPermissionRequestSetup() {
        SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences(DOCUMENT_NAME_TEST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        HashSet<String> missingPermissions = new HashSet<>();
        missingPermissions.add("SEND_SMS");
        missingPermissions.add("RECEIVE_SMS");
        editor.putStringSet(KEY_MISSING_PERMISSIONS_TO_REQUEST_ON_STARTUP, missingPermissions);
        editor.apply();

        return missingPermissions;
    }
}