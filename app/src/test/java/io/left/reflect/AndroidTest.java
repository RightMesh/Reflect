package io.left.reflect;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import androidx.annotation.IdRes;
import androidx.annotation.IntegerRes;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class,
        sdk = 23)
public abstract class AndroidTest<T extends Activity> {

    protected Application app;
    protected T activity;

    /**
     * Set up activity and app before each test method.
     */
    @Before
    public void setUp() {
        app = RuntimeEnvironment.application;
        runActivity();
    }

    /**
     * Get class type.
     * @return Activity class type
     */
    protected abstract Class<T> getActivityClass();

    /**
     * Set up activity.
     */
    protected void runActivity() {
        Class<T> classActivity = getActivityClass();

        ActivityController<T> controller = Robolectric.buildActivity(classActivity);
        activity = controller.get();
        setMockObjectActivity();

        //Run Activity
        controller.create();
    }

    protected abstract void setMockObjectActivity();


    protected <V extends View> V findViewById(@IdRes int id) {
        return activity.findViewById(id);
    }

    /**
     * Get String from string resource.
     *
     * @param intRes String Id
     * @return string
     */
    protected String getString(@IntegerRes int intRes) {
        return app.getString(intRes);
    }
}
