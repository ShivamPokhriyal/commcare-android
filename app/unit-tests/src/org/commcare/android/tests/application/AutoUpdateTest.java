package org.commcare.android.tests.application;

import android.text.format.DateUtils;

import org.commcare.CommCareApp;
import org.commcare.CommCareApplication;
import org.commcare.CommCareTestApplication;
import org.commcare.android.CommCareTestRunner;
import org.commcare.android.util.TestAppInstaller;
import org.commcare.engine.resource.AppInstallStatus;
import org.commcare.engine.resource.ResourceInstallUtils;
import org.commcare.preferences.PrefValues;
import org.commcare.suite.model.Profile;
import org.commcare.tasks.ResultAndError;
import org.commcare.tasks.TaskListener;
import org.commcare.tasks.TaskListenerRegistrationException;
import org.commcare.tasks.UpdateTask;
import org.commcare.utils.PendingCalcs;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test auto-update triggering logic and update downloading retry logic once
 * the auto-update is triggered.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
@Config(application = CommCareTestApplication.class)
@RunWith(CommCareTestRunner.class)
public class AutoUpdateTest {

    /**
     * Test the auto-update pending calculations, which have several edge
     * cases.
     */
    @Test
    public void testAutoUpdateCalc() {
        // should be ready for update if last check was 3 days ago
        long checkedThreeDaysAgo = DateTime.now().minusDays(3).getMillis();
        Assert.assertTrue(PendingCalcs.isTimeForAutoUpdateCheck(checkedThreeDaysAgo,
                PrefValues.FREQUENCY_DAILY));

        // shouldn't be ready for update if last check was 3 hours ago
        long checkedThreeHoursAgo = DateTime.now().minusHours(3).getMillis();
        if (isSameDayAsNow(checkedThreeHoursAgo)) {
            Assert.assertFalse(PendingCalcs.isTimeForAutoUpdateCheck(checkedThreeHoursAgo,
                    PrefValues.FREQUENCY_DAILY));
        } else {
            Assert.assertTrue(PendingCalcs.isTimeForAutoUpdateCheck(checkedThreeHoursAgo,
                    PrefValues.FREQUENCY_DAILY));
        }

        // test different calendar day less than 24 hours ago trigger when
        // checking every day
        DateTime yesterdayNearMidnight = new DateTime();
        yesterdayNearMidnight =
                yesterdayNearMidnight.minusDays(1).withTimeAtStartOfDay().plusHours(23);
        DateTime now = new DateTime();
        long diff = yesterdayNearMidnight.minus(now.getMillis()).getMillis();
        Assert.assertTrue(diff < DateUtils.DAY_IN_MILLIS);
        Assert.assertTrue(PendingCalcs.isTimeForAutoUpdateCheck(yesterdayNearMidnight.getMillis(),
                PrefValues.FREQUENCY_DAILY));

        // test timeshift a couple of hours in the future, shouldn't be enough
        // to warrant a update trigger
        long hoursInTheFuture = DateTime.now().plusHours(2).getMillis();
        Assert.assertFalse(PendingCalcs.isTimeForAutoUpdateCheck(hoursInTheFuture,
                PrefValues.FREQUENCY_DAILY));

        // test timeshift where if we last checked more than one day in the
        // future then we trigger
        long daysLater = DateTime.now().plusDays(2).getMillis();
        Assert.assertTrue(PendingCalcs.isTimeForAutoUpdateCheck(daysLater,
                PrefValues.FREQUENCY_DAILY));

        long weekLater = DateTime.now().plusWeeks(1).getMillis();
        Assert.assertTrue(PendingCalcs.isTimeForAutoUpdateCheck(weekLater,
                PrefValues.FREQUENCY_DAILY));
    }

    private static boolean isSameDayAsNow(long checkTime) {
        return getDayOfWeek(checkTime) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
    }

    private static int getDayOfWeek(long time) {
        Calendar lastRestoreCalendar = Calendar.getInstance();
        lastRestoreCalendar.setTimeInMillis(time);
        return lastRestoreCalendar.get(Calendar.DAY_OF_WEEK);
    }
}
