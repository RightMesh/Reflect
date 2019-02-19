package io.left.reflect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.left.reflect.helper.MeshHelper;
import io.left.rightmesh.id.MeshId;
import io.left.rightmesh.mesh.MeshManager.DataReceivedEvent;
import io.left.rightmesh.mesh.MeshManager.RightMeshEvent;
import io.left.rightmesh.util.RightMeshException;
import io.left.rightmesh.util.RightMeshException.RightMeshServiceDisconnectedException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Simple app for testing RightMesh network reach.
 *
 *
 * Device A sends a ping to Device B over a mesh network containing a unique message (a
 * simple timestamp). Device B receives it and echoes it back. Device A displays when the
 * echoed message is received. This makes it easy to leave a device in one spot, roam around
 * with another device, and test if the network still works.
 */
public class MainActivity extends AppCompatActivity implements
        RightMeshRecipientView.RecipientChangedListener {
    private static final String TAG = MainActivity.class.getCanonicalName();

    /**
     * MESH_PORT is the mesh port that this app is allowed to run on, according to your license key.
     * See developer.rightmesh.io for more details.
     */
    public static final int MESH_PORT = 9876;

    RightMeshConnector rightMeshConnector;

    // Id of this device, stored for UI use.
    MeshId deviceId;

    // Id of the peer to send pings to.
    MeshId recipientId;

    // Adapter for tracking views and the spinner it
    // feeds, both mostly powered by `viewRightMeshRecipient`.
    MeshIdAdapter peersListAdapter;

    // List and adapter tracking sent pings and whether or not they have been echoed.
    ArrayList<String> pingsList;
    ArrayAdapter<String> pingsListAdapter;

    public static final char ALREADY_ECHOED = '0';
    public static final char ECHO = '1';

    private TextView tvLibStatus;
    // Responsible for allowing the user to select the ping recipient.
    RightMeshRecipientView viewRightMeshRecipient;
    Spinner spinnerPeers;

    /**
     * MainActivity construction.
     */
    public MainActivity() {
        pingsListAdapter = null;
        pingsList = new ArrayList<>();
        spinnerPeers = null;
        peersListAdapter = null;
        recipientId = null;
        deviceId = null;
    }

    /**
     * Initializes list adapters, sets UI event handlers, and starts the RightMesh library
     * connection on application start.
     *
     * @param savedInstanceState passed by Android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Send a ping when the floating send button is tapped.
        FloatingActionButton buttonSend = findViewById(R.id.button_send);
        tvLibStatus = findViewById(R.id.text_view_device_status);

        buttonSend.setOnClickListener(this::sendPing);

        // Display the RightMesh settings activity when the send button is tapped and held.
        buttonSend.setLongClickable(true);
        buttonSend.setOnLongClickListener(v -> {
            rightMeshConnector.toRightMeshWalletActivty();
            return false;
        });

        // Set up the recipient selection spinner.
        peersListAdapter = new MeshIdAdapter(this);
        viewRightMeshRecipient = findViewById(R.id.view_rightmesh_recipient);
        viewRightMeshRecipient.setSpinnerAdapter(peersListAdapter);
        viewRightMeshRecipient.setOnRecipientChangedListener(this);
        spinnerPeers = findViewById(R.id.spinner_recipient);

        // Set up the rvLogs list.
        pingsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pingsList);
        ListView listViewLogs = findViewById(R.id.listview_logs);
        listViewLogs.setAdapter(pingsListAdapter);

        initRightMeshConnector();
    }

    /**
     * Initialize and connect RightMesh.
     */
    private void initRightMeshConnector() {
        getRightMeshConnector().setOnPeerChangedListener(event -> {
            viewRightMeshRecipient.updatePeersList(event);
            updateColoursOnPeerChanged(event);
        });
        getRightMeshConnector().setOnDataReceiveListener(this::receiveData);
        getRightMeshConnector().setOnConnectSuccessListener(meshId -> {
            deviceId = meshId;
            // Initialize the peer adapter with this device's MeshId.
            peersListAdapter.add(deviceId);
            peersListAdapter.setDeviceId(deviceId);
            peersListAdapter.notifyDataSetChanged();


            tvLibStatus.setText(getString(R.string.lib_start_with_meshid)
                    + getRightMeshConnector().getUuid().toString());
        });
        getRightMeshConnector().connect(getApplicationContext());
    }

    /**
     * Send a ping to the recipient.
     *
     * @param view passed by Android
     */
    public void sendPing(View view) {
        DateFormat df = new SimpleDateFormat("MMM dd kk:mm:ss:SSSS");

        // Null check, as recipientId has no default value.
        if (recipientId != null) {
            // Ping content is just the current time, so they are unique and give us some rough
            // concept of delay.
            String timestamp = df.format(new Date());
            String payload = "1" + timestamp;

            try {
                // Attempt to ping the currently selected recipient.
                getRightMeshConnector().sendDataReliable(recipientId, payload.getBytes());

                // Log the ping if sent successfully.
                pingsList.add(0, timestamp);
                pingsListAdapter.notifyDataSetChanged();
            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected before ping could be sent, with message: "
                        + sde.getMessage());
            } catch (RightMeshException rme) {
                Log.e(TAG, "Error occurred sending ping, with message: " + rme.getMessage());
            }
        }
    }

    /**
     * Fired by the {@link RightMeshRecipientView} when the selected recipient Id has changed.
     *
     * Stores the new recipient and updates the display.
     *
     * @param recipient new recipient
     */
    @Override
    public void onRecipientChanged(MeshId recipient) {
        recipientId = recipient;
        updateRecipientColour();
    }

    /**
     * Returns a ping, or logs a returned ping.
     *
     * @param rme Event passed from RightMesh.
     */
    private void receiveData(RightMeshEvent rme) {
        // Parse data from event.
        DataReceivedEvent dre = (DataReceivedEvent) rme;
        String dataString = new String(dre.data);
        char echoBit = dataString.charAt(0); // `1` for initial requests, `0` for echoed requests.
        String timestamp = dataString.substring(1);

        if (echoBit == ECHO) {
            // Echo messages starting with '1'.
            String responsePayload = "0" + dataString.substring(1);
            try {
                getRightMeshConnector().sendDataReliable(dre.peerUuid, responsePayload.getBytes());
                pingsList.add(0, "Echoed ping. ("
                        + MeshHelper.getInstance().shortenMeshId(rme.peerUuid) + ")");
            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected before ping could be returned, "
                        + "with message: " + sde.getMessage());
            } catch (RightMeshException rmx) {
                Log.e(TAG, "Error occurred sending ping, with message: " + rmx.getMessage());
            }
        } else if (echoBit == ALREADY_ECHOED) {
            // Messages starting with '0' have already been echoed - update log.
            if (pingsList.contains(timestamp)) {
                pingsList.set(pingsList.indexOf(timestamp),
                        timestamp + " - Received! ("
                                + MeshHelper.getInstance().shortenMeshId(rme.peerUuid) + ")");
            }
        }

        // Null-check the adapter, as events may fire when the activity doesn't exist.
        if (pingsListAdapter != null) {
            pingsListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Updates the colour of the selected peer every time its connection state changes.
     *
     * @param rme event passed from RightMesh
     */
    private void updateColoursOnPeerChanged(RightMeshEvent rme) {
        if (rme.peerUuid.equals(recipientId)) {
            updateRecipientColour();
        }
    }

    /**
     * Resume RightMesh connection on activity resume.
     */
    @Override
    protected void onResume() {
        super.onResume();
        getRightMeshConnector().resume();
    }

    /**
     * Close RightMesh connection when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getRightMeshConnector().stop();
    }

    //
    // HELPER FUNCTIONS
    //

    /**
     * Makes the recipient colour green if the recipient is connected, red if it isn't, or leaves it
     * blue if the recipient is the current device.
     */
    private void updateRecipientColour() {
        if (!recipientId.equals(deviceId)) {
            int colour;
            if (peersListAdapter.contains(recipientId)) {
                colour = R.color.green;
            } else {
                colour = R.color.red;
            }
            ((TextView) spinnerPeers.getSelectedView())
                    .setTextColor(ContextCompat.getColor(this, colour));
        }
    }

    /**
     * Get RightMeshConnector.
     *
     * This method and {@link MainActivity#setRightMeshConnector(RightMeshConnector)} help for
     * testing with mocked objects.
     *
     * @return {@link RightMeshConnector} which works on MESH_PORT
     */
    public RightMeshConnector getRightMeshConnector() {
        if (rightMeshConnector == null) {
            rightMeshConnector = new RightMeshConnector(MESH_PORT);
        }
        return rightMeshConnector;
    }

    /**
     * Using this method to assign mocked object to RightMeshConnector.
     *
     * @param rightMeshConnector Mocked object.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setRightMeshConnector(RightMeshConnector rightMeshConnector) {
        this.rightMeshConnector = rightMeshConnector;
    }

    /**
     * Using this method to assign mocked object to recipientId.
     *
     * @param recipientId Mocked object.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setRecipientId(MeshId recipientId) {
        this.recipientId = recipientId;
    }
}