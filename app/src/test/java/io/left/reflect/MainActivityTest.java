package io.left.reflect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Application;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.left.rightmesh.id.MeshId;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.util.RightMeshException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.internal.LocalPermissionGranter;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class,
        sdk = 23)
public class MainActivityTest extends AndroidTest<MainActivity> {
    private LocalPermissionGranter localPermissionGranter = new LocalPermissionGranter();

    @Mock
    private RightMeshConnector rightMeshConnector;
    @Mock
    private MeshId meshId;

    @Captor
    private ArgumentCaptor<RightMeshConnector.OnConnectSuccessListener> onConnectSuccess;
    @Captor
    private ArgumentCaptor<RightMeshConnector.OnDataReceiveListener> onDataReceive;
    @Captor
    private ArgumentCaptor<RightMeshConnector.OnPeerChangedListener> onPeerChanged;

    private TextView tvLibStatus;
    private RightMeshRecipientView viewRightMeshRecipient;
    private Spinner spinnerPeers;
    private FloatingActionButton buttonSend;
    private ListView listViewLogs;

    @Override
    protected Class<MainActivity> getActivityClass() {
        return MainActivity.class;
    }

    /**
     * Assign mocked objects to Activity.
     */
    @Override
    protected void setMockObjectActivity() {
        activity.setRightMeshConnector(rightMeshConnector);
        activity.setRecipientId(meshId);
    }

    /**
     * Run activity and find view id.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        super.setUp();

        localPermissionGranter.addPermissions(Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        localPermissionGranter.requestPermissions();

        tvLibStatus = findViewById(R.id.text_view_device_status);
        viewRightMeshRecipient = findViewById(R.id.view_rightmesh_recipient);
        spinnerPeers = findViewById(R.id.spinner_recipient);
        buttonSend = findViewById(R.id.button_send);
        listViewLogs = findViewById(R.id.listview_logs);
    }

    @Test
    public void onConnectSuccess() {
        doReturn(meshId).when(rightMeshConnector).getUuid();

        Assert.assertEquals(tvLibStatus.getText(),
                getString(R.string.status_label_waiting));

        verify(rightMeshConnector).setOnConnectSuccessListener(onConnectSuccess.capture());
        onConnectSuccess.getValue().onConnectSuccess(meshId);

        Assert.assertTrue(tvLibStatus.getText().toString().startsWith(
                getString(R.string.lib_start_with_meshid)));
    }

    /**
     * Check if listViewLogs have a new item when received ECHO msg.
     */
    @Test
    public void onDataReceive_echo() {
        //Init DataReceivedEvent with EchoBit = ECHO
        MeshManager.DataReceivedEvent rme = new MeshManager
                .DataReceivedEvent(MainActivity.MESH_PORT,
                mock(MeshId.class),
                new byte[]{MainActivity.ECHO, 1});

        Assert.assertNotNull(listViewLogs.getAdapter());
        int numberLogs = listViewLogs.getAdapter().getCount();

        verify(rightMeshConnector).setOnDataReceiveListener(onDataReceive.capture());
        onDataReceive.getValue().onDataReceive(rme);

        Assert.assertEquals(listViewLogs.getAdapter().getCount(), numberLogs + 1);
    }

    /**
     * Check if listViewLogs update when received ALREADY_ECHOED msg.
     */
    @Test
    public void onDataReceive_already_echoed() {
        //Data Receive with echo bit
        onDataReceive_echo();

        //Init DataReceivedEvent with EchoBit = ALREADY_ECHOED
        MeshManager.DataReceivedEvent rme = new MeshManager
                .DataReceivedEvent(MainActivity.MESH_PORT,
                mock(MeshId.class),
                new byte[]{MainActivity.ALREADY_ECHOED, 2});

        Assert.assertNotNull(listViewLogs.getAdapter());
        int numberLogs = listViewLogs.getAdapter().getCount();

        verify(rightMeshConnector).setOnDataReceiveListener(onDataReceive.capture());
        onDataReceive.getValue().onDataReceive(rme);

        Assert.assertEquals(listViewLogs.getAdapter().getCount(), numberLogs);
    }

    @Test
    public void sendPing() throws RightMeshException {
        buttonSend.callOnClick();

        verify(rightMeshConnector).sendDataReliable(any(), any());
    }
}
