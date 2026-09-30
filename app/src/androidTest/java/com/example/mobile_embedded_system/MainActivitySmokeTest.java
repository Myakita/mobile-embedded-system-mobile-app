package com.example.mobile_embedded_system;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.ui.TelemetryViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Smoke checks for the existing navigation and built-in telemetry simulator. */
@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void launchAndNavigateThroughAllTabs() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
        onView(withId(R.id.mapView)).check(matches(isDisplayed()));

        selectTab(R.id.usersFragment, R.id.recyclerUsers);
        selectTab(R.id.hierarchyFragment, R.id.recyclerHierarchy);
        selectTab(R.id.devicesFragment, R.id.editDeviceSerial);
        selectTab(R.id.settingsFragment, R.id.textMqttStatusBadge);
        selectTab(R.id.mapFragment, R.id.mapView);
    }

    @Test
    public void usersScreenShowsSimulatedVitals() throws InterruptedException {
        selectTab(R.id.usersFragment, R.id.recyclerUsers);
        onView(withId(R.id.textUserCountSummary))
                .check(matches(withText("3 ИЗ 3 ЧЕЛ.")));

        // Observe the same activity-scoped state as UsersFragment. This waits for
        // actual simulator output without relying on a broker or a fixed sleep.
        CountDownLatch telemetryReady = new CountDownLatch(1);
        AtomicReference<LiveData<TelemetryEntity>> latestRef = new AtomicReference<>();
        AtomicReference<Observer<TelemetryEntity>> observerRef = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> {
            TelemetryViewModel viewModel = new ViewModelProvider(activity)
                    .get(TelemetryViewModel.class);
            LiveData<TelemetryEntity> latest = viewModel.getLatestTelemetry(1001L);
            Observer<TelemetryEntity> observer = entity -> {
                if (entity != null) {
                    telemetryReady.countDown();
                }
            };
            latestRef.set(latest);
            observerRef.set(observer);
            latest.observeForever(observer);
        });

        try {
            assertTrue("No mock telemetry received within 10 seconds",
                    telemetryReady.await(10, TimeUnit.SECONDS));
        } finally {
            activityRule.getScenario().onActivity(activity -> {
                LiveData<TelemetryEntity> latest = latestRef.get();
                Observer<TelemetryEntity> observer = observerRef.get();
                if (latest != null && observer != null) {
                    latest.removeObserver(observer);
                }
            });
        }

        onView(withId(R.id.recyclerUsers)).check((view, noViewFound) -> {
            if (noViewFound != null) {
                throw noViewFound;
            }
            RecyclerView users = (RecyclerView) view;
            assertNotNull(users.getAdapter());
            assertEquals(3, users.getAdapter().getItemCount());

            boolean vitalsVisible = false;
            for (int i = 0; i < users.getChildCount(); i++) {
                View row = users.getChildAt(i);
                TextView vitals = row.findViewById(R.id.textUserVitals);
                if (vitals != null && vitals.getText().toString().contains("BPM")
                        && vitals.getText().toString().contains("°C")) {
                    vitalsVisible = true;
                    break;
                }
            }
            assertTrue("No simulated vital signs are visible in the users list", vitalsVisible);
        });
    }

    private void selectTab(int tabId, int screenViewId) {
        onView(withId(tabId)).perform(click());
        onView(withId(screenViewId)).check(matches(isDisplayed()));
    }
}
