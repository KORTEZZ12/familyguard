package com.example.testjava;

import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SimpleActivityTest {

    @Test
    public void launchTestEmptyActivity_usingActivityScenario() {
        try (ActivityScenario<TestEmptyActivity> scenario =
                     ActivityScenario.launch(TestEmptyActivity.class)) {
            // Проверяем, что Activity создалась
            scenario.onActivity(activity ->
                    assertNotNull("Activity не должна быть null", activity)
            );
        }
    }
}
